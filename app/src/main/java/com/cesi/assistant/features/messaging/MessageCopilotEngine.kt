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

        parseCaptionStyleCommand(clean)?.let { return it }
        parseQuotedReply(clean)?.let { return it }
        if (looksLikeQuotedReply(clean)) return null

        parseDirectMessage(clean)?.let { return it }
        parseMediaShare(clean)?.let { return it }

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
        var clean = command.trim().replace(Regex("\\s+"), " ")
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
            // Common Hausa word order: "ka saka wa wannan saƙon reaction na dariya".
            Regex("""^(?:(?:don\s+Allah)\s+)?(?:(?:ka|ki)\s+)?(?:saka|sa|dora|daura|yi)\s+wa\s+(?:(?:wannan|wancan)\s+)?(?:saƙon|sakon|saƙo|sako|message)(?:\s+(?:nan|da ya shigo|da aka aiko|da aka turo|da aka karɓa|da aka karba))?\s+(?:reaction|react)\s+(?:da|na|din|ɗin|with)\s+(.+)$""", RegexOption.IGNORE_CASE),
            // "yi reaction a kan wannan saƙon da dariya".
            Regex("""^(?:(?:don\s+Allah)\s+)?(?:(?:ka|ki)\s+)?(?:yi|saka|sa|dora|daura)\s+(?:masa|mata)?\s*(?:reaction|react)\s+(?:a kan|akan|ga|to)\s+(?:(?:wannan|wancan|this|that|the)\s+)?(?:saƙon|sakon|saƙo|sako|message)(?:\s+(?:nan|da ya shigo|da aka aiko|da aka turo|da aka karɓa|da aka karba))?\s+(?:da|na|with)\s+(.+)$""", RegexOption.IGNORE_CASE),
            // English references to the latest/last message.
            Regex("""^(?:please\s+)?react\s+to\s+(?:the\s+)?(?:last|latest|previous)\s+message\s+with\s+(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:please\s+)?react\s+to\s+(?:(?:the|this|that)\s+)?(?:last|latest|previous)\s+(?:whatsapp\s+)?message\s+with\s+(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:please\s+)?react\s+to\s+(?:his|her|their)\s+message\s+with\s+(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:please\s+)?(?:put|add|set)\s+(.+?)\s+reaction\s+(?:on|to)\s+(?:(?:(?:the|this|that)\s+)?(?:last|latest|previous)\s+|(?:(?:the|this|that)\s+)?)message$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:please\s+)?(?:react|add|put|set)\s+(?:a\s+)?reaction\s+(?:to\s+(?:this|that|the)\s+message\s+)?(?:with|using|as|da|na)\s+(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:please\s+)?react\s+to\s+(?:this|that|the)\s+message\s+with\s+(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:(?:don\s+Allah)\s+)?(?:(?:ka|ki)\s+)?yi\s+wa\s+(?:wannan\s+)?(?:saƙon|sakon|saƙo|sako)(?:\s+nan)?\s+reaction\s+da\s+(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:please\s+)?(?:react\s+with)\s+(.+?)\s+to\s+(?:this|that|the)\s+message$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:please\s+)?(?:react\s+with)\s+(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:(?:ka|ki)\s+)?(?:yi|saka|sa|dora|daura)\s+(?:(?:masa|mata)\s+)?(?:reaction|react)\s+(?:da|na|din|ɗin|with)\s+(.+?)\s+(?:a kan|akan|ga|to|on)\s+(?:(?:wannan|wancan|this|that|the|last|latest|previous)\s+)?(?:saƙon|sakon|saƙo|sako|message)(?:\s+(?:nan|da ya shigo|da aka aiko|da aka turo|da aka karɓa|da aka karba))?$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:(?:don\s+Allah)\s+)?(?:(?:ka|ki)\s+)?(?:yi|saka|sa|dora|daura)\s+(?:(?:masa|mata)\s+)?(?:reaction|react)\s+(?:da|na|din|ɗin|with)?\s*(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:(?:don\s+Allah)\s+)?(?:yi|saka|sa|dora|daura)\s+(?:masa|mata)\s+(?:reaction|react)\s+(?:da|na|din|ɗin|with)\s+(.+)$""", RegexOption.IGNORE_CASE),
            // Voice commands often name the recipient's message rather than saying "this message".
            Regex("""^(?:please\s+)?(?:react|put|add|send)\s+(?:a\s+)?(.+?)\s+reaction\s+(?:to|on)\s+(?:(?:(?:the|this|that)\s+)?(?:last|latest|previous)\s+|(?:his|her|their|this|that|the)\s+)?(?:whatsapp\s+)?message$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:please\s+)?react\s+with\s+(.+?)\s+(?:on|to)\s+(?:(?:(?:the|this|that)\s+)?(?:last|latest|previous)\s+|(?:his|her|their|this|that|the)\s+)?(?:whatsapp\s+)?message$""", RegexOption.IGNORE_CASE),
            // Hausa speech recognition commonly keeps the possessive suffix attached to "sako".
            Regex("""^(?:(?:don\s+Allah)\s+)?(?:(?:ka|ki)\s+)?(?:yi|saka|sa|dora|daura)\s+(?:(?:masa|mata)\s+)?(?:reaction|react)\s+(?:da|na|din|ɗin|with)\s+(.+?)\s+(?:a kan|akan|ga)\s+(?:(?:wannan|wancan|na ƙarshe|na karshe|sabon)\s+)?(?:saƙonsa|sakonsa|saƙonta|sakonta|saƙonsu|sakonsu|saƙon|sakon|saƙo|sako)(?:\s+(?:nan|sa|da ya shigo|da ya turo|da ya aiko|da aka aiko|da aka turo))?$""", RegexOption.IGNORE_CASE),
        )

        // Do not stop at the first regex match: broad patterns such as
        // "react with X" can capture a target phrase plus "on the latest message".
        // Continue until the captured text is actually a known emoji/reaction.
        for (pattern in specificPatterns) {
            val candidate = pattern.find(command)?.groupValues?.getOrNull(1)?.trim()
            if (candidate.isNullOrBlank()) continue
            val action = reactionAction(candidate)
            if (action != null) return action
        }

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
        if (!direct.isNullOrBlank()) return reactionAction(direct)

        return null
    }

    private fun reactionAction(raw: String): MessageAction.React? {
        val emoji = normalizeReaction(raw)
        return if (isKnownReaction(emoji) || containsEmoji(emoji)) {
            MessageAction.React(emoji)
        } else {
            null
        }
    }

    private fun isKnownReaction(value: String): Boolean = value in setOf(
        "😂", "❤️", "👍", "😢", "😡", "😮",
        "laugh", "laughing", "dariya", "na dariya", "funny", "face with tears of joy", "laughing face",
        "love", "heart", "red heart", "zuciya", "soyayya", "kauna", "ƙauna",
        "like", "thumbs up", "thumb up", "approve", "yarda", "babban yatsa",
        "sad", "cry", "crying", "kuka", "baƙin ciki", "bakin ciki",
        "angry", "anger", "fushi", "haushi", "wow", "surprised", "mamaki"
    )

    private fun containsEmoji(value: String): Boolean =
        value.codePoints().anyMatch { codePoint ->
            codePoint in 0x1F000..0x1FAFF || codePoint == 0x2764 || codePoint == 0xFE0F
        }



    private fun looksLikeQuotedReply(command: String): Boolean =
        Regex(
            """^(?:please\s+)?(?:reply\s+to(?:\s+the)?\s+(?:old\s+)?message|amsa(?:\s+wa)?(?:\s+wannan)?\s+(?:saƙon|sakon|message))\s+["“]""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(command)

    /**
     * A quoted reply identifies the exact older message instead of relying on a
     * short-lived notification. Quotes are required so target text and reply text
     * cannot be confused by the parser.
     */
    private fun parseQuotedReply(command: String): MessageAction.QuotedReply? {
        val pattern = Regex(
            """^(?:please\s+)?(?:reply\s+to(?:\s+the)?\s+(?:old\s+)?message|amsa(?:\s+wa)?(?:\s+wannan)?\s+(?:saƙon|sakon|message))\s+["“](.+?)["”]\s+(?:with|saying|that\s+says|da|cewa)\s+["“](.+?)["”]$""",
            RegexOption.IGNORE_CASE
        )
        val match = pattern.find(command) ?: return null
        val target = match.groupValues[1].trim()
        val reply = match.groupValues[2].trim()
        if (target.isBlank() || reply.isBlank()) return null
        return MessageAction.QuotedReply(targetText = target, replyText = reply)
    }

    /**
     * Media sharing is user-mediated: the system picker grants access to one
     * selected item, and WhatsApp still requires the user to choose a destination
     * and press Send/Post. No broad media permission is requested.
     */

    private fun parseCaptionStyleCommand(command: String): MessageAction? {
        val lower = command.trim().lowercase(Locale.ROOT)
        if (lower in setOf(
                "forget my caption style",
                "clear caption style",
                "soke salon caption",
                "manta salon caption",
                "manta salon rubutun caption"
            )
        ) return MessageAction.ClearCaptionStyle

        val patterns = listOf(
            Regex("""^(?:remember|learn|set|use)\s+(?:my\s+)?caption\s+(?:style|tone)\s+(?:as|to|is)?\s*(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^for\s+future\s+captions,?\s+(?:use|make\s+(?:them|it))\s+(?:a\s+)?(.+?)(?:\s+style)?$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:remember\s+)?(?:i\s+prefer|use)\s+(.+?)\s+captions?$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:ka\s+riƙa|ka\s+rinka|ka\s+rika)\s+amfani\s+da\s+salon\s+(.+?)\s+wajen\s+captions?$""", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val candidate = pattern.find(command)?.groupValues?.getOrNull(1)?.trim() ?: continue
            val style = WhatsAppCaptionStyle.fromPhrase(candidate) ?: continue
            return MessageAction.RememberCaptionStyle(style.name)
        }
        return null
    }

    private fun parseMediaShare(command: String): MessageAction.ShareMedia? {
        val lower = command.lowercase(Locale.ROOT)
        val hasShareVerb = Regex(
            """\b(?:send|share|upload|post|put|tura|aika|dora|daura|saka)\b""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(command)
        if (!hasShareVerb) return null

        val asStatus = Regex(
            """\b(?:my\s+)?(?:whatsapp\s+)?status\b""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(command)
        val captionMatch = Regex(
            """\bcaption(?:\s+(?:din|ɗin))?\s*[:,-]?\s*(.+)$""",
            RegexOption.IGNORE_CASE
        ).find(command)
        val hasCaptionIntent = captionMatch != null ||
            Regex("""\bwith\s+(?:the\s+)?caption\b""", RegexOption.IGNORE_CASE).containsMatchIn(command)
        if (!asStatus && !hasCaptionIntent) return null

        val mediaKind = when {
            Regex("""\b(?:video|clip)\b""", RegexOption.IGNORE_CASE).containsMatchIn(command) -> "video"
            Regex("""\b(?:photo|picture|image|hoto|hoton|hoto(?:n)?\s*)\b""", RegexOption.IGNORE_CASE).containsMatchIn(command) -> "image"
            else -> "any"
        }
        val providedCaption = captionMatch?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)
        val caption = providedCaption ?: when {
                asStatus && mediaKind == "video" -> "Ga ɗan abin da nake so in raba. ✨"
                asStatus && mediaKind == "image" -> "Wani sabon lokaci, Alhamdulillah ✨"
                mediaKind == "video" -> "Ga bidiyon da nake son rabawa."
                mediaKind == "image" -> "Ga hoton da nake son rabawa."
                else -> "Ga wannan media."
            }
        return MessageAction.ShareMedia(
            caption = caption,
            mediaKind = mediaKind,
            asStatus = asStatus,
            captionProvided = providedCaption != null
        )
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
        val withoutTarget = value.replace(
            Regex(
                """\s+(?:a\s+kan|akan|ga|to|on)\s+(?:(?:wannan|wancan|this|that|the|last|latest|previous)\s+)?(?:saƙon|sakon|saƙo|sako|message)(?:\s+(?:nan|da ya shigo|da aka aiko|da aka turo|da aka karɓa|da aka karba))?$""",
                RegexOption.IGNORE_CASE
            ),
            ""
        ).trim()
        val normalized = withoutTarget
            .lowercase(Locale.ROOT)
            .replace(Regex("""^(?:reaction|react)\s+(?:da|na|din|ɗin|with)\s+"""), "")
            .replace(Regex("""^(?:a|an|the)\s+"""), "")
            .replace(Regex("""^(?:din|ɗin)\s+"""), "")
            .replace(Regex("""(?:\s+(?:emoji|reaction))+$"""), "")
            .trim()

        return when (normalized) {
            "laugh", "laughing", "laughter", "funny", "haha", "hahaha", "dariya", "na dariya", "face with tears of joy", "laughing face", "face laughing", "tears of joy" -> "😂"
            "love", "heart", "red heart", "zuciya", "soyayya", "kauna", "ƙauna" -> "❤️"
            "like", "thumbs up", "thumbs-up", "thumb up", "approve", "yarda", "babban yatsa" -> "👍"
            "sad", "cry", "crying", "crying face", "kuka", "baƙin ciki", "bakin ciki" -> "😢"
            "angry", "anger", "fushi", "haushi" -> "😡"
            "wow", "surprised", "mamaki" -> "😮"
            else -> withoutTarget
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
    data class QuotedReply(val targetText: String, val replyText: String) : MessageAction()
    data class RememberCaptionStyle(val style: String) : MessageAction()
    data object ClearCaptionStyle : MessageAction()
    data class ShareMedia(val caption: String, val mediaKind: String, val asStatus: Boolean, val captionProvided: Boolean = false) : MessageAction()
}