package com.cesi.assistant.features.phone

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import com.cesi.assistant.features.contacts.ContactController
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER).orEmpty()
        if (number.isBlank()) return // Android may withhold caller ID; never announce a guessed identity.

        val name = try {
            ContactController(context).findContactName(number) ?: number
        } catch (_: SecurityException) {
            number
        }

        // TextToSpeech initializes asynchronously. Keep the receiver alive briefly
        // and always shut down the engine, including initialization/error timeouts.
        val pending = goAsync()
        val handler = Handler(Looper.getMainLooper())
        val finished = AtomicBoolean(false)
        var engine: TextToSpeech? = null
        var timeout: Runnable? = null

        val finish: () -> Unit = {
            if (finished.compareAndSet(false, true)) {
                timeout?.let(handler::removeCallbacks)
                try {
                    engine?.stop()
                    engine?.shutdown()
                } catch (_: Exception) {
                    // Cleanup must not prevent BroadcastReceiver completion.
                }
                pending.finish()
            }
        }

        timeout = Runnable { finish() }
        handler.postDelayed(timeout!!, MAX_TTS_LIFETIME_MS)

        engine = TextToSpeech(context.applicationContext) { result ->
            val activeEngine = engine
            if (result != TextToSpeech.SUCCESS || activeEngine == null || finished.get()) {
                finish()
                return@TextToSpeech
            }

            activeEngine.language = Locale.US
            activeEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = finish()
                override fun onError(utteranceId: String?) = finish()
            })

            val status = activeEngine.speak(
                "Incoming call from $name",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "cesi_incoming_call"
            )
            if (status == TextToSpeech.ERROR) finish()
        }
    }

    private companion object {
        const val MAX_TTS_LIFETIME_MS = 10_000L
    }
}
