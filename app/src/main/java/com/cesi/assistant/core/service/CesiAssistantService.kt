package com.cesi.assistant.core.service

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.Settings
import android.speech.*
import android.speech.tts.TextToSpeech
import android.view.*
import android.widget.*
import com.cesi.assistant.R
import com.cesi.assistant.core.action.ActionRouter
import com.cesi.assistant.core.intent.IntentEngine
import java.util.Locale

class CesiAssistantService : Service() {
    companion object {
        const val CHANNEL_ID = "cesi_background"
        const val NOTIFICATION_ID = 4001
        const val EXTRA_WAKE_PHRASE = "wake_phrase"
    }

    private lateinit var intentEngine: IntentEngine
    private lateinit var actionRouter: ActionRouter
    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private var overlayView: View? = null
    private var orbView: TextView? = null
    private var statusText: TextView? = null
    private var activePanel: LinearLayout? = null
    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())

    private val cyan = Color.rgb(0, 229, 255)
    private val purple = Color.rgb(108, 77, 255)
    private val dark = Color.rgb(5, 10, 24)

    override fun onCreate() {
        super.onCreate()
        intentEngine = IntentEngine()
        actionRouter = ActionRouter(this)
        tts = TextToSpeech(this) { engine ->
            if (engine == TextToSpeech.SUCCESS) {
                tts.language = Locale("ha", "NG")
                tts.setSpeechRate(0.95f)
            }
        }
        createChannel()
        startForeground(NOTIFICATION_ID, notification())
        showOverlay()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "CESI Assistant", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(): Notification {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val pending = PendingIntent.getActivity(
            this, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("CESI tana aiki")
            .setContentText("Ka ce “Hey CESI” ko ka taɓa Orb")
            .setContentIntent(pending)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun showOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return
        if (overlayView != null) return

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        orbView = TextView(this).apply {
            text = "✦"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = orbBackground()
            elevation = 30f
            setOnClickListener { startListening() }
        }

        val orbParams = FrameLayout.LayoutParams(88, 88, Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM).apply {
            bottomMargin = 26
        }
        root.addView(orbView, orbParams)

        activePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(30, 28, 30, 28)
            background = panelBackground()
            elevation = 32f
            visibility = View.GONE
        }

        val panelParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ).apply {
            leftMargin = 34
            rightMargin = 34
        }

        val panelOrb = TextView(this).apply {
            text = "✦"
            textSize = 38f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = orbBackground()
            elevation = 28f
        }
        statusText = TextView(this).apply {
            text = "Sauraro…"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 18, 0, 8)
        }

        activePanel?.addView(panelOrb, LinearLayout.LayoutParams(116, 116))
        activePanel?.addView(statusText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 56
        ))
        root.addView(activePanel, panelParams)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        wm.addView(root, params)
        overlayView = root

        root.setOnClickListener { /* keep overlay passive outside CESI */ }
    }

    private fun orbBackground(): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(0, 90, 180), purple, Color.rgb(20, 12, 55))
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(3, cyan)
        }

    private fun panelBackground(): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(8, 24, 48), Color.rgb(13, 10, 35))
        ).apply {
            cornerRadius = 44f
            setStroke(2, Color.argb(120, 0, 229, 255))
        }

    private fun startListening() {
        if (isListening) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Ban samu speech recognition ba a wannan wayar.")
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) { isListening = true; setState("LISTENING") }
            override fun onBeginningOfSpeech() { isListening = true; setState("LISTENING") }
            override fun onEndOfSpeech() { isListening = false; setState("THINKING") }
            override fun onError(e: Int) { isListening = false; setState("READY") }
            override fun onResults(r: Bundle?) {
                isListening = false
                val text = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                if (text.isBlank()) setState("READY") else handleCommand(text)
            }
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })

        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-NG")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        try {
            speechRecognizer?.startListening(i)
        } catch (_: Exception) {
            isListening = false
            setState("READY")
        }
    }

    private fun handleCommand(command: String) {
        setState("THINKING")
        handler.postDelayed({
            val response = actionRouter.route(intentEngine.understand(command))
            speak(response)
        }, 180)
    }

    private fun speak(text: String) {
        setState("SPEAKING")
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cesi_response")
        handler.postDelayed({ setState("READY") }, 2200)
    }

    private fun setState(state: String) {
        handler.post {
            val display = when (state) {
                "LISTENING" -> "Sauraro…"
                "THINKING" -> "Tunani…"
                "SPEAKING" -> "Amsa…"
                else -> "Ready"
            }

            val active = state != "READY"
            activePanel?.visibility = if (active) View.VISIBLE else View.GONE
            orbView?.visibility = if (active) View.GONE else View.VISIBLE

            statusText?.text = display
            activePanel?.getChildAt(0)?.let { panelOrb ->
                if (panelOrb is TextView) {
                    panelOrb.text = when (state) {
                        "LISTENING" -> "◉"
                        "THINKING" -> "✦"
                        "SPEAKING" -> "◌"
                        else -> "✦"
                    }
                    panelOrb.animate().cancel()
                    panelOrb.animate()
                        .scaleX(1.08f)
                        .scaleY(1.08f)
                        .setDuration(350)
                        .withEndAction {
                            panelOrb.animate().scaleX(1f).scaleY(1f).setDuration(350).start()
                        }
                        .start()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showOverlay()
        if (intent?.hasExtra(EXTRA_WAKE_PHRASE) == true) {
            handler.postDelayed({ startListening() }, 350)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        if (::tts.isInitialized) tts.shutdown()
        overlayView?.let {
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
            } catch (_: Exception) {}
        }
        overlayView = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
