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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cesi.assistant.core.memory.HistoryEntry
import com.cesi.assistant.core.memory.HistoryStore
import com.cesi.assistant.core.service.CesiAssistantService
import com.cesi.assistant.core.task.ContextTaskEngine
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speakWhatsAppNotifications = WhatsAppNotificationPrivacy.isSpeechEnabled(this)
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
                tts.setSpeechRate(0.96f)
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

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = { BottomNav() }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("CESI", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                        Text("Your Voice. Your Device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        modifier = Modifier.size(44.dp).clickable { selectedTab = 2 },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("⚙", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(10.dp),
                            shape = CircleShape,
                            color = if (errorState) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ) {}
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (errorState) "CESI needs attention" else "CESI is ready",
                                fontWeight = FontWeight.Bold
                            )
                            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CesiOrb(uiState)
                }

                if (lastHeard.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            Modifier
                                .padding(18.dp)
                                .heightIn(min = 96.dp, max = 260.dp)
                        ) {
                            Text(
                                "KA TAMBAYA",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Column(
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                            ) {
                                Text(
                                    lastHeard,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (lastResponse.isNotBlank()) {
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "CESI",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        lastResponse,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                Button(
                    onClick = { startListening() },
                    enabled = !listening && !processing && !speaking,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(if (listening) "INA SAURARO..." else "🎙  MAGANA DA CESI", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { startCesiService() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("◉  KUNNA BACKGROUND & WAKE WORD")
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickAction("History", "◷") { selectedTab = 1 }
                    QuickAction("Settings", "⚙") { selectedTab = 2 }
                    QuickAction("Voice", "🎙") { startListening() }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    @Composable
    private fun RowScope.QuickAction(title: String, icon: String, action: () -> Unit) {
        Surface(
            modifier = Modifier.weight(1f).height(72.dp).clickable(onClick = action),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(icon, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(title, style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    @Composable
    private fun HistoryScreen() {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { BottomNav() }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text("Recent CESI conversations", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        onClick = {
                            historyStore.clear()
                            historyItems = emptyList()
                            status = "History an goge."
                        },
                        enabled = historyItems.isNotEmpty()
                    ) { Text("Clear") }
                }

                if (historyItems.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("◷", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(10.dp))
                            Text("Babu history tukuna.", fontWeight = FontWeight.Bold)
                            Text("Ka yi magana da CESI domin a fara adanawa.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(historyItems) { entry -> HistoryCard(entry) }
                    }
                }
            }
        }
    }

    @Composable
    private fun HistoryCard(entry: HistoryEntry) {
        val time = remember(entry.time) {
            SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(entry.time))
        }
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text("Kai", fontWeight = FontWeight.Bold)
                Text(entry.user)
                Spacer(Modifier.height(8.dp))
                Text("CESI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(entry.assistant)
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
                    Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Permissions and CESI device access", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
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
                    SettingCard(
                        "Voice recognition",
                        "On-device recognition when supported",
                        SpeechRecognizer.isRecognitionAvailable(this@MainActivity),
                        "Test"
                    ) { startListening() }
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
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (enabled) "Ready" else "Needs permission",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                TextButton(onClick = action) { Text(buttonText) }
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
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-NG")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-NG")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
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

                val command = matches?.firstOrNull()?.trim().orEmpty()

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
