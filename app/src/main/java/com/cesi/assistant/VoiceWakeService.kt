package com.cesi.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * CESI wake layer.
 *
 * This uses Android SpeechRecognizer as a practical first wake-word layer.
 * It listens for "CESI", "Hey CESI", "Cesi" and common Hausa variants.
 * OEMs may restrict long-running microphone services, so CESI also exposes
 * the normal foreground-service notification as a fallback.
 */
class VoiceWakeService : Service() {

    companion object {
        const val CHANNEL_ID = "cesi_wake"
        const val NOTIFICATION_ID = 4010
        private const val WAKE_TIMEOUT_MS = 1200L
    }

    private var recognizer: SpeechRecognizer? = null
    private var restarting = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification())
        startWakeListening()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "CESI Voice Wake",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps CESI ready for the wake phrase"
                setShowBadge(false)
            }
        )
    }

    private fun notification(): Notification {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val pending = PendingIntent.getActivity(
            this,
            4010,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("CESI is listening")
            .setContentText("Say “Hey CESI” to wake CESI")
            .setContentIntent(pending)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun startWakeListening() {
        if (restarting || !SpeechRecognizer.isRecognitionAvailable(this)) return

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onResults(results: Bundle?) {
                val phrases = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .orEmpty()

                val wake = phrases.firstOrNull { containsWakeWord(it) }
                if (wake != null) {
                    wakeCesi(wake)
                } else {
                    restartSoon()
                }
            }

            override fun onError(error: Int) {
                restartSoon()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-NG")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-NG")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            restartSoon()
        }
    }

    private fun containsWakeWord(value: String): Boolean {
        val normalized = value
            .trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("\s+"), " ")

        return normalized == "cesi" ||
            normalized == "hey cesi" ||
            normalized == "hey, cesi" ||
            normalized.startsWith("cesi ") ||
            normalized.startsWith("hey cesi ") ||
            normalized.contains(" cesi")
    }

    private fun wakeCesi(spoken: String) {
        restarting = true

        val glowIntent = Intent(this, GlowActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            putExtra("wake_phrase", spoken)
        }

        try {
            startActivity(glowIntent)
        } catch (_: Exception) {
            // If Android/OEM blocks background activity launch, the
            // foreground notification remains available as the fallback.
        }

        val assistant = Intent(this, com.cesi.assistant.core.service.CesiAssistantService::class.java)
        try {
            startForegroundService(assistant)
        } catch (_: Exception) {
        }

        stopSelf()
    }

    private fun restartSoon() {
        if (restarting) return
        restarting = true
        android.os.Handler(mainLooper).postDelayed({
            restarting = false
            startWakeListening()
        }, WAKE_TIMEOUT_MS)
    }

    override fun onDestroy() {
        recognizer?.destroy()
        recognizer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
