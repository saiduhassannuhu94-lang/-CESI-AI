package com.cesi.assistant

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cesi.assistant.core.voice.VoiceManager
import com.cesi.assistant.core.voice.VoiceProfile
import com.cesi.assistant.ui.CesiTheme

class VoicePickerActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech
    private lateinit var voiceManager: VoiceManager

    private var ready by mutableStateOf(false)
    private var selectedName by mutableStateOf("")
    private var voices by mutableStateOf<List<Voice>>(emptyList())
    private var previewing by mutableStateOf<String?>(null)
    private var info by mutableStateOf("Ana duba muryoyin da wayarka ta girka…")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceManager = VoiceManager(this)
        selectedName = voiceManager.selectedVoiceName()

        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                voices = voiceManager.availableLocalVoices(tts)
                voiceManager.applySavedVoice(tts)
                ready = true
                info = if (voices.isEmpty()) {
                    "Babu local voice mai dacewa da aka samu a wannan wayar."
                } else {
                    "An samo ${voices.size} local voice. Zaɓi profile sannan ka gwada Preview."
                }
            } else {
                info = "Ba a iya kunna Text-to-Speech a wannan wayar ba."
            }
        }

        setContent {
            CesiTheme {
                VoicePickerScreen()
            }
        }
    }

    @Composable
    private fun VoicePickerScreen() {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("CESI Voices", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text("3 male • 3 female • Preview & Select", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Text(info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Voice references", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Sunayen David, James, Tunde, Aisha, Zainab da Fatima su profiles ne. Android baya bada tabbataccen gender label ga TTS voices; CESI zai yi amfani da real voice da aka samu a device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(VoiceProfile.defaults) { profile ->
                    VoiceCard(profile)
                }

                item {
                    OutlinedButton(
                        onClick = {
                            voiceManager.clearSelection()
                            selectedName = ""
                            info = "An koma Auto. CESI zai yi amfani da default device voice."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Use device default / Auto")
                    }
                }
            }
        }
    }

    @Composable
    private fun VoiceCard(profile: VoiceProfile) {
        val assigned = remember(voices, profile.id) {
            if (ready) voiceManager.voiceForProfile(tts, profile) else null
        }
        val selected = assigned?.name == selectedName
        val accent = if (profile.gender == VoiceProfile.Gender.MALE) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondary
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(58.dp).background(accent.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(profile.name.take(1), color = accent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.name, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(if (profile.gender == VoiceProfile.Gender.MALE) "♂" else "♀", color = accent)
                    }
                    Text(profile.style, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Device voice: ${assigned?.name ?: "not available"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selected) {
                        Text("Selected", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    TextButton(
                        enabled = ready && assigned != null,
                        onClick = { preview(profile, assigned) }
                    ) {
                        Text(if (previewing == profile.id) "Playing…" else "▶ Preview")
                    }
                    Button(
                        enabled = ready && assigned != null,
                        onClick = {
                            assigned?.let {
                                voiceManager.applyVoice(tts, it)
                                selectedName = it.name
                                info = "${profile.name} voice an zaɓa."
                            }
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Select")
                    }
                }
            }
        }
    }

    private fun preview(profile: VoiceProfile, voice: Voice?) {
        if (voice == null) return

        if (!voiceManager.previewVoice(tts, voice)) {
            info = "Ba a iya preview wannan voice ba."
            return
        }

        previewing = profile.id
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                runOnUiThread { previewing = profile.id }
            }
            override fun onDone(utteranceId: String?) {
                runOnUiThread { previewing = null }
            }
            override fun onError(utteranceId: String?) {
                runOnUiThread { previewing = null }
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                runOnUiThread { previewing = null }
            }
        })
        tts.speak(profile.sample, TextToSpeech.QUEUE_FLUSH, null, "voice_preview_${profile.id}")
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
