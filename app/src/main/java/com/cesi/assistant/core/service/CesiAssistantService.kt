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
import com.cesi.assistant.core.action.ActionRouter
import com.cesi.assistant.core.task.TaskEngine
import com.cesi.assistant.core.task.ContextTaskEngine
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
        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

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
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
        ) {
            return
        }

        if (overlayView != null) return

        val windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(10, 10, 10, 10)
            setBackgroundColor(Color.TRANSPARENT)
        }

        val orbBackground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.rgb(11, 18, 32))
            setStroke(2, Color.rgb(102, 227, 255))
        }

        val orb = TextView(this).apply {
            text = "CESI"
            textSize = 13f
            setTextColor(Color.rgb(102, 227, 255))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = orbBackground
            elevation = 14f
            setPadding(18, 18, 18, 18)
            contentDescription = "CESI voice assistant"
        }

        statusText = TextView(this).apply {
            text = "Ready"
            textSize = 10f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(12, 5, 12, 5)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(Color.argb(210, 11, 18, 32))
            }
        }

        container.addView(
            orb,
            LinearLayout.LayoutParams(
                72,
                72
            )
        )

        container.addView(
            statusText,
            LinearLayout.LayoutParams(
                112,
                34
            ).apply {
                topMargin = 6
            }
        )

        val params = WindowManager.LayoutParams(
            128,
            126,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.END
        params.x = 20
        params.y = 180

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

                    params.x =
                        startX +
                            (touchX - event.rawX).toInt()

                    params.y =
                        startY +
                            (event.rawY - touchY).toInt()

                    windowManager.updateViewLayout(
                        container,
                        params
                    )

                    true
                }

                MotionEvent.ACTION_UP -> {

                    val moved =
                        kotlin.math.abs(
                            event.rawX - touchX
                        ) > 20 ||
                        kotlin.math.abs(
                            event.rawY - touchY
                        ) > 20

                    if (!moved) {
                        startListening()
                    }

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

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {
                    isListening = true
                    setStatus("Listening")
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
                }

                override fun onResults(
                    results: Bundle?
                ) {

                    isListening = false

                    val text =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )?.firstOrNull().orEmpty()

                    if (text.isBlank()) {
                        setStatus("Ready")
                        return
                    }

                    handleCommand(text)
                }

                override fun onRmsChanged(
                    rmsdB: Float
                ) {}

                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {}

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {}

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {}
            }
        )

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "en-NG"
                )

                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    true
                )
            }

        speechRecognizer?.startListening(intent)
    }

    private fun handleCommand(command: String) {

        val response = contextTaskEngine.execute(command)

        setStatus("Ready")
        speak(response)
    }

    private fun speak(text: String) {

        setStatus("Speaking")

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

            statusText?.visibility =
                if (text == "Ready")
                    View.GONE
                else
                    View.VISIBLE
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

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
                val windowManager =
                    getSystemService(WINDOW_SERVICE)
                        as WindowManager

                windowManager.removeView(it)
            } catch (_: Exception) {}
        }

        overlayView = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
