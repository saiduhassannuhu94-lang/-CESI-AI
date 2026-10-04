package com.cesi.assistant.core.task

/**
 * Semantic representation of a request before it is mapped to Android
 * capabilities. This layer describes what the user wants, not how Android
 * should execute it.
 */
data class RequestUnderstanding(
    val goal: AssistantGoal,
    val confidence: Float,
    val entities: Map<String, String> = emptyMap()
)

enum class AssistantGoal {
    INFORMATION,
    DEVICE_CONTROL,
    APP_CONTROL,
    COMMUNICATION,
    MEDIA,
    SEARCH,
    NAVIGATION,
    SCHEDULING,
    UNKNOWN
}

/**
 * Deterministic local classifier used as a safe baseline.
 *
 * It is intentionally conservative: unknown language stays UNKNOWN instead
 * of being guessed. A broader reasoning provider can enrich this later.
 */
class RequestUnderstandingEngine {

    fun understand(request: AssistantRequest): RequestUnderstanding {
        val text = request.normalizedText.trim().lowercase()
        if (text.isBlank()) {
            return RequestUnderstanding(AssistantGoal.UNKNOWN, 0f)
        }

        return when {
            containsAny(text, "battery", "baturi", "lokaci", "time", "date", "yau") ->
                RequestUnderstanding(AssistantGoal.INFORMATION, 0.9f)

            containsAny(text, "brightness", "haske", "flashlight", "wifi", "wi-fi", "bluetooth", "volume") ->
                RequestUnderstanding(AssistantGoal.DEVICE_CONTROL, 0.9f)

            containsAny(text, "open", "buɗe", "bude", "launch", "whatsapp", "youtube") ->
                RequestUnderstanding(AssistantGoal.APP_CONTROL, 0.85f)

            containsAny(text, "call", "kira", "dial", "message", "saƙo", "sako", "reply", "amsa", "tura") ->
                RequestUnderstanding(AssistantGoal.COMMUNICATION, 0.85f)

            containsAny(text, "play", "kunna", "music", "song", "waka", "video") ->
                RequestUnderstanding(AssistantGoal.MEDIA, 0.8f)

            containsAny(text, "search", "nemo", "find", "bincika") ->
                RequestUnderstanding(AssistantGoal.SEARCH, 0.8f)

            containsAny(text, "remind", "reminder", "alarm", "tunatar") ->
                RequestUnderstanding(AssistantGoal.SCHEDULING, 0.85f)

            containsAny(text, "location", "where am i", "ina nake") ->
                RequestUnderstanding(AssistantGoal.NAVIGATION, 0.8f)

            else -> RequestUnderstanding(AssistantGoal.UNKNOWN, 0f)
        }
    }

    private fun containsAny(text: String, vararg terms: String): Boolean =
        terms.any { text.contains(it) }
}
