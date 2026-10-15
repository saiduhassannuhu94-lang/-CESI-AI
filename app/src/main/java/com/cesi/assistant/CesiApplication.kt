package com.cesi.assistant

import android.app.Application

/**
 * Startup migration that removes sensitive WhatsApp text written by older
 * versions to SharedPreferences. The current implementation never recreates
 * that store; deleting it repeatedly is intentionally safe and idempotent.
 */
class CesiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        deleteSharedPreferences(LEGACY_MESSAGE_CONTEXT_PREFERENCES)
    }

    private companion object {
        const val LEGACY_MESSAGE_CONTEXT_PREFERENCES = "cesi_message_copilot"
    }
}
