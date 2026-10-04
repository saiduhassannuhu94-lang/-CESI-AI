package com.cesi.assistant.core.task

/**
 * Structured representation of a natural-language request before Android
 * execution. Keeping this separate from Android APIs lets the same request
 * contract power the APK and the browser test lab.
 */
data class AssistantRequest(
    val rawText: String,
    val normalizedText: String,
    val source: RequestSource = RequestSource.VOICE,
    val localeHint: String? = null
)

enum class RequestSource {
    VOICE,
    TEXT,
    NOTIFICATION,
    WEB_TEST
}
