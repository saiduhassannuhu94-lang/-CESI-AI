package com.cesi.assistant.core.service

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
import android.widget.*
import com.cesi.assistant.R
import com.cesi.assistant.core.task.ContextTaskEngine
import com.cesi.assistant.core.task.TaskEngine
import java.util.Locale

class CesiAssistantService : Service() {

    companion object {
        const val CHANNEL_ID = "cesi_background"
        const val NOTIFICATION_ID = 4001
    }

    private lateinit var taskEngine: TaskEngine
    private lateinit var contextTaskEngine: ContextTaskEngine
    private lateinit var tts: TextToSpeech

    private var speechRecognizer: SpeechRecognizer? = null
    private var overlayView: View? = null
    private var statusText: TextView? = null
    private var transcriptText: TextView? = null
    private var isListening = false

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        taskEngine = TaskEngine(this)
        contextTaskEngine = ContextTaskEngine(this)

        tts = TextToSpeech(this) {
            tts.language = Locale.US
        }

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

    private fun createNotification(): Notification {
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
            .setContentText("Tap the CESI Orb to speak")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
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

        val orb = TextView(this).apply {
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
            textSize = 12f
            setTextColor(Color.rgb(190, 204, 222))
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
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

        orbRow.addView(orb, LinearLayout.LayoutParams(58, 58))
        orbRow.addView(
            textColumn,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        container.addView(orbRow)

        val params = WindowManager.LayoutParams(
            340,
            92,
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

        orb.setOnTouchListener { _, event ->
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

    private fun startListening() {
        if (isListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Speech recognition is not available.")
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    setStatus("Listening")
                    setTranscript("I'm listening…")
                }

                override fun onBeginningOfSpeech() {
                    isListening = true
                    setStatus("Listening")
                }

                override fun onEndOfSpeech() {
                    isListening = false
                    setStatus("Processing")
                }

                override fun onError(error: Int) {
                    isListening = false
                    setStatus("Ready")
                    setTranscript("Tap to speak")
                }

                override fun onResults(results: Bundle?) {
                    isListening = false

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

                override fun onRmsChanged(rmsdB: Float) {}
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
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.startListening(intent)
    }

    private fun handleCommand(command: String) {
        val response = contextTaskEngine.execute(command)
        setStatus("Ready")
        setTranscript(response)
        speak(response)
    }

    private fun speak(text: String) {
        setStatus("Speaking")
        setTranscript(text)

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
        return START_STICKY
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null

        if (::tts.isInitialized) {
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
