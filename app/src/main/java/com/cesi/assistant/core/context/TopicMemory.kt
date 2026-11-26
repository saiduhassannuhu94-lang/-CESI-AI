package com.cesi.assistant.core.context

/**
 * Validates whether the short-lived topic reference is safe to reuse.
 * Topic context expires after 30 minutes and never accepts future timestamps.
 */
object TopicMemory {
    const val MAX_AGE_MILLIS: Long = 30 * 60 * 1000L

    fun isFresh(topic: String, savedAtMillis: Long, nowMillis: Long): Boolean {
        if (topic.isBlank() || savedAtMillis < 0L || nowMillis < savedAtMillis) return false
        return nowMillis - savedAtMillis <= MAX_AGE_MILLIS
    }
}
