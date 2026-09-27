package com.cesi.assistant.features.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.speech.tts.TextToSpeech
import com.cesi.assistant.features.contacts.ContactController
import java.util.Locale

class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER).orEmpty()
        val name = ContactController(context).findContactName(number) ?: number.ifBlank { "unknown number" }

        val appContext = context.applicationContext
        val tts = TextToSpeech(appContext) { result ->
            if (result == TextToSpeech.SUCCESS) {
                ttsHolder?.language = Locale("en", "NG")
                ttsHolder?.speak(
                    "Incoming call from " + name,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "cesi_incoming_call"
                )
            }
        }
        ttsHolder = tts
    }

    companion object {
        private var ttsHolder: TextToSpeech? = null
    }
}
