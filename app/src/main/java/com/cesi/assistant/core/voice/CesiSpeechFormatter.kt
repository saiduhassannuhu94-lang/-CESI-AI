package com.cesi.assistant.core.voice

/** Safe speech pacing limits and conservative text cleanup for Android TTS. */
object CesiSpeechRate {
    const val MIN = 0.75f
    const val MAX = 1.05f
    const val DEFAULT = 0.88f

    fun clamp(value: Float): Float = value.coerceIn(MIN, MAX)
}

object CesiSpeechFormatter {
    fun format(input: String): String {
        if (input.isBlank()) return ""

        val withoutCode = input
            .replace(Regex("(?s)\u0060\u0060\u0060.*?\u0060\u0060\u0060"), " code omitted ")
            .replace(Regex("\u0060([^\u0060]+)\u0060"), "$1")
            .replace(Regex("\\[([^\\]]+)\\]\\((?:https?://)?[^)]+\\)"), "$1")
            .replace(Regex("https?://\\S+"), " link ")
            .replace(Regex("(?m)^\\s*#{1,6}\\s*"), "")
            .replace(Regex("(?m)^\\s*[-*•]\\s*"), "")
            .replace(Regex("(?m)^\\s*\\d+[.)]\\s*"), "")

        return withoutCode
            .lines()
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotEmpty() }
            .joinToString(". ")
            .replace(Regex("\\s*([.!?])\\s*"), "$1 ")
            .replace(Regex("(?:[.!?]\\s*){2,}"), ". ")
            .trim()
            .trimEnd('.', ' ')
    }
}
