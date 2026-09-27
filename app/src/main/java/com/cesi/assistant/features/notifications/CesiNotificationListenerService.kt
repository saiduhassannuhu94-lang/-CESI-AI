package com.cesi.assistant.features.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import com.cesi.assistant.core.memory.HistoryStore
import java.util.Locale

class CesiNotificationListenerService : NotificationListenerService() {
    private var tts: TextToSpeech? = null

    override fun onCreate() {
        super.onCreate()
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

        val message = if (title.isBlank()) text else "WhatsApp message from " + title + ": " + text
        HistoryStore(this).add("WhatsApp notification", message)
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "cesi_whatsapp_" + System.currentTimeMillis())
    }

    override fun onDestroy() {
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
