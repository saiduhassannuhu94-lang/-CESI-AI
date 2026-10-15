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
        when (request.stage) {
            WhatsAppReactionStage.FIND_MESSAGE -> tryFindAndOpenReactionPicker(root, request)
            WhatsAppReactionStage.FIND_REACTION -> tryChooseReaction(root, request)
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        WhatsAppReactionCoordinator.clear()
        return super.onUnbind(intent)
    }

    private fun tryFindAndOpenReactionPicker(root: AccessibilityNodeInfo, request: PendingWhatsAppReaction) {
        val found = try { root.findAccessibilityNodeInfosByText(request.message).orEmpty() }
        catch (_: Exception) { emptyList() }
        val matches = found.filter { it.text?.toString()?.trim() == request.message }
        found.filterNot { it in matches }.forEach { it.recycle() }

        // If the same text appears more than once, do not guess which message to react to.
        if (matches.size != 1) {
            matches.forEach { it.recycle() }
            return
        }

        val target = matches.single()
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

    private fun performLongClickOnMessage(target: AccessibilityNodeInfo): Boolean {
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
        // Do not search for the requested emoji in an ordinary chat screen.
        // Require several known quick-reaction controls to be visible first.
        if (visibleQuickReactionGlyphs(root).size < MIN_VISIBLE_REACTIONS) return

        val matches = mutableListOf<AccessibilityNodeInfo>()
        collectEmojiNodes(root, request.emoji, matches, 0)
        if (matches.size != 1) {
            matches.forEach { it.recycle() }
            return
        }

        val node = matches.single()
        var cursor: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(node)
        var clicked = false
        try {
            var depth = 0
            while (cursor != null && depth < MAX_ANCESTOR_DEPTH && !clicked) {
                val current = cursor
                clicked = current.isClickable && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (!clicked) {
                    val parent = current.parent
                    current.recycle()
                    cursor = parent
                }
                depth++
            }
        } catch (_: Exception) {
            clicked = false
        } finally {
            cursor?.recycle()
            node.recycle()
        }

        if (clicked) {
            WhatsAppReactionCoordinator.clear()
            Toast.makeText(
                this,
                "CESI ya danna reaction control. Duba WhatsApp don tabbatar.",
                Toast.LENGTH_LONG
            ).show()
        }
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
        val text = node.text?.toString()?.trim()
        val description = node.contentDescription?.toString()?.trim()
        if (text in KNOWN_REACTION_GLYPHS) result += text!!
        if (description in KNOWN_REACTION_GLYPHS) result += description!!
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            try { collectKnownReactionGlyphs(child, result, depth + 1) }
            finally { child.recycle() }
            if (result.size >= MIN_VISIBLE_REACTIONS) return
        }
    }

    private fun collectEmojiNodes(
        node: AccessibilityNodeInfo,
        emoji: String,
        result: MutableList<AccessibilityNodeInfo>,
        depth: Int
    ) {
        if (depth > MAX_TREE_DEPTH || result.size > MAX_MATCHES) return
        if (node.text?.toString()?.trim() == emoji ||
            node.contentDescription?.toString()?.trim() == emoji
        ) result += AccessibilityNodeInfo.obtain(node)

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            try { collectEmojiNodes(child, emoji, result, depth + 1) }
            finally { child.recycle() }
            if (result.size > MAX_MATCHES) return
        }
    }

    companion object {
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val MAX_ANCESTOR_DEPTH = 8
        private const val MAX_TREE_DEPTH = 40
        private const val MAX_MATCHES = 2
        private const val MIN_VISIBLE_REACTIONS = 3
        private val KNOWN_REACTION_GLYPHS = setOf("👍", "❤️", "😂", "😮", "😢", "🙏")

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
