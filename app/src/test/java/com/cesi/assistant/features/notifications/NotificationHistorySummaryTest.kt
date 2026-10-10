package com.cesi.assistant.features.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NotificationHistorySummaryTest {
    @Test
    fun historySummaryDoesNotPersistSenderOrMessageText() {
        val sender = "Private Contact 123"
        val message = "Confidential one-time password 827194"

        val summary = NotificationHistorySummary.forNotification(sender, message)

        assertEquals("Received a WhatsApp notification", summary)
        assertFalse(summary.contains(sender))
        assertFalse(summary.contains(message))
        assertFalse(summary.contains("827194"))
    }
}
