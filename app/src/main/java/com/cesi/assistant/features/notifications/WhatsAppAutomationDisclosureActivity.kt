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
            .setTitle("Allow CESI to help with WhatsApp actions?")
            .setMessage(
                "If you continue, Android will let CESI inspect visible controls and text inside " +
                    "WhatsApp only while a reaction or quoted reply you explicitly requested is pending. " +
                    "For a reaction, CESI tries to locate the exact visible message and select your emoji. " +
                    "For a quoted reply, CESI tries to locate the exact quoted message, choose Reply, enter " +
                    "your text, and tap Send only after you confirm the command. This is processed on this " +
                    "device and this feature does not send message text to CESI servers. WhatsApp UI changes " +
                    "may cause the action to stop; a UI click is not proof of delivery. You can decline now " +
                    "or disable CESI in Android Accessibility settings."
            )
            .setPositiveButton("I understand and continue") { _, _ ->
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().putBoolean(KEY_CONSENT, true).apply()
                openAccessibilitySettingsForResult()
            }
            .setNegativeButton("Not now") { _, _ ->
                WhatsAppReactionCoordinator.clear()
                WhatsAppQuotedReplyCoordinator.clear()
                setResult(RESULT_CANCELED)
                finish()
            }
            .setOnCancelListener {
                WhatsAppReactionCoordinator.clear()
                WhatsAppQuotedReplyCoordinator.clear()
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
                WhatsAppQuotedReplyCoordinator.clear()
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
            WhatsAppQuotedReplyCoordinator.clear()
            Toast.makeText(
                this,
                "Ba a kunna CESI Accessibility ba. Reaction ɗin bai gudana ba.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        if (WhatsAppReactionCoordinator.current() == null) {
            WhatsAppQuotedReplyCoordinator.clear()
            Toast.makeText(
                this,
                "An kunna CESI Accessibility. Ka koma CESI ka sake ba da umarnin WhatsApp ɗin da kake son yi.",
                Toast.LENGTH_LONG
            ).show()
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
        const val KEY_CONSENT = "accessibility_disclosure_accepted_v2"
        private const val REQUEST_ACCESSIBILITY_SETTINGS = 7201

        fun hasConsent(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_CONSENT, false)
    }
}
