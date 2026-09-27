package com.cesi.assistant.core.memory

import android.content.Context

/**
 * Small, local context layer for follow-up commands.
 *
 * This is deliberately separate from long-term history: it keeps only the
 * latest useful references needed for natural follow-up commands.
 */
class ConversationContextStore(context: Context) {
    private val prefs = context.getSharedPreferences("cesi_context", Context.MODE_PRIVATE)

    var lastCommand: String?
        get() = prefs.getString("last_command", null)
        private set(value) {
            prefs.edit().putString("last_command", value).apply()
        }

    var lastApp: String?
        get() = prefs.getString("last_app", null)
        private set(value) {
            prefs.edit().putString("last_app", value).apply()
        }

    var lastSearch: String?
        get() = prefs.getString("last_search", null)
        private set(value) {
            prefs.edit().putString("last_search", value).apply()
        }

    var lastContact: String?
        get() = prefs.getString("last_contact", null)
        private set(value) {
            prefs.edit().putString("last_contact", value).apply()
        }

    fun rememberCommand(command: String) {
        if (command.isNotBlank()) lastCommand = command.trim()
    }

    fun rememberApp(app: String) {
        if (app.isNotBlank()) lastApp = app.trim()
    }

    fun rememberSearch(query: String) {
        if (query.isNotBlank()) lastSearch = query.trim()
    }

    fun rememberContact(contact: String) {
        if (contact.isNotBlank()) lastContact = contact.trim()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
