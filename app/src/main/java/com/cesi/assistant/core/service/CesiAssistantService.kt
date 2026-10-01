package com.cesi.assistant.core.service

import android.animation.ValueAnimator
import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.Settings
import android.speech.*
import android.speech.tts.TextToSpeech
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.*
import com.cesi.assistant.R
import com.cesi.assistant.core.task.ContextTaskEngine
import com.cesi.assistant.core.memory.HistoryStore
import com.cesi.assistant.core.task.TaskEngine
import com.cesi.assistant.core.voice.VoiceManager
import java.util.Locale

class CesiAssistantService : Service() {

    companion object {
        const val CHANNEL_ID = "cesi_background"
        const val NOTIFICATION_ID = 4001
        const val EXTRA_WAKE_PHRASE = "wake_phrase"
        const val EXTRA_COMMAND = "command"
    }

    private lateinit var taskEngine: TaskEngine
    private lateinit var historyStore: HistoryStore
    private lateinit var contextTaskEngine: ContextTaskEngine
    private lateinit var tts: TextToSpeech

    private var speechRecognizer: SpeechRecognizer? = null
    private var overlayView: View? = null
    private var statusText: TextView? = null
    private var transcriptText: TextView? = null
    private var orbView: TextView? = null
    private var pulseAnimator: ValueAnimator? = null
    private var isListening = false
    private var ttsReady = false
    private var pendingSpeech: String? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        taskEngine = TaskEngine(this)
        contextTaskEngine = ContextTaskEngine(this)
        historyStore = HistoryStore(this)

        tts = TextToSpeech(this, TextToSpeech.OnInitListener { result ->
            ttsReady = result == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts.language = Locale.US
                val voiceManager = VoiceManager(this)
                if (!voiceManager.applySavedVoice(tts)) {
                    voiceManager.applyBestEnglishVoice(tts)
                }
                tts.setSpeechRate(0.94f)
                tts.setPitch(1.0f)
                pendingSpeech?.let {
                    pendingSpeech = null
                    speakNow(it)
                }
            }
        }, preferredTtsEngine())
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        showOverlay()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CESI Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "CESI background assistant"
                setShowBadge(false)
            }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun createNotification(response: String = "Tap the CESI Orb to speak"): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("CESI is ready")
            .setContentText(response)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun preferredTtsEngine(): String? {
        return try {
            packageManager.queryIntentServices(
                Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE),
                0
            ).firstOrNull { it.serviceInfo.packageName == "com.google.android.tts" }?.serviceInfo?.packageName
        } catch (_: Exception) {
            null
        }
    }

    private fun showOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        if (overlayView != null) return

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(16, 12, 16, 12)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 34f
                setColor(Color.argb(235, 11, 18, 32))
                setStroke(1, Color.rgb(55, 76, 102))
            }
            elevation = 18f
        }

        val orbRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val orbBackground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.rgb(11, 18, 32))
            setStroke(2, Color.rgb(102, 227, 255))
        }

        orbView = TextView(this).apply {
            text = "CESI"
            textSize = 12f
            setTextColor(Color.rgb(102, 227, 255))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = orbBackground
            contentDescription = "CESI voice assistant"
            setPadding(12, 12, 12, 12)
        }

        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 0, 4, 0)
        }

        statusText = TextView(this).apply {
            text = "Ready"
            textSize = 13f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        transcriptText = TextView(this).apply {
            text = "Tap to speak"
            textSize = 18f
            setTextColor(Color.rgb(232, 240, 250))
            maxLines = 5
            ellipsize = null
        }

        textColumn.addView(statusText)
        textColumn.addView(
            transcriptText,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { topMargin = 2 }
        )

        orbRow.addView(orbView, LinearLayout.LayoutParams(64, 64))
        orbRow.addView(
            textColumn,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        container.addView(orbRow)

        val params = WindowManager.LayoutParams(
            360,
            178,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.y = 34

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        orbView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (touchX - event.rawX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(container, params)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val moved = kotlin.math.abs(event.rawX - touchX) > 20 ||
                        kotlin.math.abs(event.rawY - touchY) > 20

                    if (!moved) startListening()
                    true
                }

                else -> false
            }
        }

        windowManager.addView(container, params)
        overlayView = container
    }

    private fun startPulse() {
        val orb = orbView ?: return
        if (pulseAnimator?.isRunning == true) return

        pulseAnimator = ValueAnimator.ofFloat(1f, 1.16f, 1f).apply {
            duration = 900L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { value ->
                val scale = value.animatedValue as Float
                orb.scaleX = scale
                orb.scaleY = scale
            }
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        orbView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(180L)?.start()
    }

    private fun startListening() {
        if (isListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Speech recognition is not available.")
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = try {
            SpeechRecognizer.createSpeechRecognizer(this)
        } catch (_: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
            ) SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            else throw IllegalStateException("No speech recognizer available")
        }

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    setStatus("Listening")
                    setTranscript("Ina sauraronka…")
                    startPulse()
                }

                override fun onBeginningOfSpeech() {
                    isListening = true
                    setStatus("Listening")
                    startPulse()
                }

                override fun onEndOfSpeech() {
                    isListening = false
                    setStatus("Processing")
                    stopPulse()
                }

                override fun onError(error: Int) {
                    isListening = false
                    stopPulse()
                    setStatus("Ready")
                    setTranscript("Tap to speak")
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    stopPulse()

                    val text = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )?.firstOrNull().orEmpty()

                    if (text.isBlank()) {
                        setStatus("Ready")
                        setTranscript("Tap to speak")
                        return
                    }

                    setTranscript(text)
                    handleCommand(text)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )?.firstOrNull().orEmpty()

                    if (partial.isNotBlank()) {
                        setTranscript(partial)
                    }
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val orb = orbView ?: return
                    if (isListening) {
                        val scale = (1f + (rmsdB.coerceIn(0f, 12f) / 100f))
                        orb.scaleX = scale
                        orb.scaleY = scale
                    }
                }

                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
        )

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-NG")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-NG")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            isListening = false
            stopPulse()
            setStatus("Ready")
        }
    }

    private fun handleCommand(command: String) {
        setStatus("Thinking")
        val response = try {
            contextTaskEngine.execute(command)
        } catch (_: Exception) {
            "An samu matsala wajen aiwatar da wannan umarnin."
        }
        historyStore.add(command, response)
        setStatus("Ready")
        setTranscript(response)
        updateResponseNotification(response)
        speak(response)
    }

    private fun speak(text: String) {
        setStatus("Speaking")
        setTranscript(text)

        if (!ttsReady) {
            pendingSpeech = text
            setStatus("Ready")
            return
        }

        speakNow(text)
    }

    private fun speakNow(text: String) {
        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "cesi_response"
        )

        mainHandler.postDelayed(
            {
                setStatus("Ready")
            },
            1800
        )
    }

    private fun updateResponseNotification(response: String) {
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, createNotification(response.take(180)))
        } catch (_: Exception) {}
    }

    private fun setStatus(text: String) {
        mainHandler.post {
            statusText?.text = text
        }
    }

    private fun setTranscript(text: String) {
        mainHandler.post {
            transcriptText?.text = text
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showOverlay()

        val command = intent?.getStringExtra(EXTRA_COMMAND)?.trim().orEmpty()
        if (command.isNotBlank()) {
            mainHandler.postDelayed(
                {
                    setStatus("Thinking")
                    setTranscript(command)
                    handleCommand(command)
                },
                250L
            )
        } else if (intent?.hasExtra(EXTRA_WAKE_PHRASE) == true) {
            mainHandler.postDelayed({ startListening() }, 350L)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        stopPulse()

        if (::tts.isInitialized) {
            ttsReady = false
            pendingSpeech = null
            tts.shutdown()
        }

        overlayView?.let {
            try {
                val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }

        overlayView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
