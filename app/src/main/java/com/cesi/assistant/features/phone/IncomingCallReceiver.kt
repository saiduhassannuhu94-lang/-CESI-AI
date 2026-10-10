package com.cesi.assistant.features.phone

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import com.cesi.assistant.features.contacts.ContactController
import java.util.Locale

class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER).orEmpty()
        if (number.isBlank()) return // Android may withhold caller ID; never announce a guessed identity.
        val name = ContactController(context).findContactName(number) ?: number.ifBlank { "unknown number" }

        lateinit var tts: TextToSpeech
        tts = TextToSpeech(context.applicationContext) { result ->
            if (result == TextToSpeech.SUCCESS) {
                tts.language = Locale("en", "NG")
                tts.speak(
                    "Incoming call from " + name,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "cesi_incoming_call"
                )
            }
        }
    }
}
