package com.cesi.assistant.features.notifications

/**
 * One-shot request for a user-commanded WhatsApp reaction.
 *
 * The request is in memory only, scoped to the exact notification message,
 * and expires quickly. This coordinator never reports that WhatsApp sent a
 * reaction; UI automation must still observe the result on the device.
 */
internal object WhatsAppReactionCoordinator {
    private val store = WhatsAppReactionRequestStore()

    fun begin(message: String, emoji: String): Boolean = store.begin(message, emoji)

    fun current(): PendingWhatsAppReaction? = store.current()

    fun advance(expected: WhatsAppReactionStage, next: WhatsAppReactionStage): Boolean =
        store.advance(expected, next)

    fun clear() = store.clear()
}

internal enum class WhatsAppReactionStage {
    FIND_MESSAGE,
    FIND_REACTION
}

internal data class PendingWhatsAppReaction(
    val message: String,
    val emoji: String,
    val createdAtMs: Long,
    val stage: WhatsAppReactionStage
)

/** Pure, testable, in-memory state with a short monotonic timeout. */
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
            message = cleanMessage,
            emoji = cleanEmoji,
            createdAtMs = nowMs(),
            stage = WhatsAppReactionStage.FIND_MESSAGE
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
    fun clear() {
        pending = null
    }

    companion object {
        const val DEFAULT_TTL_MS = 30_000L
    }
}
