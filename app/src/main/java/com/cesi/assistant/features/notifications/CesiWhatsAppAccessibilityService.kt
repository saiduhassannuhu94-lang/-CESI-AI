package com.cesi.assistant.features.notifications

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * User-triggered one-shot helper. It inspects WhatsApp only for a pending
 * message action and YouTube/TikTok only for a pending search-and-play request.
 * A UI click is not proof of playback, reaction sync or message delivery.
 */
class CesiWhatsAppAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (!WhatsAppAutomationDisclosureActivity.hasConsent(this)) return
        if (packageName != WHATSAPP_PACKAGE) {
            val request = MediaPlaybackCoordinator.currentForPackage(packageName) ?: return
            val mediaRoot = rootInActiveWindow ?: return
            try { tryPlayFirstMediaResult(mediaRoot, request) } finally { mediaRoot.recycle() }
            return
        }
        val reactionRequest = WhatsAppReactionCoordinator.current()
        val quotedReplyRequest = WhatsAppQuotedReplyCoordinator.current()
        if (reactionRequest == null && quotedReplyRequest?.authorized != true) return
        val root = rootInActiveWindow ?: return
        try {
            if (reactionRequest != null) {
                when (reactionRequest.stage) {
                    WhatsAppReactionStage.FIND_MESSAGE -> tryFindAndOpenReactionPicker(root, reactionRequest)
                    WhatsAppReactionStage.FIND_REACTION -> tryChooseReaction(root, reactionRequest)
                }
            } else if (quotedReplyRequest?.authorized == true) {
                when (quotedReplyRequest.stage) {
                    WhatsAppQuotedReplyStage.FIND_TARGET_MESSAGE -> tryFindTargetForQuotedReply(root, quotedReplyRequest)
                    WhatsAppQuotedReplyStage.FIND_REPLY_ACTION -> tryChooseReplyAction(root, quotedReplyRequest)
                    WhatsAppQuotedReplyStage.FILL_REPLY -> tryFillQuotedReply(root, quotedReplyRequest)
                    WhatsAppQuotedReplyStage.SEND_REPLY -> trySendQuotedReply(root, quotedReplyRequest)
                }
            }
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() {
        // Never resume a half-completed external UI action after Android interrupts the service.
        WhatsAppReactionCoordinator.clear()
        WhatsAppQuotedReplyCoordinator.clear()
        MediaPlaybackCoordinator.clear()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        WhatsAppReactionCoordinator.clear()
        WhatsAppQuotedReplyCoordinator.clear()
        MediaPlaybackCoordinator.clear()
        return super.onUnbind(intent)
    }

    private fun tryPlayFirstMediaResult(root: AccessibilityNodeInfo, request: PendingMediaPlayback) {
        if (!hasVisibleSearchQuery(root, request.query)) return
        val metrics = resources.displayMetrics
        val candidates = linkedMapOf<String, AccessibilityNodeInfo>()
        collectMediaResultTargets(root, candidates, 0, intArrayOf(0), metrics.widthPixels, metrics.heightPixels, metrics.density)
        if (candidates.isEmpty()) return
        val target = candidates.values.minWithOrNull(
            compareBy<AccessibilityNodeInfo> { node ->
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                bounds.top
            }.thenBy { node ->
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                bounds.left
            }
        )
        candidates.values.filter { it !== target }.forEach { it.recycle() }
        if (target == null) return
        val clicked = try {
            performClickOnNodeOrAncestor(target)
        } catch (_: Exception) {
            false
        } finally {
            target.recycle()
        }
        if (clicked) {
            MediaPlaybackCoordinator.clear()
            Toast.makeText(
                this,
                "CESI ya zaɓi sakamakon bidiyo na farko. Duba app ɗin don tabbatar da kunnawa.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun hasVisibleSearchQuery(root: AccessibilityNodeInfo, query: String): Boolean =
        hasVisibleSearchQueryRecursive(root, query, 0, intArrayOf(0))

    private fun hasVisibleSearchQueryRecursive(
        node: AccessibilityNodeInfo,
        query: String,
        depth: Int,
        visited: IntArray
    ): Boolean {
        if (depth > MAX_TREE_DEPTH || visited[0] >= MAX_TREE_NODES) return false
        visited[0]++
        if (node.isVisibleToUser && MediaPlaybackAccessibilityMatcher.isSearchQueryVisible(
                query,
                node.viewIdResourceName.orEmpty(),
                node.text?.toString().orEmpty(),
                node.contentDescription?.toString().orEmpty()
            )
        ) return true
        for (index in 0 until node.childCount) {
            if (visited[0] >= MAX_TREE_NODES) return false
            val child = node.getChild(index) ?: continue
            val found = try {
                hasVisibleSearchQueryRecursive(child, query, depth + 1, visited)
            } finally {
                child.recycle()
            }
            if (found) return true
        }
        return false
    }

    private fun collectMediaResultTargets(
        node: AccessibilityNodeInfo,
        targets: MutableMap<String, AccessibilityNodeInfo>,
        depth: Int,
        visited: IntArray,
        screenWidth: Int,
        screenHeight: Int,
        density: Float
    ) {
        if (depth > MAX_TREE_DEPTH || visited[0] >= MAX_TREE_NODES) return
        visited[0]++
        if (node.isVisibleToUser && MediaPlaybackAccessibilityMatcher.hasResultLabel(
                node.viewIdResourceName.orEmpty(),
                node.text?.toString().orEmpty(),
                node.contentDescription?.toString().orEmpty()
            )
        ) {
            val target = findClickableResultCardAncestor(node, screenWidth, screenHeight, density)
            if (target != null) {
                val key = actionableNodeKey(target)
                if (key == null || targets.containsKey(key)) {
                    target.recycle()
                } else {
                    targets[key] = target
                }
            }
        }
        for (index in 0 until node.childCount) {
            if (visited[0] >= MAX_TREE_NODES) return
            val child = node.getChild(index) ?: continue
            try {
                collectMediaResultTargets(child, targets, depth + 1, visited, screenWidth, screenHeight, density)
            } finally {
                child.recycle()
            }
        }
    }

    private fun findClickableResultCardAncestor(
        node: AccessibilityNodeInfo,
        screenWidth: Int,
        screenHeight: Int,
        density: Float
    ): AccessibilityNodeInfo? {
        var cursor: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(node)
        var depth = 0
        while (cursor != null && depth < MAX_ANCESTOR_DEPTH) {
            val current = cursor
            if (current.isClickable) {
                val bounds = Rect()
                current.getBoundsInScreen(bounds)
                if (MediaPlaybackAccessibilityMatcher.isPlausibleResultCard(
                        bounds.width(), bounds.height(), bounds.top, bounds.bottom,
                        screenWidth, screenHeight, density
                    )
                ) return current
            }
            val parent = current.parent
            current.recycle()
            cursor = parent
            depth++
        }
        cursor?.recycle()
        return null
    }

    private fun tryFindAndOpenReactionPicker(root: AccessibilityNodeInfo, request: PendingWhatsAppReaction) {
        val matches = mutableListOf<AccessibilityNodeInfo>()
        collectExactMessageNodes(root, request.message, matches, 0, intArrayOf(0))

        // Several text nodes may belong to one bubble. Collapse them by the
        // bounds of their nearest long-clickable ancestor, but never guess
        // between two distinct bubbles containing the same text.
        val targets = linkedMapOf<String, AccessibilityNodeInfo>()
        for (match in matches) {
            val target = findLongClickableAncestor(match) ?: AccessibilityNodeInfo.obtain(match)
            val key = actionableNodeKey(target)
            if (key == null) {
                target.recycle()
            } else if (targets.containsKey(key)) {
                target.recycle()
            } else {
                targets[key] = target
            }
            match.recycle()
        }

        if (targets.size != 1) {
            targets.values.forEach { it.recycle() }
            return
        }

        val target = targets.values.single()
        val opened = try { performLongClickOnMessage(target) }
        catch (_: Exception) { false }
        finally { target.recycle() }

        if (opened) {
            WhatsAppReactionCoordinator.advance(
                WhatsAppReactionStage.FIND_MESSAGE,
                WhatsAppReactionStage.FIND_REACTION
            )
        }
    }

    private fun collectExactMessageNodes(
        node: AccessibilityNodeInfo,
        expected: String,
        result: MutableList<AccessibilityNodeInfo>,
        depth: Int,
        visited: IntArray
    ) {
        if (depth > MAX_TREE_DEPTH || visited[0] >= MAX_TREE_NODES) return
        visited[0]++
        if (node.isVisibleToUser &&
            WhatsAppReactionAccessibilityMatcher.matchesMessageText(
                expected,
                node.text?.toString().orEmpty()
            )
        ) {
            result += AccessibilityNodeInfo.obtain(node)
        }
        for (index in 0 until node.childCount) {
            if (visited[0] >= MAX_TREE_NODES) return
            val child = node.getChild(index) ?: continue
            try { collectExactMessageNodes(child, expected, result, depth + 1, visited) }
            finally { child.recycle() }
        }
    }

    private fun findLongClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var cursor: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(node)
        var depth = 0
        while (cursor != null && depth < MAX_ANCESTOR_DEPTH) {
            val current = cursor
            if (current.isLongClickable) return current
            val parent = current.parent
            current.recycle()
            cursor = parent
            depth++
        }
        cursor?.recycle()
        return null
    }

    private fun performLongClickOnMessage(target: AccessibilityNodeInfo): Boolean {
        if (target.isLongClickable &&
            target.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        ) return true

        var cursor: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(target)
        var depth = 0
        try {
            while (cursor != null && depth < MAX_ANCESTOR_DEPTH) {
                val current = cursor
                if (current.isLongClickable &&
                    current.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                ) return true

                val parent = current.parent
                current.recycle()
                cursor = parent
                depth++
            }
            return false
        } finally {
            cursor?.recycle()
        }
    }

    private fun tryChooseReaction(root: AccessibilityNodeInfo, request: PendingWhatsAppReaction) {
        // WhatsApp's accessible labels vary by version and locale. Accept known
        // emoji labels, but require a visible reaction picker before selecting.
        if (visibleQuickReactionGlyphs(root).size < MIN_VISIBLE_REACTIONS) return

        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collectReactionNodes(root, request.emoji, candidates, 0, intArrayOf(0))
        val targets = linkedMapOf<String, AccessibilityNodeInfo>()
        for (candidate in candidates) {
            val target = findClickableAncestor(candidate) ?: AccessibilityNodeInfo.obtain(candidate)
            val key = actionableNodeKey(target)
            if (key == null) {
                target.recycle()
            } else if (targets.containsKey(key)) {
                target.recycle()
            } else {
                targets[key] = target
            }
            candidate.recycle()
        }

        if (targets.size != 1) {
            targets.values.forEach { it.recycle() }
            return
        }

        val target = targets.values.single()
        val clicked = try { performClickOnNodeOrAncestor(target) }
        catch (_: Exception) { false }
        finally { target.recycle() }

        if (clicked) {
            WhatsAppReactionCoordinator.clear()
            Toast.makeText(
                this,
                "CESI ya danna reaction control. Duba WhatsApp don tabbatar.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun findClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var cursor: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(node)
        var depth = 0
        while (cursor != null && depth < MAX_ANCESTOR_DEPTH) {
            val current = cursor
            if (current.isClickable) return current
            val parent = current.parent
            current.recycle()
            cursor = parent
            depth++
        }
        cursor?.recycle()
        return null
    }

    private fun performClickOnNodeOrAncestor(target: AccessibilityNodeInfo): Boolean {
        if (target.isClickable && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        var cursor: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(target)
        var depth = 0
        try {
            while (cursor != null && depth < MAX_ANCESTOR_DEPTH) {
                val current = cursor
                if (current.isClickable &&
                    current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                ) return true
                val parent = current.parent
                current.recycle()
                cursor = parent
                depth++
            }
            return false
        } finally {
            cursor?.recycle()
        }
    }

    private fun actionableNodeKey(node: AccessibilityNodeInfo): String? {
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.isEmpty) return null
        return "${node.viewIdResourceName.orEmpty()}|${node.className?.toString().orEmpty()}|${bounds.flattenToString()}"
    }

    private fun visibleQuickReactionGlyphs(root: AccessibilityNodeInfo): Set<String> {
        val visible = mutableSetOf<String>()
        collectKnownReactionGlyphs(root, visible, 0)
        return visible
    }

    private fun collectKnownReactionGlyphs(
        node: AccessibilityNodeInfo,
        result: MutableSet<String>,
        depth: Int
    ) {
        if (depth > MAX_TREE_DEPTH || result.size >= MIN_VISIBLE_REACTIONS) return
        if (!node.isVisibleToUser) return
        val text = node.text?.toString().orEmpty()
        val description = node.contentDescription?.toString().orEmpty()
        WhatsAppReactionAccessibilityMatcher.canonicalReaction(text)?.let(result::add)
        WhatsAppReactionAccessibilityMatcher.canonicalReaction(description)?.let(result::add)
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            try { collectKnownReactionGlyphs(child, result, depth + 1) }
            finally { child.recycle() }
            if (result.size >= MIN_VISIBLE_REACTIONS) return
        }
    }

    private fun collectReactionNodes(
        node: AccessibilityNodeInfo,
        emoji: String,
        result: MutableList<AccessibilityNodeInfo>,
        depth: Int,
        visited: IntArray
    ) {
        if (depth > MAX_TREE_DEPTH || visited[0] >= MAX_TREE_NODES) return
        visited[0]++
        if (node.isVisibleToUser &&
            (WhatsAppReactionAccessibilityMatcher.matchesRequestedReaction(
                node.text?.toString().orEmpty(), emoji
            ) || WhatsAppReactionAccessibilityMatcher.matchesRequestedReaction(
                node.contentDescription?.toString().orEmpty(), emoji
            ))
        ) {
            result += AccessibilityNodeInfo.obtain(node)
        }
        for (index in 0 until node.childCount) {
            if (visited[0] >= MAX_TREE_NODES) return
            val child = node.getChild(index) ?: continue
            try { collectReactionNodes(child, emoji, result, depth + 1, visited) }
            finally { child.recycle() }
        }
    }


    private fun tryFindTargetForQuotedReply(
        root: AccessibilityNodeInfo,
        request: PendingWhatsAppQuotedReply
    ) {
        // Require a real chat composer; this prevents matching a message preview
        // in the chat list or a search result and replying to the wrong place.
        if (!hasVisibleMessageComposer(root)) return

        val matches = mutableListOf<AccessibilityNodeInfo>()
        collectExactMessageNodes(root, request.targetText, matches, 0, intArrayOf(0))
        val targets = linkedMapOf<String, AccessibilityNodeInfo>()
        for (match in matches) {
            val target = findLongClickableAncestor(match) ?: AccessibilityNodeInfo.obtain(match)
            val key = actionableNodeKey(target)
            if (key == null || targets.containsKey(key)) {
                target.recycle()
            } else {
                targets[key] = target
            }
            match.recycle()
        }

        if (targets.size != 1) {
            targets.values.forEach { it.recycle() }
            return
        }

        val target = targets.values.single()
        val opened = try { performLongClickOnMessage(target) }
        catch (_: Exception) { false }
        finally { target.recycle() }

        if (opened) {
            WhatsAppQuotedReplyCoordinator.advance(
                WhatsAppQuotedReplyStage.FIND_TARGET_MESSAGE,
                WhatsAppQuotedReplyStage.FIND_REPLY_ACTION
            )
        }
    }

    private fun tryChooseReplyAction(root: AccessibilityNodeInfo, request: PendingWhatsAppQuotedReply) {
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collectLabelNodes(root, REPLY_LABELS, candidates, 0, intArrayOf(0))
        val targets = linkedMapOf<String, AccessibilityNodeInfo>()
        for (candidate in candidates) {
            val target = findClickableAncestor(candidate) ?: AccessibilityNodeInfo.obtain(candidate)
            val key = actionableNodeKey(target)
            if (key == null || targets.containsKey(key)) {
                target.recycle()
            } else {
                targets[key] = target
            }
            candidate.recycle()
        }

        if (targets.size != 1) {
            targets.values.forEach { it.recycle() }
            return
        }

        val target = targets.values.single()
        val clicked = try { performClickOnNodeOrAncestor(target) }
        catch (_: Exception) { false }
        finally { target.recycle() }

        if (clicked) {
            WhatsAppQuotedReplyCoordinator.advance(
                WhatsAppQuotedReplyStage.FIND_REPLY_ACTION,
                WhatsAppQuotedReplyStage.FILL_REPLY
            )
        }
    }

    private fun tryFillQuotedReply(root: AccessibilityNodeInfo, request: PendingWhatsAppQuotedReply) {
        if (!hasVisibleMessageComposer(root)) return
        val editable = mutableListOf<AccessibilityNodeInfo>()
        collectEditableNodes(root, editable, 0, intArrayOf(0))
        val composers = editable.filter { node ->
            val hint = node.hintText?.toString().orEmpty().lowercase(java.util.Locale.ROOT)
            val description = node.contentDescription?.toString().orEmpty().lowercase(java.util.Locale.ROOT)
            hint.contains("message") || hint.contains("saƙo") || hint.contains("sako") ||
                description.contains("message") || description.contains("saƙo") || description.contains("sako")
        }
        editable.filterNot { it in composers }.forEach { it.recycle() }
        if (composers.size != 1) {
            composers.forEach { it.recycle() }
            return
        }

        val composer = composers.single()
        val arguments = android.os.Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                request.replyText
            )
        }
        val set = try {
            composer.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments) &&
                composer.text?.toString() == request.replyText
        } catch (_: Exception) {
            false
        } finally {
            composer.recycle()
        }

        if (set) {
            WhatsAppQuotedReplyCoordinator.advance(
                WhatsAppQuotedReplyStage.FILL_REPLY,
                WhatsAppQuotedReplyStage.SEND_REPLY
            )
        }
    }

    private fun trySendQuotedReply(root: AccessibilityNodeInfo, request: PendingWhatsAppQuotedReply) {
        // Re-check both the target and the exact reply immediately before the
        // external side effect. The user may have changed screens or edited text.
        if (!hasVisibleMessageComposer(root)) return
        if (!hasVisibleExactTargetMessage(root, request.targetText)) return
        if (!hasExactReplyInComposer(root, request.replyText)) return
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collectLabelNodes(root, SEND_LABELS, candidates, 0, intArrayOf(0))
        val targets = linkedMapOf<String, AccessibilityNodeInfo>()
        for (candidate in candidates) {
            val target = findClickableAncestor(candidate) ?: AccessibilityNodeInfo.obtain(candidate)
            val key = actionableNodeKey(target)
            if (key == null || targets.containsKey(key)) {
                target.recycle()
            } else {
                targets[key] = target
            }
            candidate.recycle()
        }

        if (targets.size != 1) {
            targets.values.forEach { it.recycle() }
            return
        }

        val target = targets.values.single()
        val clicked = try { performClickOnNodeOrAncestor(target) }
        catch (_: Exception) { false }
        finally { target.recycle() }

        if (clicked) {
            WhatsAppQuotedReplyCoordinator.clear()
            Toast.makeText(
                this,
                "CESI ya danna Send bayan tabbatarwarka. Duba chat ɗin don tabbatar da isarwa.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun hasVisibleExactTargetMessage(
        root: AccessibilityNodeInfo,
        targetText: String
    ): Boolean {
        val matches = mutableListOf<AccessibilityNodeInfo>()
        collectExactMessageNodes(root, targetText, matches, 0, intArrayOf(0))
        val found = matches.isNotEmpty()
        matches.forEach { it.recycle() }
        return found
    }

    private fun hasExactReplyInComposer(
        root: AccessibilityNodeInfo,
        expectedReply: String
    ): Boolean {
        val editables = mutableListOf<AccessibilityNodeInfo>()
        collectEditableNodes(root, editables, 0, intArrayOf(0))
        val composers = editables.filter { node ->
            val hint = node.hintText?.toString().orEmpty().lowercase(java.util.Locale.ROOT)
            val description = node.contentDescription?.toString().orEmpty().lowercase(java.util.Locale.ROOT)
            hint.contains("message") || hint.contains("saƙo") || hint.contains("sako") ||
                description.contains("message") || description.contains("saƙo") || description.contains("sako")
        }
        editables.filterNot { it in composers }.forEach { it.recycle() }
        if (composers.size != 1) {
            composers.forEach { it.recycle() }
            return false
        }
        val composer = composers.single()
        val matches = composer.text?.toString() == expectedReply
        composer.recycle()
        return matches
    }

    private fun hasVisibleMessageComposer(root: AccessibilityNodeInfo): Boolean {
        val editables = mutableListOf<AccessibilityNodeInfo>()
        collectEditableNodes(root, editables, 0, intArrayOf(0))
        val found = editables.any { node ->
            val hint = node.hintText?.toString().orEmpty().lowercase(java.util.Locale.ROOT)
            val description = node.contentDescription?.toString().orEmpty().lowercase(java.util.Locale.ROOT)
            hint.contains("message") || hint.contains("saƙo") || hint.contains("sako") ||
                description.contains("message") || description.contains("saƙo") || description.contains("sako")
        }
        editables.forEach { it.recycle() }
        return found
    }

    private fun collectEditableNodes(
        node: AccessibilityNodeInfo,
        result: MutableList<AccessibilityNodeInfo>,
        depth: Int,
        visited: IntArray
    ) {
        if (depth > MAX_TREE_DEPTH || visited[0] >= MAX_TREE_NODES) return
        visited[0]++
        if (node.isVisibleToUser && node.isEditable) result += AccessibilityNodeInfo.obtain(node)
        for (index in 0 until node.childCount) {
            if (visited[0] >= MAX_TREE_NODES) return
            val child = node.getChild(index) ?: continue
            try { collectEditableNodes(child, result, depth + 1, visited) }
            finally { child.recycle() }
        }
    }

    private fun collectLabelNodes(
        node: AccessibilityNodeInfo,
        acceptedLabels: Set<String>,
        result: MutableList<AccessibilityNodeInfo>,
        depth: Int,
        visited: IntArray
    ) {
        if (depth > MAX_TREE_DEPTH || visited[0] >= MAX_TREE_NODES) return
        visited[0]++
        if (node.isVisibleToUser) {
            val text = node.text?.toString().orEmpty().trim().lowercase(java.util.Locale.ROOT)
            val description = node.contentDescription?.toString().orEmpty().trim().lowercase(java.util.Locale.ROOT)
            if (text in acceptedLabels || description in acceptedLabels) {
                result += AccessibilityNodeInfo.obtain(node)
            }
        }
        for (index in 0 until node.childCount) {
            if (visited[0] >= MAX_TREE_NODES) return
            val child = node.getChild(index) ?: continue
            try { collectLabelNodes(child, acceptedLabels, result, depth + 1, visited) }
            finally { child.recycle() }
        }
    }

    companion object {
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val MAX_ANCESTOR_DEPTH = 8
        private const val MAX_TREE_DEPTH = 40
        private const val MAX_TREE_NODES = 1_500
        private const val MIN_VISIBLE_REACTIONS = 3
        private val REPLY_LABELS = setOf("reply", "reply message", "amsa", "amsa saƙo", "amsa sako")
        private val SEND_LABELS = setOf("send", "send message", "send button", "tura", "aika", "tura saƙo", "tura sako")

        fun isEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty()
            val expected = ComponentName(context, CesiWhatsAppAccessibilityService::class.java)
            return enabledServices.split(':').any { flattened ->
                ComponentName.unflattenFromString(flattened) == expected
            }
        }
    }
}
