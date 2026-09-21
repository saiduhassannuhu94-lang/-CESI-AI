package com.cesi.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class VoiceWakeService : Service() {
    companion object {
        const val CHANNEL_ID = "cesi_wake"
        const val NOTIFICATION_ID = 4010
        const val EXTRA_WAKE_PHRASE = "wake_phrase"
        private const val RESTART_DELAY_MS = 900L
    }

    private var recognizer: SpeechRecognizer? = null
    private var restarting = false
    private val handler by lazy { Handler(mainLooper) }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "CESI Voice Wake",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("CESI is ready")
                .setContentText("Say “Hey CESI”")
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
        } else {
            Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("CESI is ready")
                .setContentText("Say “Hey CESI”")
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
        }

        startForeground(NOTIFICATION_ID, notification)
        startWakeListening()
    }

    private fun startWakeListening() {
        if (restarting || !SpeechRecognizer.isRecognitionAvailable(this)) return

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}

            override fun onError(e: Int) = restartSoon()

            override fun onResults(results: Bundle?) {
                val phrases = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .orEmpty()

                val wake = phrases.firstOrNull { containsWakeWord(it) }
                if (wake != null) wakeCesi(wake) else restartSoon()
            }
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
        val s = value.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
        return s == "cesi" ||
            s == "hey cesi" ||
            s == "hey, cesi" ||
            s.startsWith("cesi ") ||
            s.startsWith("hey cesi ") ||
            s.contains(" cesi")
    }

    private fun wakeCesi(spoken: String) {
        restarting = true

        try {
            startActivity(Intent(this, GlowActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                putExtra(EXTRA_WAKE_PHRASE, spoken)
            })
        } catch (_: Exception) {}

        try {
            startForegroundService(
                Intent(this, com.cesi.assistant.core.service.CesiAssistantService::class.java).apply {
                    putExtra(EXTRA_WAKE_PHRASE, spoken)
                }
            )
        } catch (_: Exception) {}

        stopSelf()
    }

    private fun restartSoon() {
        if (restarting) return
        restarting = true
        handler.postDelayed(
            {
                restarting = false
                startWakeListening()
            },
            RESTART_DELAY_MS
        )
    }

    override fun onDestroy() {
        recognizer?.destroy()
        recognizer = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
