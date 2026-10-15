package com.cesi.assistant.features.notifications

/**
 * Safe, content-free description for the general-purpose history store.
 * Notification text is handled separately as short-lived in-memory context.
 */
object NotificationHistorySummary {
    @Suppress("UNUSED_PARAMETER")
    fun forNotification(sender: String, message: String): String =
        "Received a WhatsApp notification"
}
