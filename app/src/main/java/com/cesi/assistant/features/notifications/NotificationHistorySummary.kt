package com.cesi.assistant.features.notifications

/**
 * Keeps notification history useful without persisting private message contents
 * or contact names. The notification text is retained separately only by the
 * short-lived MessageCopilotEngine context required for reply suggestions.
 */
object NotificationHistorySummary {
    fun forNotification(sender: String, message: String): String {
        // Keep parameters explicit so callers cannot accidentally persist raw
        // notification content; only a generic event summary is returned.
        return "Received a WhatsApp notification"
    }
}
