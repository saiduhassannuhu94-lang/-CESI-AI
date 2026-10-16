package com.cesi.assistant.features.notifications

/** One-shot, in-memory request to reply to an exact quoted message in the open chat. */
internal object WhatsAppQuotedReplyCoordinator {
    private val store = WhatsAppQuotedReplyRequestStore()

    fun begin(targetText: String, replyText: String): Boolean = store.begin(targetText, replyText)
    fun current(): PendingWhatsAppQuotedReply? = store.current()
    fun authorize(): Boolean = store.authorize()
    fun advance(expected: WhatsAppQuotedReplyStage, next: WhatsAppQuotedReplyStage): Boolean =
        store.advance(expected, next)
    fun clear() = store.clear()
}

internal enum class WhatsAppQuotedReplyStage {
    FIND_TARGET_MESSAGE,
    FIND_REPLY_ACTION,
    FILL_REPLY,
    SEND_REPLY
}

internal data class PendingWhatsAppQuotedReply(
    val targetText: String,
    val replyText: String,
    val createdAtMs: Long,
    val stage: WhatsAppQuotedReplyStage,
    val authorized: Boolean
)

/**
 * Pure state contract. A request is not executable until the user confirms it,
 * and it expires quickly so a stale request cannot send into a later chat.
 */
internal class WhatsAppQuotedReplyRequestStore(
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
    private val ttlMs: Long = DEFAULT_TTL_MS
) {
    private var pending: PendingWhatsAppQuotedReply? = null

    init {
        require(ttlMs > 0L) { "ttlMs must be positive" }
    }

    @Synchronized
    fun begin(targetText: String, replyText: String): Boolean {
        val target = targetText.trim()
        val reply = replyText.trim()
        if (target.isBlank() || reply.isBlank()) return false
        pending = PendingWhatsAppQuotedReply(
            targetText = target,
            replyText = reply,
            createdAtMs = nowMs(),
            stage = WhatsAppQuotedReplyStage.FIND_TARGET_MESSAGE,
            authorized = false
        )
        return true
    }

    @Synchronized
    fun authorize(): Boolean {
        val value = current() ?: return false
        pending = value.copy(authorized = true)
        return true
    }

    @Synchronized
    fun current(): PendingWhatsAppQuotedReply? {
        val value = pending ?: return null
        val elapsed = nowMs() - value.createdAtMs
        if (elapsed < 0L || elapsed >= ttlMs) {
            pending = null
            return null
        }
        return value
    }

    @Synchronized
    fun advance(expected: WhatsAppQuotedReplyStage, next: WhatsAppQuotedReplyStage): Boolean {
        val value = current() ?: return false
        if (!value.authorized || value.stage != expected) return false
        pending = value.copy(stage = next)
        return true
    }

    @Synchronized
    fun clear() {
        pending = null
    }

    companion object {
        const val DEFAULT_TTL_MS = 180_000L
    }
}
