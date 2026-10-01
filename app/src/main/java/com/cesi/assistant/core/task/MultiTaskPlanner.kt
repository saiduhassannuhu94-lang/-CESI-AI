package com.cesi.assistant.core.task

/**
 * Splits a voice request into independent CESI tasks.
 *
 * The parser is intentionally conservative: "and" is treated as a task
 * separator only when what follows clearly looks like another command.
 * This prevents message text and ordinary questions from being cut in half.
 */
class MultiTaskPlanner {

    fun split(input: String): List<String> {
        val normalized = input.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return emptyList()

        val actionStart =
            """(?i)(?:please )?(?:open|launch|start|run|bude|buɗe|turn|switch|enable|disable|take|tell|show|check|search|google|call|kira|dial|ussd|send|tura|message|reply|amsa|set|play|stop|increase|decrease|raise|lower|mute|unmute|find|nemo|bincika|explain|define)\b"""

        val explicitSeparator =
            Regex("""\s+(?:sannan|sai|daga nan|then|and then|after that)\s+""", RegexOption.IGNORE_CASE)

        val actionSeparator =
            Regex("""(?:,\s*|\s+)and\s+(?=$actionStart)|(?:,\s*|\s+)\&\s+(?=$actionStart)""")

        fun isMessageLike(value: String): Boolean {
            val lower = value.lowercase()
            val messageStart = lower.startsWith("send ") ||
                lower.startsWith("send a ") ||
                lower.startsWith("send me a ") ||
                lower.startsWith("message ") ||
                lower.startsWith("tura ") ||
                lower.startsWith("tura wa ")
            return messageStart && (
                lower.contains(" saying ") ||
                    lower.contains(" that says ") ||
                    lower.contains(" with the message ") ||
                    lower.contains(" cewa ") ||
                    lower.contains(" yana cewa ")
            )
        }

        val chunks = mutableListOf<String>()
        var remainder = normalized

        while (true) {
            val explicit = explicitSeparator.find(remainder)
            val action = actionSeparator.find(remainder)
            val separator = listOfNotNull(explicit, action)
                .filterNot { isMessageLike(remainder.substring(0, it.range.first).trim()) }
                .minByOrNull { it.range.first }
                ?: break

            val left = remainder.substring(0, separator.range.first).trim().trimEnd(',')
            val right = remainder.substring(separator.range.last + 1).trim()

            if (left.isNotBlank()) chunks += left
            remainder = right
        }

        if (remainder.isNotBlank()) chunks += remainder
        return chunks
    }
}
