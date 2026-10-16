package com.cesi.assistant.features.notifications

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

/** Separate prominent disclosure before requesting Accessibility-service enablement. */
class WhatsAppAutomationDisclosureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasConsent(this)) {
            openAccessibilitySettingsForResult()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Allow CESI to help with WhatsApp reactions?")
            .setMessage(
                "If you continue, Android will let CESI inspect visible controls and text inside " +
                    "WhatsApp only while a reaction you requested is pending. CESI uses the current " +
                    "notification message to find the matching message bubble, then tries to select " +
                    "your chosen emoji. This information is processed on this device for that action " +
                    "and is not sent to CESI servers by this feature. The helper may fail when " +
                    "WhatsApp changes its interface, and a button click does not prove the reaction " +
                    "was sent. You can decline now or turn off CESI in Android Accessibility settings."
            )
            .setPositiveButton("I understand and continue") { _, _ ->
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().putBoolean(KEY_CONSENT, true).apply()
                openAccessibilitySettingsForResult()
            }
            .setNegativeButton("Not now") { _, _ ->
                WhatsAppReactionCoordinator.clear()
                setResult(RESULT_CANCELED)
                finish()
            }
            .setOnCancelListener {
                WhatsAppReactionCoordinator.clear()
                finish()
            }
            .show()
    }

    private fun openAccessibilitySettingsForResult() {
        try {
            startActivityForResult(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                REQUEST_ACCESSIBILITY_SETTINGS
            )
        } catch (_: Exception) {
            try {
                startActivityForResult(Intent(Settings.ACTION_SETTINGS), REQUEST_ACCESSIBILITY_SETTINGS)
            } catch (_: Exception) {
                WhatsAppReactionCoordinator.clear()
                Toast.makeText(this, "Ba a iya buɗe Accessibility settings ba.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    @Deprecated("Deprecated by Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_ACCESSIBILITY_SETTINGS) return

        if (!hasConsent(this) || !CesiWhatsAppAccessibilityService.isEnabled(this)) {
            WhatsAppReactionCoordinator.clear()
            Toast.makeText(
                this,
                "Ba a kunna CESI Accessibility ba. Reaction ɗin bai gudana ba.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        if (WhatsAppReactionCoordinator.current() == null) {
            Toast.makeText(this, "Lokacin reaction ya wuce. Ka sake ba da umarni.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (CesiNotificationListenerService.openLatestWhatsAppConversationForReaction()) {
            Toast.makeText(
                this,
                "An kunna izinin. CESI zai yi ƙoƙarin saka reaction ɗin. Duba WhatsApp don tabbatarwa.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            WhatsAppReactionCoordinator.clear()
            Toast.makeText(
                this,
                "Ba a sami sabon WhatsApp notification ba. Ka karɓi saƙo sannan ka sake umarni.",
                Toast.LENGTH_LONG
            ).show()
        }
        finish()
    }

    companion object {
        const val PREFS_NAME = "cesi_whatsapp_automation"
        const val KEY_CONSENT = "accessibility_disclosure_accepted"
        private const val REQUEST_ACCESSIBILITY_SETTINGS = 7201

        fun hasConsent(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_CONSENT, false)
    }
}
