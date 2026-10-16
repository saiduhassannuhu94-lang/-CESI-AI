package com.cesi.assistant.features.notifications

import java.text.Normalizer
import java.util.Locale

/**
 * Normalizes WhatsApp's version- and locale-dependent accessibility labels.
 * This only identifies candidate controls; the service still requires a
 * unique visible target and a visible reaction picker before acting.
 */
internal object WhatsAppReactionAccessibilityMatcher {
    private val aliases = mapOf(
        "laugh" to "😂",
        "laughing" to "😂",
        "laughing face" to "😂",
        "face laughing" to "😂",
        "face with tears of joy" to "😂",
        "haha" to "😂",
        "hahaha" to "😂",
        "love" to "❤️",
        "heart" to "❤️",
        "red heart" to "❤️",
        "like" to "👍",
        "thumb up" to "👍",
        "thumbs up" to "👍",
        "thumbs-up" to "👍",
        "sad" to "😢",
        "sad face" to "😢",
        "cry" to "😢",
        "crying" to "😢",
        "crying face" to "😢",
        "angry" to "😡",
        "angry face" to "😡",
        "wow" to "😮",
        "surprised" to "😮",
        "thanks" to "🙏",
        "thankful" to "🙏",
        "folded hands" to "🙏"
    )

    private val knownEmojis = setOf("😂", "❤️", "👍", "😢", "😡", "😮", "🙏")

    fun canonicalReaction(label: String): String? {
        val raw = label.trim()
        if (raw in knownEmojis) return raw
        var normalized = normalizeLabel(raw)
        repeat(2) {
            normalized = when {
                normalized.startsWith("react with ") -> normalized.removePrefix("react with ").trim()
                normalized.startsWith("reaction ") -> normalized.removePrefix("reaction ").trim()
                normalized.startsWith("react ") -> normalized.removePrefix("react ").trim()
                normalized.startsWith("emoji ") -> normalized.removePrefix("emoji ").trim()
                else -> normalized
            }
        }
        normalized = normalized
            .replace(Regex("""\s+emoji$"""), "")
            .replace(Regex("""\s+reaction$"""), "")
            .trim()
        if (normalized in knownEmojis) return normalized
        return aliases[normalized]
    }

    fun matchesRequestedReaction(label: String, requestedEmoji: String): Boolean =
        label.trim() == requestedEmoji || canonicalReaction(label) == requestedEmoji

    fun matchesMessageText(expected: String, visibleText: String): Boolean {
        val normalizedExpected = normalizeMessageText(expected)
        return normalizedExpected.isNotBlank() &&
            normalizedExpected == normalizeMessageText(visibleText)
    }

    private fun normalizeLabel(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace(Regex("""[\u00A0\s]+"""), " ")
            .trim()

    private fun normalizeMessageText(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .replace(Regex("""[\u00A0\s]+"""), " ")
            .trim()
}
