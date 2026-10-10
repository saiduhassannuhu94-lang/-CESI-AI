package com.cesi.assistant.core.task

/**
 * Splits one voice/text request into independent assistant actions.
 *
 * Safety principles:
 * - A separator is only active when the text on its right starts with a
 *   recognizable command shape.
 * - Message bodies with explicit content markers are kept intact; connectors
 *   inside a message are text, not task boundaries.
 * - The original text of each task is retained apart from surrounding spaces
 *   and separator punctuation.
 * - When a boundary is ambiguous, prefer one unsplit task over corrupting
 *   message content or inventing a second command.
 */
class MultiTaskPlanner {

    // Keep comma and word separators in separate searches. A comma followed
    // by the word "then" is not itself a task boundary; using one findAll()
    // expression would consume the whitespace needed to detect "then".
    private val commaSeparatorPattern = Regex(
        """,\s*(?:and\s+)?""",
        RegexOption.IGNORE_CASE
    )

    private val wordSeparatorPattern = Regex(
        """\s+(?:and\s+then|after\s+that|then|sannan|daga\s+nan|sai|and|&)\s+""",
        RegexOption.IGNORE_CASE
    )

    private val actionStartPattern = Regex(
        """^(?:please\s+)?(?:open|launch|start|run|bude|buɗe|turn|switch|enable|disable|take|tell\s+me|show\s+me|show|check|get|search|google|call|kira|dial|buga|ussd|send|tura|message|reply|amsa|set|play|stop|increase|decrease|raise|lower|mute|unmute|find|nemo|bincika|explain|define|kunna|kashe|ƙara|kara|rage|what\s+time|what\s+is|what's|how\s+much|where\s+am\s+i|what\s+day|give\s+me\s+advice|i\s+need\s+advice)\b""",
        RegexOption.IGNORE_CASE
    )

    fun split(input: String): List<String> {
        var remainder = input.trim()
        if (remainder.isBlank()) return emptyList()

        val tasks = mutableListOf<String>()

        while (true) {
            val candidates = (
                commaSeparatorPattern.findAll(remainder).toList() +
                    wordSeparatorPattern.findAll(remainder).toList()
                ).sortedBy { it.range.first }

            val boundary = candidates.firstOrNull { match ->
                val left = remainder.substring(0, match.range.first).trim()
                val right = remainder.substring(match.range.last + 1).trim()

                left.isNotBlank() &&
                    right.isNotBlank() &&
                    actionStartPattern.containsMatchIn(right) &&
                    !isMessageContent(left)
            } ?: break

            val left = remainder.substring(0, boundary.range.first).trim()
                .trimEnd(',', ';', '&')
                .trim()
            val right = remainder.substring(boundary.range.last + 1).trim()

            if (left.isNotBlank() && right.isNotBlank()) {
                tasks += left
                remainder = right
            } else {
                // Defensive progress guarantee if a future pattern change
                // introduces a malformed edge boundary.
                break
            }
        }

        if (remainder.isNotBlank()) tasks += remainder
        return tasks
    }

    private fun isMessageContent(prefix: String): Boolean {
        val lower = prefix.trim().lowercase()
        val messageCommand = lower.startsWith("send ") ||
            lower.startsWith("message ") ||
            lower.startsWith("reply ") ||
            lower.startsWith("reply to ") ||
            lower.startsWith("tell ") ||
            lower.startsWith("tura ") ||
            lower.startsWith("amsa ")

        if (!messageCommand) return false

        return listOf(
            " saying ",
            " that says ",
            " with the message ",
            " message saying ",
            " cewa ",
            " yana cewa ",
            " tana cewa ",
            " text cewa "
        ).any { lower.contains(it) }
    }
}
