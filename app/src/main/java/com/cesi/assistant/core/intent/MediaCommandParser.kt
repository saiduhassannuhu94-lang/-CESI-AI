package com.cesi.assistant.core.intent

import java.util.Locale

/**
 * Recognizes combined media-platform search and playback requests as one action.
 * Search results can be opened; automatic video selection/playback is not yet
 * implemented, so playback requests must report a partial result honestly.
 */
object MediaCommandParser {
    private val playbackSuffix = Regex(
        """\s+(?:(?:and\s+then|after\s+that|then|sannan|sai|and)\s+)?play\s+(?:it|the\s+first\s+(?:video|result)|a\s+video)\s*[.!?]*$""",
        RegexOption.IGNORE_CASE
    )
    private val platformFirst = Regex(
        """^(?:(?:open|launch|start|bude|buɗe)\s+(?:the\s+)?)?(youtube|tiktok)(?:\s+app)?(?:\s*,\s*|\s+)(?:(?:and\s+)?(?:search|serach|sarch|find)(?:\s+for)?\s+)(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val queryFirst = Regex(
        """^(?:search|serach|sarch|find)\s+(?:for\s+)?(.+?)\s+(?:on|in)\s+(youtube|tiktok)$""",
        RegexOption.IGNORE_CASE
    )
    private val hausaQueryFirst = Regex(
        """^(?:bincika|nemo)\s+(.+?)\s+a\s+(youtube|tiktok)$""",
        RegexOption.IGNORE_CASE
    )

    fun parse(input: String): AssistantIntent.MediaSearchAndPlay? {
        var command = normalize(input)
        if (command.isBlank()) return null
        val playMatch = playbackSuffix.find(command)
        val requestPlayback = playMatch != null
        if (playMatch != null) command = command.substring(0, playMatch.range.first).trim().trimEnd(',', ';').trim()
        command = command.trim().trimEnd('.', '!', '?').trim()
        if (command.isBlank()) return null

        val platformMatch = platformFirst.matchEntire(command)
        if (platformMatch != null) {
            val platform = platform(platformMatch.groupValues[1]) ?: return null
            val query = cleanQuery(platformMatch.groupValues[2])
            if (query.isBlank()) return null
            return AssistantIntent.MediaSearchAndPlay(platform, query, requestPlayback)
        }
        val queryMatch = queryFirst.matchEntire(command) ?: hausaQueryFirst.matchEntire(command)
        if (queryMatch != null) {
            val query = cleanQuery(queryMatch.groupValues[1])
            val platform = platform(queryMatch.groupValues[2]) ?: return null
            if (query.isBlank()) return null
            return AssistantIntent.MediaSearchAndPlay(platform, query, requestPlayback)
        }
        return null
    }

    private fun platform(value: String): VideoPlatform? = when (value.lowercase(Locale.ROOT)) {
        "youtube" -> VideoPlatform.YOUTUBE
        "tiktok" -> VideoPlatform.TIKTOK
        else -> null
    }

    private fun cleanQuery(value: String): String = value.trim().trimEnd(',', ';', '.', '!', '?').trim()

    private fun normalize(input: String): String {
        var command = input.trim().replace(Regex("\\s+"), " ")
        repeat(2) {
            command = command.replace(
                Regex("""^(?:(?:hey|ok|okay)\s+)?cesi\b[\s,:-]*""", RegexOption.IGNORE_CASE), ""
            ).trim()
            command = command.replace(
                Regex("""^(?:please|kindly|can you|could you|would you(?: please)?)\s+""", RegexOption.IGNORE_CASE), ""
            ).trim()
        }
        return command
    }
}
