package com.cesi.assistant.features.notifications

/** One-shot, in-memory request for a user-commanded WhatsApp reaction. */
internal object WhatsAppReactionCoordinator {
    private val store = WhatsAppReactionRequestStore()
    fun begin(message: String, emoji: String): Boolean = store.begin(message, emoji)
    fun current(): PendingWhatsAppReaction? = store.current()
    fun advance(expected: WhatsAppReactionStage, next: WhatsAppReactionStage): Boolean =
        store.advance(expected, next)
    fun clear() = store.clear()
}

internal enum class WhatsAppReactionStage { FIND_MESSAGE, FIND_REACTION }

internal data class PendingWhatsAppReaction(
    val message: String,
    val emoji: String,
    val createdAtMs: Long,
    val stage: WhatsAppReactionStage
)

/** Pure state contract with a short monotonic timeout for deterministic tests. */
internal class WhatsAppReactionRequestStore(
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
    private val ttlMs: Long = DEFAULT_TTL_MS
) {
    private var pending: PendingWhatsAppReaction? = null

    init {
        require(ttlMs > 0L) { "ttlMs must be positive" }
    }

    @Synchronized
    fun begin(message: String, emoji: String): Boolean {
        val cleanMessage = message.trim()
        val cleanEmoji = emoji.trim()
        if (cleanMessage.isBlank() || cleanEmoji.isBlank()) return false
        pending = PendingWhatsAppReaction(
            cleanMessage, cleanEmoji, nowMs(), WhatsAppReactionStage.FIND_MESSAGE
        )
        return true
    }

    @Synchronized
    fun current(): PendingWhatsAppReaction? {
        val value = pending ?: return null
        val elapsed = nowMs() - value.createdAtMs
        if (elapsed < 0L || elapsed >= ttlMs) {
            pending = null
            return null
        }
        return value
    }

    @Synchronized
    fun advance(expected: WhatsAppReactionStage, next: WhatsAppReactionStage): Boolean {
        val value = current() ?: return false
        if (value.stage != expected) return false
        pending = value.copy(stage = next)
        return true
    }

    @Synchronized
    fun clear() { pending = null }

    companion object { const val DEFAULT_TTL_MS = 120_000L }
}
