package com.cesi.assistant.features.notifications

import android.content.Context

/**
 * User-controlled handling of notification speech.
 *
 * Android notification access is an OS-level grant; speaking message bodies
 * is a separate preference and is off by default.
 */
object WhatsAppNotificationPrivacy {
    const val PREFERENCES_NAME = "cesi_privacy"
    const val SPEAK_MESSAGES_KEY = "speak_whatsapp_notifications"
    const val DEFAULT_SPEAK_MESSAGES = false

    fun isSpeechEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getBoolean(SPEAK_MESSAGES_KEY, DEFAULT_SPEAK_MESSAGES)

    fun setSpeechEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SPEAK_MESSAGES_KEY, enabled)
            .apply()
    }
}
