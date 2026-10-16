package com.cesi.assistant.features.messaging

import java.util.Locale

/**
 * In-process, short-lived context for replying to the latest WhatsApp notification.
 *
 * Message text is deliberately not written to SharedPreferences, files, or the
 * general history store. It is retained only in process memory and checked
 * against a monotonic TTL whenever it is read. Process death clears it.
 */
class MessageCopilotEngine {
    fun rememberIncoming(sender: String, message: String) =
        processStore.remember(sender, message)

    fun latest(): MessageContext? = processStore.latest()

    fun clear() = processStore.clear()

    fun suggestions(): List<String> =
        latest()?.let { MessageCopilotParser.suggestionsFor(it.message) } ?: emptyList()

    fun parseCommand(command: String): MessageAction? =
        MessageCopilotParser.parse(command)

    companion object {
        private val processStore = EphemeralMessageContextStore()
    }
}

/** Pure, testable memory-only context with expiry and explicit clearing. */
internal class EphemeralMessageContextStore(
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
    private val ttlMs: Long = DEFAULT_TTL_MS
) {
    private var current: MessageContext? = null

    init {
        require(ttlMs > 0L) { "ttlMs must be positive" }
    }

    @Synchronized
    fun remember(sender: String, message: String) {
        val cleanSender = sender.trim()
        val cleanMessage = message.trim()
        if (cleanSender.isBlank() || cleanMessage.isBlank()) {
            current = null
            return
        }
        current = MessageContext(cleanSender, cleanMessage, nowMs())
    }

    @Synchronized
    fun latest(): MessageContext? {
        val value = current ?: return null
        val elapsed = nowMs() - value.timestamp
        if (elapsed < 0L || elapsed >= ttlMs) {
            current = null
            return null
        }
        return value
    }

    @Synchronized
    fun clear() {
        current = null
    }

    companion object {
        const val DEFAULT_TTL_MS = 5 * 60 * 1000L
    }
}

/** Pure command/suggestion logic. Keeping this separate makes it unit-testable. */
object MessageCopilotParser {
    fun suggestionsFor(message: String): List<String> {
        val lower = message.trim().lowercase()

        return when {
            containsAny(lower, "where are you", "ina kake", "ina kike", "ina kake yanzu") ->
                listOf(
                    "Ina gida yanzu.",
                    "Ina hanya, zan sanar da kai idan na iso.",
                    "Ina nan, zan dawo nan ba da jimawa ba."
                )

            containsAny(lower, "when are you coming", "when will you come", "yaushe zaka", "yaushe zaki", "yaushe zaka dawo") ->
                listOf(
                    "Zan dawo nan ba da jimawa ba.",
                    "Zan dawo da misalin karfe 6.",
                    "Har yanzu ban tabbatar da lokacin ba, zan sanar da kai."
                )

            containsAny(lower, "how are you", "lafiya", "ya ya", "yaya kake", "yaya kike") ->
                listOf(
                    "Lafiya lau, na gode.",
                    "Lafiya kalau. Kai fa?",
                    "Alhamdulillah, komai lafiya."
                )

            containsAny(lower, "thank", "na gode", "nagode") ->
                listOf("Babu komai.", "You're welcome.", "Komai lafiya.")

            containsAny(lower, "sorry", "yi hakuri", "yi haƙuri") ->
                listOf("Babu komai, komai ya wuce.", "Ba damuwa.", "It's okay.")

            else -> emptyList()
        }
    }

    fun parse(command: String): MessageAction? {
        val raw = command.trim().replace(Regex("\\s+"), " ")
        val clean = raw
            .replace(Regex("""^(?:(?:hey|hi)\s+)?cesi[,;:]?\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^[,;:]\s*"""), "")
            .trim()
        if (clean.isBlank()) return null

        val lower = clean.lowercase(Locale.ROOT)

        if (containsAny(lower, "what should i say", "what can i reply", "what do i say", "me zan ce", "me zan tura", "me zan amsa")) {
            return MessageAction.Suggest
        }

        if (containsAny(lower, "don't reply", "do not reply", "no reply", "kar ka reply", "kar a reply", "kar ka amsa")) {
            return MessageAction.Ignore
        }

        val reactionPatterns = listOf(
            Regex("""^(?:react with|react to|react)\s+(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:(?:please|don Allah)\s+)?(?:(?:ka|ki|ku)\s+)?(?:yi|saka|sa|dora|daura)\s+(?:(?:min|mini)\s+)?(?:reaction|react)(?:\s+(?:da|na|with|to))?\s+(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:reaction|react)\s+(?:da|na|with)?\s*(.+)$""", RegexOption.IGNORE_CASE)
        )
        val reactionRaw = reactionPatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(clean)?.groupValues?.getOrNull(1)?.trim()
        }
        val reaction = reactionRaw?.let(::normalizeReaction)
        if (!reaction.isNullOrBlank()) return MessageAction.React(reaction)

        val sticker = Regex("""^(?:reply with|send)\s+(?:a\s+)?sticker$""", RegexOption.IGNORE_CASE).matches(clean) ||
            Regex("""^(?:reply|tura)\s+(?:da\s+)?sticker$""", RegexOption.IGNORE_CASE).matches(clean)
        if (sticker) return MessageAction.Sticker

        val gif = Regex("""^(?:reply with|send)\s+(?:a\s+)?gif$""", RegexOption.IGNORE_CASE).matches(clean) ||
            Regex("""^(?:reply|tura)\s+(?:da\s+)?gif$""", RegexOption.IGNORE_CASE).matches(clean)
        if (gif) return MessageAction.Gif

        val image = Regex("""^(?:send|tura)\s+(?:him|her|it)\s+(?:a\s+)?(?:picture|photo|image)\s+(.+)$""", RegexOption.IGNORE_CASE)
            .find(clean)?.groupValues?.getOrNull(1)?.trim()
        if (!image.isNullOrBlank()) return MessageAction.Image(image)

        val reply = extractReplyText(clean)
        if (!reply.isNullOrBlank()) return MessageAction.TextReply(reply)

        return null
    }

    private fun normalizeReaction(raw: String): String {
        val value = raw.trim()
        val normalized = value
            .lowercase(Locale.ROOT)
            .replace(Regex("""\s+emoji$"""), "")
            .trim()

        return when (normalized) {
            "laugh", "laughing", "dariya", "na dariya", "face with tears of joy", "laughing face" -> "😂"
            "love", "heart", "red heart", "zuciya", "soyayya", "kauna", "ƙauna" -> "❤️"
            "like", "thumbs up", "thumb up", "approve", "yarda", "babban yatsa" -> "👍"
            "sad", "cry", "crying", "kuka", "baƙin ciki", "bakin ciki" -> "😢"
            "angry", "anger", "fushi", "haushi" -> "😡"
            "wow", "surprised", "mamaki" -> "😮"
            else -> value
        }
    }

    private fun extractReplyText(command: String): String? {
        val patterns = listOf(
            Regex("""^(?:reply|amsa|reply to him|reply to her)\s+(?:that\s+)?(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:tell|say|send)\s+(?:him|her|them)\s+(?:a\s+message\s+)?(?:saying\s+|that\s+)?(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:just\s+)?say\s+(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:just\s+)?ce\s+(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:tura|tura masa|tura mata)\s+(.+)$""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val value = pattern.find(command)?.groupValues?.getOrNull(1)?.trim()
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    private fun containsAny(value: String, vararg terms: String): Boolean =
        terms.any { value.contains(it) }
}

data class MessageContext(
    val sender: String,
    val message: String,
    val timestamp: Long
)

sealed class MessageAction {
    data object Suggest : MessageAction()
    data object Ignore : MessageAction()
    data class TextReply(val text: String) : MessageAction()
    data class React(val emoji: String) : MessageAction()
    data object Sticker : MessageAction()
    data object Gif : MessageAction()
    data class Image(val query: String) : MessageAction()
}
