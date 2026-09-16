package com.cesi.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech
    private var recognizer: SpeechRecognizer? = null
    private var listening by mutableStateOf(false)
    private var status by mutableStateOf("Ready")

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { tts.language = Locale.US }
        permissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE))
        setContent { CesiScreen() }
    }

    private fun speak(text: String) { status = text; tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cesi") }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { speak("Speech recognition is not available on this phone."); return }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : android.speech.RecognitionListener {
                override fun onResults(results: Bundle) { listening = false; val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(); handleCommand(text) }
                override fun onError(error: Int) { listening = false; status = "I couldn't hear that." }
                override fun onReadyForSpeech(p: Bundle?) { listening = true; status = "Listening…" }
                override fun onBeginningOfSpeech() {} ; override fun onRmsChanged(r: Float) {} ; override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() { listening = false }; override fun onPartialResults(b: Bundle?) {} ; override fun onEvent(t: Int, b: Bundle?) {}
            })
        }
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-NG"); putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to CESI") }
        recognizer!!.startListening(i)
    }

    private fun handleCommand(raw: String) {
        val c = raw.lowercase(Locale.getDefault()).trim()
        when {
            c.contains("camera") || c.contains("take photo") || c.contains("take a picture") || c.contains("selfie") -> { speak("Opening camera."); startActivity(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)) }
            c.contains("flashlight") || c.contains("torch") -> { speak("Flashlight control is next in the device skill.") }
            c.contains("location") || c.contains("where am i") -> { speak("Location skill is connected in the next build; Android location permission is already prepared.") }
            c.startsWith("call ") || c.contains(" kira ") || c.startsWith("kira ") -> { speak("Call skill is prepared; contact resolution will be added next.") }
            else -> speak("I heard: $raw. I don't have that skill yet.")
        }
    }

    @Composable private fun CesiScreen() {
        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("CESI", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Your Voice. Your Device.")
                    Spacer(Modifier.height(36.dp))
                    Text(if (listening) "Listening…" else status)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { listen() }, modifier = Modifier.fillMaxWidth()) { Text(if (listening) "Listening…" else "🎙 Talk to CESI") }
                    Spacer(Modifier.height(24.dp))
                    Text("CESI 0.1 • Voice foundation")
                }
            }
        }
    }

    override fun onDestroy() { recognizer?.destroy(); tts.shutdown(); super.onDestroy() }
}
