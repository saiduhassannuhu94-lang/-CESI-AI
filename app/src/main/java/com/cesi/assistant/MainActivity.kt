package com.cesi.assistant

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cesi.assistant.core.action.ActionRouter
import com.cesi.assistant.core.service.CesiAssistantService

import com.cesi.assistant.core.intent.IntentEngine
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var tts: TextToSpeech
    private lateinit var intentEngine: IntentEngine
    private lateinit var actionRouter: ActionRouter

    private var recognizer: SpeechRecognizer? = null
    private var listening by mutableStateOf(false)
    private var status by mutableStateOf("Ready")

    private val permissions =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intentEngine = IntentEngine()
        actionRouter = ActionRouter(this)

        tts = TextToSpeech(this) {
            tts.language = Locale.US
        }

        permissions.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE
            )
        )

        setContent {
            CesiScreen()
        }

        val serviceIntent = Intent(
            this,
            CesiAssistantService::class.java
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun speak(text: String) {
        status = text
        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "cesi"
        )
    }

    private fun listen() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Speech recognition is not available on this phone.")
            return
        }

        recognizer?.destroy()

        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                    status = "Listening…"
                }

                override fun onBeginningOfSpeech() {
                    listening = true
                }

                override fun onEndOfSpeech() {
                    listening = false
                }

                override fun onError(error: Int) {
                    listening = false
                    status = "I couldn't hear that."
                }

                override fun onResults(results: Bundle?) {

                    listening = false

                    val text =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )?.firstOrNull().orEmpty()

                    if (text.isBlank()) {
                        speak("I didn't hear a command.")
                        return
                    }

                    handleCommand(text)
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {}
            }
        )

        val intent = Intent(
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
                RecognizerIntent.EXTRA_PROMPT,
                "Speak to CESI"
            )
        }

        recognizer?.startListening(intent)
    }

    private fun handleCommand(raw: String) {

        val intent = intentEngine.understand(raw)
        val response = actionRouter.route(intent)

        speak(response)
    }

    @Composable
    private fun CesiScreen() {

        MaterialTheme {

            Surface(
                modifier = Modifier.fillMaxSize()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(
                        "CESI",
                        style =
                            MaterialTheme.typography.displayMedium
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        "Your Voice. Your Device."
                    )

                    Spacer(
                        modifier = Modifier.height(36.dp)
                    )

                    Text(
                        if (listening)
                            "Listening…"
                        else
                            status
                    )

                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )

                    Button(
                        onClick = {
                            listen()
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            if (listening)
                                "Listening…"
                            else
                                "🎙 Talk to CESI"
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )

                    Text(
                        "CESI 0.4 • Action Router"
                    )
                }
            }
        }
    }

    override fun onDestroy() {

        recognizer?.destroy()
        tts.shutdown()

        super.onDestroy()
    }
}
