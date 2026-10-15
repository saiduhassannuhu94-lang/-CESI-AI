package com.cesi.assistant.features.notifications

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

/**
 * Prominent disclosure shown before opening Android Accessibility settings.
 * CESI is not declared as an accessibility tool; this is an optional,
 * deterministic, user-triggered WhatsApp reaction helper.
 */
class WhatsAppAutomationDisclosureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (preferences.getBoolean(KEY_CONSENT, false)) {
            openAccessibilitySettings()
            finish()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Allow CESI to help with WhatsApp reactions?")
            .setMessage(
                "If you continue, Android will let CESI inspect visible controls and text " +
                    "inside WhatsApp only while a reaction you requested is pending. CESI uses " +
                    "the current notification message to find the matching message bubble, then " +
                    "tries to select your chosen emoji. This information is processed on this " +
                    "device for that action and is not sent to CESI servers by this feature. " +
                    "The helper may fail when WhatsApp changes its interface, and a button click " +
                    "does not prove the reaction was sent. You can decline now or turn off CESI " +
                    "in Android Accessibility settings at any time."
            )
            .setPositiveButton("I understand and continue") { _, _ ->
                preferences.edit().putBoolean(KEY_CONSENT, true).apply()
                openAccessibilitySettings()
                finish()
            }
            .setNegativeButton("Not now") { _, _ ->
                setResult(RESULT_CANCELED)
                finish()
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    companion object {
        const val PREFS_NAME = "cesi_whatsapp_automation"
        const val KEY_CONSENT = "accessibility_disclosure_accepted"
    }
}
