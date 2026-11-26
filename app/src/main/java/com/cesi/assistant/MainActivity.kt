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
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.cesi.assistant.ui.CesiTheme
import com.cesi.assistant.ui.CesiUiState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cesi.assistant.core.memory.HistoryEntry
import com.cesi.assistant.core.memory.HistoryStore
import com.cesi.assistant.core.service.CesiAssistantService
import com.cesi.assistant.core.task.ContextTaskEngine
import com.cesi.assistant.core.voice.VoiceCommandCandidateSelector
import com.cesi.assistant.core.voice.VoiceInputPreferences
import com.cesi.assistant.core.voice.VoiceManager
import com.cesi.assistant.features.notifications.CesiNotificationListenerService
import com.cesi.assistant.features.notifications.WhatsAppNotificationPrivacy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_PERMISSIONS = 7001
    }

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var historyStore: HistoryStore
    private lateinit var contextTaskEngine: ContextTaskEngine

    private var selectedTab by mutableStateOf(0)
    private var historyItems by mutableStateOf<List<HistoryEntry>>(emptyList())
    private var listening by mutableStateOf(false)
    private var processing by mutableStateOf(false)
    private var speaking by mutableStateOf(false)
    private var errorState by mutableStateOf(false)
    private var status by mutableStateOf("A shirye nake.")
    private var lastHeard by mutableStateOf("")
    private var lastResponse by mutableStateOf("")
    private var rmsLevel by mutableStateOf(0.18f)
    private var speakWhatsAppNotifications by mutableStateOf(false)
    private var voiceInputLanguage by mutableStateOf(VoiceInputPreferences.ENGLISH_NIGERIA)
    private var speechRate by mutableStateOf(0.88f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speakWhatsAppNotifications = WhatsAppNotificationPrivacy.isSpeechEnabled(this)
        voiceInputLanguage = VoiceInputPreferences.selectedLanguage(this)
        speechRate = getSharedPreferences("cesi_voice_preferences", MODE_PRIVATE).getFloat("speech_rate", CesiSpeechRate.DEFAULT)
        contextTaskEngine = ContextTaskEngine(this)
        historyStore = HistoryStore(this)
        historyItems = historyStore.getAll().reversed()
        historyItems.firstOrNull()?.let {
            lastHeard = it.user
            lastResponse = it.assistant
        }

        tts = TextToSpeech(this, TextToSpeech.OnInitListener { result ->
            if (result == TextToSpeech.SUCCESS) {
                val preferred = Locale("en", "NG")
                val available = tts.availableLanguages
                if (available != null && available.contains(preferred)) {
                    tts.language = preferred
                } else {
                    tts.language = Locale.US
                }
                val voiceManager = VoiceManager(this@MainActivity)
                if (!voiceManager.applySavedVoice(tts)) {
                    voiceManager.applyBestEnglishVoice(tts)
                }
                tts.setSpeechRate(speechRate)
                tts.setPitch(1.0f)
            }
        }, preferredTtsEngine())
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                runOnUiThread {
                    speaking = true
                    processing = false
                    errorState = false
                    status = "Ina magana..."
                }
            }

            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    speaking = false
                    status = "A shirye nake."
                }
            }

            @Deprecated("Deprecated by Android API")
            override fun onError(utteranceId: String?) {
                runOnUiThread {
                    speaking = false
                    errorState = true
                    status = "An samu matsala wajen magana."
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                runOnUiThread {
                    speaking = false
                    errorState = true
                    status = "An samu matsala wajen magana."
                }
            }
        })

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = try {
                SpeechRecognizer.createSpeechRecognizer(this)
            } catch (_: Exception) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
                ) SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
                else throw IllegalStateException("No speech recognizer available")
            }
            speechRecognizer.setRecognitionListener(createRecognitionListener())
        }

        setContent {
            CesiTheme {
                CesiScreen()
            }
        }

        requestRequiredPermissions()
    }

    @Composable
    private fun CesiScreen() {
        when (selectedTab) {
            1 -> HistoryScreen()
            2 -> SettingsScreen()
            else -> HomeScreen()
        }
    }

    @Composable
    private fun HomeScreen() {
        val uiState = when {
            errorState -> CesiUiState.Error
            speaking -> CesiUiState.Speaking
            listening -> CesiUiState.Listening
            processing -> CesiUiState.Processing
            else -> CesiUiState.Idle
        }
        val stateLabel = when (uiState) {
            CesiUiState.Error -> "NEEDS ATTENTION"
            CesiUiState.Listening -> "LISTENING"
            CesiUiState.Processing -> "THINKING"
            CesiUiState.Speaking -> "SPEAKING"
            CesiUiState.Idle -> "READY WHEN YOU ARE"
        }
        val accent = when (uiState) {
            CesiUiState.Error -> MaterialTheme.colorScheme.error
            CesiUiState.Processing -> Color(0xFF9C8BFF)
            else -> Color(0xFF5CE1F5)
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = { BottomNav() }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 22.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "CESI",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 5.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "YOUR VOICE. YOUR DEVICE.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.8.sp
                        )
                    }
                    Surface(
                        modifier = Modifier.size(46.dp).clickable { selectedTab = 2 },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF111C2C),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF24364A))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("⚙", color = Color(0xFF9DB4C8), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color(0xFF101B2A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(7.dp).background(accent, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.3.sp,
                            color = accent
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().height(310.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(290.dp).background(
                            Brush.radialGradient(listOf(accent.copy(alpha = 0.13f), Color.Transparent)),
                            CircleShape
                        )
                    )
                    CesiOrb(uiState)
                }

                Text(
                    when (uiState) {
                        CesiUiState.Error -> "Something needs your attention"
                        CesiUiState.Listening -> "I'm listening"
                        CesiUiState.Processing -> "Thinking it through"
                        CesiUiState.Speaking -> "Here's what I found"
                        CesiUiState.Idle -> "What can I help you with?"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (lastHeard.isNotBlank()) {
                    Spacer(Modifier.height(20.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF0C1523),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3044))
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                "RECENT EXCHANGE",
                                color = Color(0xFF5CE1F5),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(lastHeard, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            if (lastResponse.isNotBlank()) {
                                Spacer(Modifier.height(14.dp))
                                Text("CESI", color = Color(0xFF9C8BFF), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(5.dp))
                                Text(lastResponse, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = { startListening() },
                    enabled = !listening && !processing && !speaking,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5CE1F5),
                        contentColor = Color(0xFF05131B),
                        disabledContainerColor = Color(0xFF253545),
                        disabledContentColor = Color(0xFF8293A3)
                    )
                ) {
                    Text(
                        if (listening) "LISTENING..." else "TALK TO CESI",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.4.sp
                    )
                }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { startCesiService() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B4156)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB7C8D8))
                ) {
                    Text("ENABLE BACKGROUND ASSISTANT", fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                }

                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAction("History", "◷") { selectedTab = 1 }
                    QuickAction("Settings", "⚙") { selectedTab = 2 }
                    QuickAction("Voice", "♫") { startListening() }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    @Composable
    private fun RowScope.QuickAction(title: String, icon: String, action: () -> Unit) {
        Surface(
            modifier = Modifier.weight(1f).height(84.dp).clickable(onClick = action),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0D1725),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF203246))
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(icon, color = Color(0xFF5CE1F5), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color(0xFFB6C7D8), fontWeight = FontWeight.Medium)
            }
        }
    }

    @Composable
    private fun HistoryScreen() {
        Scaffold(
            containerColor = Color(0xFF07111D),
            bottomBar = { BottomNav() }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFF123144)) {
                        Text("CONVERSATION ARCHIVE", Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color(0xFF67D9F5), style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("History", style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black, color = Color(0xFFF2F7FC))
                            Text("Your recent conversations with CESI", color = Color(0xFF94A8BB))
                        }
                        TextButton(
                            onClick = {
                                historyStore.clear()
                                historyItems = emptyList()
                                status = "History an goge."
                            },
                            enabled = historyItems.isNotEmpty(),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF67D9F5))
                        ) { Text("CLEAR", fontWeight = FontWeight.Bold) }
                    }
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF102231), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF203B50))) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("◷", color = Color(0xFF67D9F5), fontSize = 20.sp)
                            Column {
                                Text("${historyItems.size} saved exchange${if (historyItems.size == 1) "" else "s"}",
                                    color = Color(0xFFE3F1FA), fontWeight = FontWeight.Bold)
                                Text("Stored on this device. Clear anytime.", color = Color(0xFF90A7BB),
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                if (historyItems.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = CircleShape, color = Color(0xFF102A3B)) {
                                Text("◷", Modifier.padding(28.dp), style = MaterialTheme.typography.displaySmall,
                                    color = Color(0xFF67D9F5))
                            }
                            Text("Nothing saved yet", fontWeight = FontWeight.Bold,
                                color = Color(0xFFE3F1FA), style = MaterialTheme.typography.titleLarge)
                            Text("Talk with CESI to create your first conversation record.",
                                color = Color(0xFF90A7BB), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(historyItems) { entry -> HistoryCard(entry) }
                    }
                }
            }
        }
    }

    @Composable
    private HistoryCard(entry: HistoryEntry) {
        val time = remember(entry.time) {
            SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(entry.time))
        }
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0C1725),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3347))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFF123144)) {
                        Text(time, Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = Color(0xFF67D9F5), style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("SAVED", color = Color(0xFF7190A7), style = MaterialTheme.typography.labelSmall)
                }
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF142638)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("YOU", color = Color(0xFF9AB2C8), style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(entry.user, color = Color(0xFFE5F0FA))
                    }
                }
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF102A3B)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("CESI", color = Color(0xFF67D9F5), style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(entry.assistant, color = Color(0xFFD9E8F5))
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsScreen() {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { BottomNav() }
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(50), color = Color(0xFF123144)) {
                                Text("CESI CONTROL", Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Color(0xFF67D9F5), style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                            }
                        }
                        Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black,
                            color = Color(0xFFF2F7FC))
                        Text("Tune CESI's access, voice, and behavior. Nothing is enabled silently.",
                            color = Color(0xFF94A8BB), style = MaterialTheme.typography.bodyMedium)
                        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF102231), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF203B50))) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("◈", color = Color(0xFF65D6F4), fontSize = 20.sp)
                                Column {
                                    Text("Permission-first control", color = Color(0xFFE3F1FA), fontWeight = FontWeight.Bold)
                                    Text("Review every access. Enable only what you actually use.",
                                        color = Color(0xFF90A7BB), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                item {
                    SettingCard(
                        "Microphone",
                        "Voice commands",
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
                        "Allow"
                    ) { requestRequiredPermissions() }
                }
                item {
                    SettingCard(
                        "Floating CESI",
                        "Orb over other apps",
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this@MainActivity),
                        "Open"
                    ) { openOverlaySettings() }
                }
                item {
                    SettingCard(
                        "Notification Access",
                        "Read WhatsApp notification messages for reply assistance",
                        isNotificationAccessEnabled(),
                        "Open"
                    ) { openNotificationAccessSettings() }
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Speak WhatsApp notifications aloud",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Off by default. When enabled, CESI may speak message text aloud. The latest message is held in process memory for up to five minutes for reply suggestions; it is not written to general history or preferences.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = speakWhatsAppNotifications,
                                onCheckedChange = { enabled ->
                                    speakWhatsAppNotifications = enabled
                                    WhatsAppNotificationPrivacy.setSpeechEnabled(this@MainActivity, enabled)
                                    if (!enabled) CesiNotificationListenerService.stopSpeaking()
                                }
                            )
                        }
                    }
                }
                item {
                    SettingCard(
                        "Caller ID",
                        "Identify incoming calls from saved contacts",
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED,
                        "Allow"
                    ) {
                        startActivity(
                            Intent(this@MainActivity, PermissionRequestActivity::class.java).apply {
                                putExtra(PermissionRequestActivity.EXTRA_KIND, PermissionRequestActivity.KIND_CALLER_ID)
                            }
                        )
                    }
                }
                item {
                    SettingCard(
                        "Accessibility",
                        "Screen-control features",
                        isAccessibilityEnabled(),
                        "Open"
                    ) { openAccessibilitySettings() }
                }
                item {
                    SettingCard(
                        "CESI Voice",
                        "3 male + 3 female profiles with Preview",
                        true,
                        "Open"
                    ) {
                        startActivity(Intent(this@MainActivity, VoicePickerActivity::class.java))
                    }
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Speaking pace", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Slower, more relaxed delivery. Voice quality still depends on the speech engine installed on your phone.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text("%.2f×".format(speechRate), color = Color(0xFF67D9F5), fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = speechRate,
                                onValueChange = { rate ->
                                    speechRate = CesiSpeechRate.clamp(rate)
                                    getSharedPreferences("cesi_voice_preferences", MODE_PRIVATE)
                                        .edit().putFloat("speech_rate", speechRate).apply()
                                    if (::tts.isInitialized) tts.setSpeechRate(speechRate)
                                },
                                valueRange = CesiSpeechRate.MIN..CesiSpeechRate.MAX,
                                steps = 5
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Relaxed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Normal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    SettingCard(
                        "Voice recognition",
                        "On-device recognition when supported",
                        SpeechRecognizer.isRecognitionAvailable(this@MainActivity),
                        "Test"
                    ) { startListening() }
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Voice input language", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "This changes what CESI listens for, not its English spoken replies. Hausa needs a supported, installed speech model. Automatic switching needs Android 14+ and a compatible provider. In Hausa/Auto mode, say “Hey CESI” first, wait for Listening, then give the command.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            val options = buildList {
                                add(VoiceInputPreferences.ENGLISH_NIGERIA to "English (Nigeria)")
                                add(VoiceInputPreferences.HAUSA_NIGERIA to "Hausa (Nigeria)")
                                if (Build.VERSION.SDK_INT >= 34) {
                                    add(VoiceInputPreferences.AUTOMATIC to "Automatic English/Hausa (Android 14+)")
                                }
                            }
                            options.forEach { (tag, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            VoiceInputPreferences.setLanguage(this@MainActivity, tag)
                                            voiceInputLanguage = tag
                                            status = when (tag) {
                                                VoiceInputPreferences.HAUSA_NIGERIA ->
                                                    "An zaɓi Hausa don fahimtar umarni."
                                                VoiceInputPreferences.AUTOMATIC ->
                                                    "An zaɓi gano English/Hausa ta atomatik."
                                                else -> "An zaɓi English (Nigeria)."
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = voiceInputLanguage == tag,
                                        onClick = {
                                            VoiceInputPreferences.setLanguage(this@MainActivity, tag)
                                            voiceInputLanguage = tag
                                            status = "An sabunta harshen sauraron CESI."
                                        }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                item {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Internet policy", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "CESI ba ya bukatar Mobile Data domin magana ko offline device actions. Idan action ya bukaci internet, CESI zai sanar da kai.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingCard(
        title: String,
        subtitle: String,
        enabled: Boolean,
        buttonText: String,
        action: () -> Unit
    ) {
        Surface(
            Modifier.fillMaxWidth().clickable(onClick = action),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0C1725),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3347))
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(shape = RoundedCornerShape(14.dp), color = if (enabled) Color(0xFF103144) else Color(0xFF33252C)) {
                    Text(if (enabled) "✓" else "!", Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                        color = if (enabled) Color(0xFF65D6F4) else Color(0xFFFFB4A9),
                        fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, color = Color(0xFFEAF3FB), fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF91A7BA))
                    Surface(shape = RoundedCornerShape(30.dp), color = if (enabled) Color(0xFF12342F) else Color(0xFF33252C)) {
                        Text(if (enabled) "ENABLED" else "ACTION REQUIRED",
                            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            color = if (enabled) Color(0xFF83E3BE) else Color(0xFFFFB4A9),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = action, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF67D9F5))) {
                    Text(buttonText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    private fun BottomNav() {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = { Text("●") },
                label = { Text("Gida") }
            )
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = {
                    historyItems = historyStore.getAll().reversed()
                    selectedTab = 1
                },
                icon = { Text("◷") },
                label = { Text("History") }
            )
            NavigationBarItem(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                icon = { Text("⚙") },
                label = { Text("Settings") }
            )
        }
    }

    @Composable
    private fun CesiOrb(state: CesiUiState) {
        val transition = rememberInfiniteTransition(label = "cesi_orb")
        val basePulse by transition.animateFloat(
            initialValue = 0.96f,
            targetValue = when (state) {
                CesiUiState.Idle -> 1.02f
                CesiUiState.Listening -> 1.08f
                CesiUiState.Processing -> 1.05f
                CesiUiState.Speaking -> 1.07f
                CesiUiState.Error -> 1f
            },
            animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
            label = "orb_pulse"
        )
        val pulse = basePulse + if (state == CesiUiState.Listening) rmsLevel * 0.08f else 0f
        val flow by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(4200), RepeatMode.Restart),
            label = "orb_flow"
        )
        val driftX by transition.animateFloat(
            initialValue = -10f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
            label = "orb_drift_x"
        )
        val driftY by transition.animateFloat(
            initialValue = 10f,
            targetValue = -10f,
            animationSpec = infiniteRepeatable(tween(2300), RepeatMode.Reverse),
            label = "orb_drift_y"
        )
        val stateColor = when (state) {
            CesiUiState.Error -> MaterialTheme.colorScheme.error
            CesiUiState.Processing -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.primary
        }

        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(pulse),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(224.dp)
                    .rotate(flow)
                    .offset(x = driftX.dp, y = driftY.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                stateColor.copy(alpha = 0.42f),
                                stateColor.copy(alpha = 0.16f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .rotate(-flow * 0.72f)
                    .offset(x = (-driftY * 0.7f).dp, y = (driftX * 0.7f).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF7A6CFF).copy(alpha = 0.34f),
                                stateColor.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            Surface(
                modifier = Modifier.size(174.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                tonalElevation = 12.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    stateColor.copy(alpha = 0.28f),
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.background
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state == CesiUiState.Error) "!" else "CESI",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = stateColor
                    )
                }
            }
            Surface(
                modifier = Modifier.size(188.dp),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(Color.Transparent, stateColor.copy(alpha = 0.10f))
                            ),
                            CircleShape
                        )
                )
            }
        }
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

    private fun requestRequiredPermissions() {
        // Ask only for the microphone needed for voice interaction at startup.
        // Other sensitive permissions are requested only when a feature actually needs them.
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

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
            errorState = true
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
        // Keep the previous completed exchange visible while CESI listens.

        try {
            speechRecognizer.cancel()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                VoiceInputPreferences.configureRecognizerIntent(this@MainActivity, this)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            }

            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            listening = false
            errorState = true
            status = "An samu matsala wajen kunna microphone."
            speak("Ban iya kunna microphone ba.")
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
                errorState = false
                status = "Ina sauraro..."
            }

            override fun onBeginningOfSpeech() {
                listening = true
                errorState = false
                status = "Ina jin muryarka..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                rmsLevel = ((rmsdB + 10f) / 20f).coerceIn(0.08f, 1f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                listening = false
                processing = true
                speaking = false
                errorState = false
                status = "Ina fahimtar umarnin..."
            }

            override fun onError(error: Int) {
                listening = false
                processing = false
                speaking = false
                errorState = true
                status = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Ba a ba CESI microphone permission ba."
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "CESI na iya yin magana offline, amma wannan wayar ta koma online speech recognition. Ka duba network ko speech pack."
                    SpeechRecognizer.ERROR_NO_MATCH -> "Ban ji kalmomin sosai ba. Sake gwadawa."
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ->
                        "Wannan speech provider bai goyi bayan harshen da aka zaɓa ba. Zaɓi English ko wani speech provider."
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                        "Ba a sauke speech model ɗin harshen da aka zaɓa ba. Sauke Hausa speech pack ko zaɓi English."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer yana aiki. Sake gwadawa."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Ban ji magana ba."
                    else -> "An samu matsala wajen sauraro. Sake gwadawa."
                }
            }

            override fun onResults(results: Bundle?) {
                listening = false
                processing = true
                speaking = false
                errorState = false

                val matches = results?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )

                val command = VoiceCommandCandidateSelector.select(matches.orEmpty()).orEmpty()

                if (command.isBlank()) {
                    processing = false
                    errorState = true
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
        // Execute actions away from the UI thread so location/network/app
        // operations cannot freeze the conversation screen.
        Thread {
            try {
                val response = contextTaskEngine.execute(command)

                runOnUiThread {
                    processing = false
                    errorState = false
                    status = response
                    lastResponse = response
                    historyStore.add(command, response)
                    historyItems = historyStore.getAll().reversed()
                    speak(response)
                }
            } catch (_: Exception) {
                runOnUiThread {
                    processing = false
                    speaking = false
                    errorState = true
                    status = "An samu matsala wajen aiwatar da umarnin."
                    lastResponse = status
                    speak("An samu matsala wajen aiwatar da umarnin.")
                }
            }
        }.start()
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

            // Start the separate wake listener only after the user has
            // explicitly enabled CESI from a visible activity.
            val wakeIntent = Intent(this, VoiceWakeService::class.java)
            ContextCompat.startForegroundService(this, wakeIntent)

            status = "CESI background assistant da Voice Wake sun fara."
        } catch (_: Exception) {
            status = "Ban iya fara CESI background assistant ba."
        }
    }

    private fun openOverlaySettings() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            try { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) }
            catch (_: Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        }
    }

    private fun openNotificationAccessSettings() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        return enabled.contains(packageName)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabled.contains(packageName)
    }

    private fun speak(text: String) {
        if (!::tts.isInitialized || text.isBlank()) return

        val spokenText = CesiSpeechFormatter.format(text)
        if (spokenText.isBlank()) return

        try {
            tts.setSpeechRate(speechRate)
            tts.speak(
                spokenText,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "cesi_response"
            )
        } catch (_: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        if (::historyStore.isInitialized) historyItems = historyStore.getAll().reversed()
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
