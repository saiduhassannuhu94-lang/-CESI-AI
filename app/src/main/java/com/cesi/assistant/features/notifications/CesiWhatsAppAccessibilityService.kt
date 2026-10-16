package com.cesi.assistant.features.notifications

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Narrow, user-triggered WhatsApp reaction helper.
 * Ambiguous message/emoji matches are refused. A UI click is never treated as
 * proof that WhatsApp sent or synced a reaction.
 */
class CesiWhatsAppAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != WHATSAPP_PACKAGE) return
        if (!WhatsAppAutomationDisclosureActivity.hasConsent(this)) return
        val request = WhatsAppReactionCoordinator.current() ?: return
        val root = rootInActiveWindow ?: return
        try {
            when (request.stage) {
                WhatsAppReactionStage.FIND_MESSAGE -> tryFindAndOpenReactionPicker(root, request)
                WhatsAppReactionStage.FIND_REACTION -> tryChooseReaction(root, request)
            }
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() {
        // Never resume a half-completed external UI action after Android interrupts the service.
        WhatsAppReactionCoordinator.clear()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        WhatsAppReactionCoordinator.clear()
        return super.onUnbind(intent)
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

    companion object {
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val MAX_ANCESTOR_DEPTH = 8
        private const val MAX_TREE_DEPTH = 40
        private const val MAX_TREE_NODES = 1_500
        private const val MIN_VISIBLE_REACTIONS = 3

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
