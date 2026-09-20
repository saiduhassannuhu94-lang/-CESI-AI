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
    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())

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
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
            setBackgroundColor(Color.TRANSPARENT)
        }

        orbView = TextView(this).apply {
            text = "✦"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(30, 18, 65))
                setStroke(4, Color.rgb(0, 229, 255))
            }
            elevation = 24f
        }

        statusText = TextView(this).apply {
            text = "Ready"
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(0, 229, 255))
            visibility = View.GONE
        }

        container.addView(orbView, LinearLayout.LayoutParams(72, 72))
        container.addView(statusText, LinearLayout.LayoutParams(100, 32))

        val params = WindowManager.LayoutParams(
            112, 112,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 18
            y = 180
        }

        var sx = 0
        var sy = 0
        var tx = 0f
        var ty = 0f
        orbView?.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx=params.x; sy=params.y; tx=e.rawX; ty=e.rawY; true }
                MotionEvent.ACTION_MOVE -> { params.x=sx+(tx-e.rawX).toInt(); params.y=sy+(e.rawY-ty).toInt(); wm.updateViewLayout(container,params); true }
                MotionEvent.ACTION_UP -> {
                    if (kotlin.math.abs(e.rawX-tx) < 20 && kotlin.math.abs(e.rawY-ty) < 20) startListening()
                    true
                }
                else -> false
            }
        }
        wm.addView(container, params)
        overlayView = container
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
            override fun onReadyForSpeech(p: Bundle?) { isListening=true; setState("LISTENING") }
            override fun onBeginningOfSpeech() { isListening=true; setState("LISTENING") }
            override fun onEndOfSpeech() { isListening=false; setState("THINKING") }
            override fun onError(e: Int) { isListening=false; setState("READY") }
            override fun onResults(r: Bundle?) {
                isListening=false
                val text=r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (text.isBlank()) setState("READY") else handleCommand(text)
            }
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int,p: Bundle?) {}
        })

        val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-NG")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false)
        }
        try { speechRecognizer?.startListening(i) } catch (_:Exception) { isListening=false; setState("READY") }
    }

    private fun handleCommand(command:String) {
        setState("THINKING")
        handler.postDelayed({
            val response=actionRouter.route(intentEngine.understand(command))
            speak(response)
        },120)
    }

    private fun speak(text:String) {
        setState("SPEAKING")
        tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"cesi_response")
        handler.postDelayed({ setState("READY") },2000)
    }

    private fun setState(state:String) {
        handler.post {
            val display = when(state) {
                "LISTENING" -> "Sauraro…"
                "THINKING" -> "Tunani…"
                "SPEAKING" -> "Amsa…"
                else -> "Ready"
            }
            statusText?.text=display
            statusText?.visibility=if(state=="READY") View.GONE else View.VISIBLE
            orbView?.text=when(state) {
                "LISTENING" -> "◉"
                "THINKING" -> "✦"
                "SPEAKING" -> "◌"
                else -> "✦"
            }
            orbView?.animate()?.cancel()
            if(state!="READY") orbView?.animate()?.scaleX(1.12f)?.scaleY(1.12f)?.setDuration(350)?.withEndAction {
                orbView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(350)?.start()
            }?.start()
        }
    }

    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        showOverlay()
        if(intent?.hasExtra(EXTRA_WAKE_PHRASE)==true) handler.postDelayed({startListening()},350)
        return START_STICKY
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        if(::tts.isInitialized) tts.shutdown()
        overlayView?.let { try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } catch(_:Exception){} }
        overlayView=null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent:Intent?):IBinder?=null
}