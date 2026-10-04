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

    override fun onCreate() {
        super.onCreate()
        instance = this
        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) tts?.language = Locale("en", "NG")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.whatsapp") return
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (text.isBlank()) return

        latestReplyAction = sbn.notification.actions
            ?.firstOrNull { action ->
                action.remoteInputs?.isNotEmpty() == true
            }

        latestConversationTitle = title
        MessageCopilotEngine(this).rememberIncoming(title.ifBlank { "WhatsApp contact" }, text)

        val message = if (title.isBlank()) text else "WhatsApp message from " + title + ": " + text
        HistoryStore(this).add("WhatsApp notification", message)

        val copilot = MessageCopilotEngine(this)
        val suggestions = copilot.suggestions()
        val speech = if (suggestions.isNotEmpty() && suggestions.size <= 3) {
            message + ". You can reply: " + suggestions.joinToString(". Or: ")
        } else {
            message
        }
        tts?.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "cesi_whatsapp_" + System.currentTimeMillis())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == "com.whatsapp") {
            if (latestConversationTitle == sbn.notification.extras
                    ?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            ) {
                latestReplyAction = null
                latestConversationTitle = ""
            }
        }
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        latestReplyAction = null
        latestConversationTitle = ""
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var instance: CesiNotificationListenerService? = null

        @Volatile
        private var latestReplyAction: Notification.Action? = null

        @Volatile
        private var latestConversationTitle: String = ""

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
    }
}
