package com.cesi.assistant.features.messaging

import android.content.Context

/**
 * Local conversation state for CESI's message-copilot flow.
 *
 * It deliberately stores only the latest actionable WhatsApp message so a
 * follow-up such as "tell him I'm coming" has a deterministic recipient.
 */
class MessageCopilotEngine(context: Context) {
    private val prefs = context.getSharedPreferences("cesi_message_copilot", Context.MODE_PRIVATE)

    fun rememberIncoming(sender: String, message: String) {
        val cleanSender = sender.trim()
        val cleanMessage = message.trim()
        if (cleanSender.isBlank() || cleanMessage.isBlank()) return

        prefs.edit()
            .putString(KEY_SENDER, cleanSender)
            .putString(KEY_MESSAGE, cleanMessage)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun latest(): MessageContext? {
        val sender = prefs.getString(KEY_SENDER, null)?.trim().orEmpty()
        val message = prefs.getString(KEY_MESSAGE, null)?.trim().orEmpty()
        if (sender.isBlank() || message.isBlank()) return null
        return MessageContext(sender, message, prefs.getLong(KEY_TIMESTAMP, 0L))
    }

    fun clear() {
        prefs.edit().remove(KEY_SENDER).remove(KEY_MESSAGE).remove(KEY_TIMESTAMP).apply()
    }

    fun suggestions(): List<String> {
        val context = latest() ?: return emptyList()
        val message = context.message.lowercase()

        return when {
            containsAny(message, "where are you", "ina kake", "ina kike", "ina kake yanzu") ->
                listOf(
                    "Ina gida yanzu.",
                    "Ina hanya, zan sanar da kai idan na iso.",
                    "Ina nan, zan dawo nan ba da jimawa ba."
                )

            containsAny(message, "when are you coming", "when will you come", "yaushe zaka", "yaushe zaki", "yaushe zaka dawo") ->
                listOf(
                    "Zan dawo nan ba da jimawa ba.",
                    "Zan dawo da misalin karfe 6.",
                    "Har yanzu ban tabbatar da lokacin ba, zan sanar da kai."
                )

            containsAny(message, "how are you", "lafiya", "ya ya", "yaya kake", "yaya kike") ->
                listOf(
                    "Lafiya lau, na gode.",
                    "Lafiya kalau. Kai fa?",
                    "Alhamdulillah, komai lafiya."
                )

            containsAny(message, "thank", "na gode", "nagode") ->
                listOf(
                    "Babu komai.",
                    "You're welcome.",
                    "Komai lafiya."
                )

            containsAny(message, "sorry", "yi hakuri", "yi haƙuri") ->
                listOf(
                    "Babu komai, komai ya wuce.",
                    "Ba damuwa.",
                    "It's okay."
                )

            else ->
                listOf(
                    "Ka ba ni amsa kadan daga abin da kake son fada.",
                    "Zan iya shirya reply idan ka gaya min abin da kake son isarwa.",
                    "Ka ce, misali: “Tell him I'll call later.”"
                )
        }
    }

    fun parseCommand(command: String): MessageAction? {
        val clean = command.trim().replace(Regex("\\s+"), " ")
        if (clean.isBlank()) return null

        val lower = clean.lowercase()

        if (containsAny(lower, "what should i say", "what can i reply", "what do i say", "me zan ce", "me zan tura", "me zan amsa")) {
            return MessageAction.Suggest
        }

        if (containsAny(lower, "don't reply", "do not reply", "no reply", "kar ka reply", "kar a reply", "kar ka amsa")) {
            return MessageAction.Ignore
        }

        val reaction = Regex("""^(?:react|react with|yi reaction da|yi react da)\s+(.+)$""", RegexOption.IGNORE_CASE)
            .find(clean)?.groupValues?.getOrNull(1)?.trim()
        if (!reaction.isNullOrBlank()) {
            return MessageAction.React(reaction)
        }

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

    private fun extractReplyText(command: String): String? {
        val patterns = listOf(
            Regex("""^(?:reply|amsa|reply to him|reply to her)\s+(?:that\s+)?(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^(?:tell|say|send)\s+(?:him|her|them)\s+(?:that\s+)?(.+)$""", RegexOption.IGNORE_CASE),
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

    companion object {
        private const val KEY_SENDER = "sender"
        private const val KEY_MESSAGE = "message"
        private const val KEY_TIMESTAMP = "timestamp"
    }
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
