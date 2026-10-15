package com.cesi.assistant.features.notifications

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import com.cesi.assistant.core.memory.HistoryStore
import com.cesi.assistant.features.messaging.MessageCopilotEngine
import java.util.Locale

class CesiNotificationListenerService : NotificationListenerService() {
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val engine = tts
                val requestedLocale = Locale("en", "NG")
                val availability = engine?.isLanguageAvailable(requestedLocale)
                    ?: TextToSpeech.LANG_NOT_SUPPORTED
                engine?.language = if (availability >= TextToSpeech.LANG_AVAILABLE) requestedLocale else Locale.US
                ttsReady = engine != null
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != WHATSAPP_PACKAGE) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty().trim()

        if (text.isBlank()) {
            clearLatestContext()
            return
        }

        latestReplyAction = sbn.notification.actions
            ?.firstOrNull { action -> action.remoteInputs?.isNotEmpty() == true }
        latestNotificationKey = sbn.key
        latestConversationIntent = sbn.notification.contentIntent

        MessageCopilotEngine().rememberIncoming(title.ifBlank { "WhatsApp contact" }, text)
        HistoryStore(this).add(
            "WhatsApp notification",
            NotificationHistorySummary.forNotification(title, text)
        )

        if (!WhatsAppNotificationPrivacy.isSpeechEnabled(this) || !ttsReady) return
        val message = if (title.isBlank()) text else "WhatsApp message from $title: $text"
        val suggestions = MessageCopilotEngine().suggestions()
        val speech = if (suggestions.isNotEmpty() && suggestions.size <= 3) {
            message + ". You can reply: " + suggestions.joinToString(". Or: ")
        } else {
            message
        }
        tts?.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "cesi_whatsapp_" + sbn.postTime)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == WHATSAPP_PACKAGE && sbn.key == latestNotificationKey) {
            clearLatestReplyAction()
            latestConversationIntent = null
        }
    }

    override fun onListenerDisconnected() {
        clearLatestContext()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        clearLatestContext()
        if (instance === this) instance = null
        ttsReady = false
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    private fun clearLatestReplyAction() {
        latestReplyAction = null
        latestNotificationKey = null
    }

    private fun clearLatestContext() {
        clearLatestReplyAction()
        latestConversationIntent = null
        MessageCopilotEngine().clear()
        WhatsAppReactionCoordinator.clear()
    }

    companion object {
        private const val WHATSAPP_PACKAGE = "com.whatsapp"

        @Volatile private var instance: CesiNotificationListenerService? = null
        @Volatile private var latestReplyAction: Notification.Action? = null
        @Volatile private var latestNotificationKey: String? = null
        @Volatile private var latestConversationIntent: PendingIntent? = null

        fun replyLatestWhatsApp(text: String): Boolean {
            val service = instance ?: return false
            val action = latestReplyAction ?: return false
            val remoteInputs = action.remoteInputs ?: return false
            if (remoteInputs.isEmpty() || text.isBlank()) return false
            return try {
                val fillInIntent = Intent()
                val results = android.os.Bundle().apply {
                    for (remoteInput in remoteInputs) putCharSequence(remoteInput.resultKey, text)
                }
                RemoteInput.addResultsToIntent(remoteInputs, fillInIntent, results)
                action.actionIntent.send(service, 0, fillInIntent)
                true
            } catch (_: Exception) {
                false
            }
        }

        fun openLatestWhatsAppConversationForReaction(): Boolean {
            val service = instance ?: return false
            val contentIntent = latestConversationIntent ?: return false
            return try {
                contentIntent.send()
                true
            } catch (_: Exception) {
                false
            }
        }

        fun stopSpeaking() {
            instance?.tts?.stop()
        }
    }
}
