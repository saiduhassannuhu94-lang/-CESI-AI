package com.cesi.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cesi.assistant.core.action.ActionRouter
import com.cesi.assistant.core.intent.IntentEngine
import com.cesi.assistant.core.service.CesiAssistantService
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_PERMISSIONS = 7001
    }

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var intentEngine: IntentEngine
    private lateinit var actionRouter: ActionRouter

    private var listening by mutableStateOf(false)
    private var processing by mutableStateOf(false)
    private var status by mutableStateOf("A shirye nake.")
    private var lastHeard by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intentEngine = IntentEngine()
        actionRouter = ActionRouter(this)

        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val preferred = Locale("en", "NG")
                val available = tts.availableLanguages
                if (available != null && available.contains(preferred)) {
                    tts.language = preferred
                } else {
                    tts.language = Locale.US
                }
                tts.setSpeechRate(0.95f)
            }
        }

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(createRecognitionListener())
        }

        setContent {
            CesiScreen()
        }

        requestRequiredPermissions()
    }

    @Composable
    private fun CesiScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF020B19))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))

            Text(
                "CESI",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                "Hausa Voice Assistant",
                color = Color(0xFF9FB6D0)
            )

            Spacer(Modifier.height(36.dp))

            Text(
                if (listening) "🎤 INA SAURARO..." else if (processing) "⚙️ INA AIKI..." else "Barka da zuwa,",
                color = Color.Cyan,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(12.dp))

            Text(
                status,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = { startListening() },
                enabled = !listening && !processing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text(
                    if (listening) "🎤 INA SAURARO..." else "🎤 KUNNA SAURARO",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = { startCesiService() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CESI POP-UP / BACKGROUND")
            }

            Spacer(Modifier.height(24.dp))

            if (lastHeard.isNotBlank()) {
                Text("Na ji:", color = Color(0xFF7DD3FC))
                Spacer(Modifier.height(6.dp))
                Text(lastHeard, color = Color.White)
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missing.toTypedArray(),
                REQUEST_PERMISSIONS
            )
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status = "Speech recognition ba ya samuwa a wannan waya."
            speak("Speech recognition ba ya samuwa a wannan waya.")
            return
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestRequiredPermissions()
            status = "Ina bukatar izinin microphone."
            return
        }

        listening = true
        processing = false
        status = "Ina sauraron ka..."
        lastHeard = ""

        try {
            speechRecognizer.cancel()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-NG")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-NG")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            listening = false
            status = "An samu matsala wajen kunna microphone."
            speak("Ban iya kunna microphone ba.")
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
                status = "Ina sauraro..."
            }

            override fun onBeginningOfSpeech() {
                listening = true
                status = "Ina jin muryarka..."
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                listening = false
                processing = true
                status = "Ina fahimtar umarnin..."
            }

            override fun onError(error: Int) {
                listening = false
                processing = false
                status = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Ba a ba CESI microphone permission ba."
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Matsalar network ta hana gane magana."
                    SpeechRecognizer.ERROR_NO_MATCH -> "Ban ji kalmomin sosai ba. Sake gwadawa."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer yana aiki. Sake gwadawa."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Ban ji magana ba."
                    else -> "An samu matsala wajen sauraro. Sake gwadawa."
                }
            }

            override fun onResults(results: Bundle?) {
                listening = false
                processing = true

                val matches = results?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )

                val command = matches?.firstOrNull()?.trim().orEmpty()

                if (command.isBlank()) {
                    processing = false
                    status = "Ban ji umarnin ba."
                    speak("Ban ji umarnin ba.")
                    return
                }

                lastHeard = command
                handleCommand(command)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )?.firstOrNull().orEmpty()

                if (partial.isNotBlank()) {
                    lastHeard = partial
                    status = "Ina jin: $partial"
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun handleCommand(command: String) {
        try {
            val intent = intentEngine.understand(command)
            val response = actionRouter.route(intent)

            processing = false
            status = response
            speak(response)
        } catch (_: Exception) {
            processing = false
            status = "An samu matsala wajen aiwatar da umarnin."
            speak("An samu matsala wajen aiwatar da umarnin.")
        }
    }

    private fun startCesiService() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestRequiredPermissions()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
        ) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            status = "Bude izinin Display over other apps, sannan ka dawo CESI."
            return
        }

        try {
            val serviceIntent = Intent(this, CesiAssistantService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
            status = "CESI background assistant ya fara."
        } catch (_: Exception) {
            status = "Ban iya fara CESI background service ba."
        }
    }

    private fun speak(text: String) {
        if (!::tts.isInitialized || text.isBlank()) return

        try {
            tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "cesi_response"
            )
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.cancel()
            speechRecognizer.destroy()
        }

        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }

        super.onDestroy()
    }
}
