package com.cesi.assistant.features.notifications

import android.app.Notification
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
                engine?.language = if (availability >= TextToSpeech.LANG_AVAILABLE) {
                    requestedLocale
                } else {
                    Locale.US
                }
                ttsReady = engine != null
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.whatsapp") return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty().trim()

        // Never leave an older reply action usable when the latest notification
        // cannot safely provide a reply target.
        if (text.isBlank()) {
            clearLatestContext()
            return
        }

        val replyAction = sbn.notification.actions
            ?.firstOrNull { action -> action.remoteInputs?.isNotEmpty() == true }

        latestReplyAction = replyAction
        latestNotificationKey = sbn.key

        // Reply suggestions need only the current message and are intentionally
        // memory-only with a five-minute logical TTL.
        MessageCopilotEngine().rememberIncoming(
            title.ifBlank { "WhatsApp contact" },
            text
        )

        // Persist an event marker, never the sender or private message body.
        HistoryStore(this).add(
            "WhatsApp notification",
            NotificationHistorySummary.forNotification(title, text)
        )

        // Reading notifications and speaking their contents are separate
        // decisions. Speech remains disabled unless the user explicitly opts in.
        if (!WhatsAppNotificationPrivacy.isSpeechEnabled(this) || !ttsReady) return

        val message = if (title.isBlank()) text else "WhatsApp message from $title: $text"
        val suggestions = MessageCopilotEngine().suggestions()
        val speech = if (suggestions.isNotEmpty() && suggestions.size <= 3) {
            message + ". You can reply: " + suggestions.joinToString(". Or: ")
        } else {
            message
        }
        tts?.speak(
            speech,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "cesi_whatsapp_" + sbn.postTime
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == "com.whatsapp" && sbn.key == latestNotificationKey) {
            clearLatestContext()
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

    private fun clearLatestContext() {
        latestReplyAction = null
        latestNotificationKey = null
        MessageCopilotEngine().clear()
    }

    companion object {
        @Volatile
        private var instance: CesiNotificationListenerService? = null

        @Volatile
        private var latestReplyAction: Notification.Action? = null

        @Volatile
        private var latestConversationTitle: String = ""

        @Volatile
        private var latestNotificationKey: String? = null

        fun replyLatestWhatsApp(text: String): Boolean {
            val service = instance ?: return false
            val action = latestReplyAction ?: return false
            val remoteInputs = action.remoteInputs ?: return false
            if (remoteInputs.isEmpty() || text.isBlank()) return false

            return try {
                val fillInIntent = Intent()
                val results = android.os.Bundle().apply {
                    for (remoteInput in remoteInputs) {
                        putCharSequence(remoteInput.resultKey, text)
                    }
                }
                RemoteInput.addResultsToIntent(remoteInputs, fillInIntent, results)
                action.actionIntent.send(service, 0, fillInIntent)
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
