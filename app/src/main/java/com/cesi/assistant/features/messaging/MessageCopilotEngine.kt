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

            containsAny(lower, "sorry", "yi hakuri", "yi haƙuri") -> listOf("Babu komai, komai ya wuce.", "Ba damuwa.", "It's okay.")

            else -> emptyList()
        }
    }

    fun parse(command: String): MessageAction? {
        val clean = normalizeCommand(command)
        if (clean.isBlank()) return null

        val lower = clean.lowercase()

        parseDirectMessage(clean)?.let { return it }

        if (containsAny(lower, "what should i say", "what can i reply", "what do i say", "me zan ce", "me zan tura", "me zan amsa")) {
            return MessageAction.Suggest
        }

        if (containsAny(lower, "don't reply", "do not reply", "no reply", "kar ka reply", "kar a reply", "kar ka amsa")) {
            return MessageAction.Ignore
        }

        parseReaction(clean)?.let { return it }

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

    /**
     * Voice transcripts can include the CESI wake phrase and polite lead-ins.
     * Normalize them at the message-parser boundary before command matching.
     */
    private fun normalizeCommand(command: String): String {
        var clean = command.trim().replace(Regex("\s+"), " ")
        repeat(2) {
            clean = clean.replace(
                Regex("""^(?:(?:hey|ok|okay)\s+)?cesi\b[\s,:-]*""", RegexOption.IGNORE_CASE),
                ""
            ).trim()
            clean = clean.replace(
                Regex("""^(?:please|kindly|can you|could you|would you(?: please)?)\s+""", RegexOption.IGNORE_CASE),
                ""
            ).trim()
        }
        return clean
    }

    /**
     * Recognize explicit English and Hausa reaction commands. Specific "react to
     * this message with X" forms must be checked before generic "react to X",
     * otherwise the entire phrase can be mistaken for the emoji name.
     */
    private fun parseReaction(command: String): MessageAction.React? {
        val specificPatterns = listOf(
            Regex("""^(?:please\s+)?(?:react|add|put|set)\s+(?:a\s+)?reaction\s+(?:to\s+(?:this|that|the)\s+message\s+)?(?:with|using|as|da|na)\s+(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:please\s+)?react\s+to\s+(?:this|that|the)\s+message\s+with\s+(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:(?:don\s+Allah)\s+)?(?:(?:ka|ki)\s+)?yi\s+wa\s+(?:wannan\s+)?(?:saƙon|sakon|saƙo|sako)(?:\s+nan)?\s+reaction\s+da\s+(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:please\s+)?(?:react\s+with)\s+(.+?)\s+to\s+(?:this|that|the)\s+message$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:please\s+)?(?:react\s+with)\s+(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:(?:ka|ki)\s+)?(?:yi|saka|sa|dora)\s+(?:(?:masa|mata)\s+)?(?:reaction|react)\s+(?:da|na|din|ɗin|with)\s+(.+?)\s+(?:a kan|akan|ga|to)\s+(?:wannan\s+)?(?:saƙon|sakon|saƙo|sako|message)(?:\s+nan)?$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:(?:don\s+Allah)\s+)?(?:(?:ka|ki)\s+)?(?:yi|saka|sa|dora)\s+(?:(?:masa|mata)\s+)?(?:reaction|react)\s+(?:da|na|din|ɗin|with)?\s*(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:(?:don\s+Allah)\s+)?(?:yi|saka|sa|dora)\s+(?:masa|mata)\s+(?:reaction|react)\s+(?:da|na|din|ɗin|with)\s+(.+)$""", RegexOption.IGNORE_CASE),
        )

        val specific = specificPatterns.firstNotNullOfOrNull {
            it.find(command)?.groupValues?.getOrNull(1)?.trim()
        }
        if (!specific.isNullOrBlank()) return MessageAction.React(normalizeReaction(specific))

        val genericTarget = Regex(
            """^(?:please\s+)?react\s+to\s+(.+)$""",
            RegexOption.IGNORE_CASE
        ).find(command)?.groupValues?.getOrNull(1)?.trim()
        if (!genericTarget.isNullOrBlank()) {
            val normalizedTarget = normalizeReaction(genericTarget)
            if (isKnownReaction(normalizedTarget) || containsEmoji(genericTarget)) {
                return MessageAction.React(normalizedTarget)
            }
            return null
        }

        val direct = Regex(
            """^(?:please\s+)?react\s+(.+)$""",
            RegexOption.IGNORE_CASE
        ).find(command)?.groupValues?.getOrNull(1)?.trim()
        if (!direct.isNullOrBlank()) return MessageAction.React(normalizeReaction(direct))

        return null
    }

    private fun isKnownReaction(value: String): Boolean = value in setOf(
        "😂", "❤️", "👍", "😢", "😡", "😮",
        "laugh", "laughing", "dariya", "na dariya", "face with tears of joy", "laughing face",
        "love", "heart", "red heart", "zuciya", "soyayya", "kauna", "ƙauna",
        "like", "thumbs up", "thumb up", "approve", "yarda", "babban yatsa",
        "sad", "cry", "crying", "kuka", "baƙin ciki", "bakin ciki",
        "angry", "anger", "fushi", "haushi", "wow", "surprised", "mamaki"
    )

    private fun containsEmoji(value: String): Boolean =
        value.codePoints().anyMatch { codePoint ->
            codePoint in 0x1F000..0x1FAFF || codePoint == 0x2764 || codePoint == 0xFE0F
        }

    private fun parseDirectMessage(command: String): MessageAction? {
        val number = """(\+?[0-9][0-9 ()-]{6,})"""
        val patterns = listOf(
            Regex("""^(?:send|message)\s+(?:(?:a\s+)?whatsapp\s+)?(?:message\s+)?(?:to\s+)?$number\s+(?:saying|that says|with the message|cewa|da cewa|:)\s+(.*)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:tura|aika)\s+(?:saƙon|sakon|saƙo|sako)\s+whatsapp\s+(?:zuwa\s+)?$number\s+(?:cewa|da cewa)\s+(.*)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:tura|aika)\s+whatsapp\s+(?:zuwa\s+)?$number\s+(?:cewa|da cewa)\s+(.*)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:tura|aika)\s+(?:saƙon|sakon|saƙo|sako)\s+(?:zuwa\s+)?$number\s+(?:cewa|da cewa)\s+(.*)$""", RegexOption.IGNORE_CASE)
        )
        val match = patterns.firstNotNullOfOrNull { it.find(command) }
        if (match == null) {
            val normalized = command.lowercase(Locale.ROOT)
            val resemblesDirectMessage = normalized.startsWith("send whatsapp") ||
                normalized.startsWith("send a whatsapp") ||
                normalized.startsWith("send message to") ||
                normalized.startsWith("message +") ||
                normalized.startsWith("message 0") ||
                normalized.startsWith("tura saƙon whatsapp") ||
                normalized.startsWith("tura sakon whatsapp") ||
                normalized.startsWith("tura sako zuwa") ||
                normalized.startsWith("tura saƙo zuwa") ||
                normalized.startsWith("aika sako zuwa") ||
                normalized.startsWith("tura whatsapp") ||
                normalized.startsWith("aika saƙon whatsapp") ||
                normalized.startsWith("aika sakon whatsapp") ||
                normalized.startsWith("aika whatsapp")
            return if (resemblesDirectMessage) MessageAction.InvalidDirectMessage else null
        }
        val rawNumber = match.groupValues[1]
        val message = match.groupValues[2].trim()
        if (message.isBlank()) return MessageAction.InvalidDirectMessage
        val phone = normalizeInternationalPhone(rawNumber)
            ?: return MessageAction.InvalidDirectMessage
        return MessageAction.DirectMessage(phone, message)
    }

    private fun normalizeInternationalPhone(raw: String): String? {
        val compact = raw.trim()
        var digits = compact.filter(Char::isDigit)
        if (compact.startsWith("00")) digits = digits.drop(2)
        else if (!compact.startsWith("+") && digits.startsWith("0")) return null
        if (digits.length !in 8..15 || digits.firstOrNull() == '0') return null
        return digits
    }

    private fun normalizeReaction(raw: String): String {
        val value = raw.trim().replace(Regex("""[.!?,]+$"""), "").trim()
        val normalized = value
            .lowercase(Locale.ROOT)
            .replace(Regex("""^(?:a|an|the)\s+"""), "")
            .replace(Regex("""^(?:din|ɗin)\s+"""), "")
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
            Regex("""^(?:tura|aika)\s+(?:masa|mata)\s+(?:(?:saƙo|sako|saƙon|sakon)\s+)?(?:cewa\s+)?(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:tura|aika)\s+(?:saƙo|sako|saƙon|sakon)\s+(?:cewa\s+)?(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:tura|aika)\s+(.+)$""", RegexOption.IGNORE_CASE)
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
    data class DirectMessage(val phoneNumber: String, val text: String) : MessageAction()
    data object InvalidDirectMessage : MessageAction()
    data class React(val emoji: String) : MessageAction()
    data object Sticker : MessageAction()
    data object Gif : MessageAction()
    data class Image(val query: String) : MessageAction()
}