package com.cesi.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val CesiBg = Color(0xFF030A18)
private val CesiPanel = Color(0xFF07152A)
private val CesiBlue = Color(0xFF00C8FF)
private val CesiPurple = Color(0xFF6C4DFF)
private val CesiText = Color(0xFFF2F7FF)
private val CesiMuted = Color(0xFF8EA4BE)

class MainActivity : ComponentActivity() {

    private val voicePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results[Manifest.permission.RECORD_AUDIO] == true) startVoiceWakeService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestVoicePermissionsIfNeeded()

        setContent {
            CesiApp(
                onWake = {
                    if (hasVoicePermission()) startVoiceWakeService()
                    else requestVoicePermissionsIfNeeded()
                },
                onPopup = {
                    if (Settings.canDrawOverlays(this)) {
                        startService(Intent(this, FloatingCesiService::class.java))
                    } else {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${packageName}")
                            )
                        )
                    }
                }
            )
        }
    }

    private fun hasVoicePermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun requiredVoicePermissions(): Array<String> {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.toTypedArray()
    }

    private fun requestVoicePermissionsIfNeeded() {
        val missing = requiredVoicePermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) voicePermissionLauncher.launch(missing.toTypedArray())
    }

    private fun startVoiceWakeService() {
        if (!hasVoicePermission()) return
        ContextCompat.startForegroundService(this, Intent(this, VoiceWakeService::class.java))
    }
}

@Composable
private fun CesiApp(onWake: () -> Unit, onPopup: () -> Unit) {
    var selected by remember { mutableStateOf(0) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = CesiBg,
            surface = CesiPanel,
            primary = CesiBlue,
            onBackground = CesiText,
            onSurface = CesiText
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF06172B), CesiBg, Color(0xFF050713))
                    )
                )
        ) {
            Column(Modifier.fillMaxSize()) {
                CesiTopBar()
                when (selected) {
                    0 -> HomeScreen(onWake, onPopup)
                    1 -> HistoryScreen()
                    else -> SettingsScreen()
                }
                CesiBottomBar(selected) { selected = it }
            }
        }
    }
}

@Composable
private fun CesiTopBar() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).background(
                Brush.radialGradient(listOf(CesiBlue, CesiPurple, Color.Transparent)),
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Text("C", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = CesiText)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("CESI", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Hausa Voice Assistant", fontSize = 11.sp, color = CesiMuted)
        }
        Text("⚙", fontSize = 23.sp, color = CesiMuted)
    }
}

@Composable
private fun HomeScreen(onWake: () -> Unit, onPopup: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "orb")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(6.dp))
        Text("Barka da zuwa,", color = CesiBlue, fontSize = 16.sp)
        Text(
            "Ina nan don taimaka maka.",
            color = CesiText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(18.dp))

        Box(
            Modifier
                .size(238.dp)
                .scale(pulse)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF263BFF), Color(0xFF071B45), Color.Transparent)
                    ),
                    CircleShape
                )
                .border(2.dp, CesiBlue.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(158.dp).background(
                    Brush.radialGradient(listOf(Color(0xFF164D88), Color(0xFF071127))),
                    CircleShape
                ).border(4.dp, CesiBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("C", fontSize = 70.sp, fontWeight = FontWeight.Bold, color = CesiText)
            }
        }

        Spacer(Modifier.height(18.dp))
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = CesiPanel.copy(alpha = 0.92f),
            border = ButtonDefaults.outlinedButtonBorder.copy(alpha = 0.55f)
        ) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎙  Ka faɗi abin da kake so", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Ka ce “Hey CESI” ko ka danna ƙasa.", color = CesiMuted, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("📷", "Kamera", onPopup, Modifier.weight(1f))
            QuickAction("☎", "Kira", onWake, Modifier.weight(1f))
            QuickAction("▦", "Apps", onWake, Modifier.weight(1f))
            QuickAction("🔊", "Sauti", onWake, Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onWake,
            Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CesiBlue, contentColor = Color(0xFF00121E))
        ) {
            Text("🎤  KUNNA SAURARO", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuickAction(icon: String, title: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = CesiPanel,
        border = ButtonDefaults.outlinedButtonBorder.copy(alpha = 0.45f)
    ) {
        Column(
            Modifier.padding(vertical = 13.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 11.sp, color = CesiMuted)
        }
    }
}

@Composable
private fun HistoryScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Tarihi", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Tattaunawarka da CESI za su bayyana a nan.", color = CesiMuted)
        Spacer(Modifier.height(18.dp))
        InfoCard("🕘", "Babu tattaunawa tukuna", "Ka fara magana da CESI domin a adana tarihin.")
    }
}

@Composable
private fun SettingsScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Settings", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Saitunan CESI", color = CesiMuted)
        Spacer(Modifier.height(18.dp))
        InfoCard("🌐", "Harshe", "Hausa + English")
        Spacer(Modifier.height(10.dp))
        InfoCard("🎙", "Microphone", "Ana amfani da shi domin sauraro.")
        Spacer(Modifier.height(10.dp))
        InfoCard("🔒", "Tsaro", "CESI yana neman izini kafin amfani da microphone.")
    }
}

@Composable
private fun InfoCard(icon: String, title: String, subtitle: String) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CesiPanel
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 24.sp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = CesiMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CesiBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = Color(0xFF050C19)) {
        listOf("⌂" to "Gida", "◷" to "Tarihi", "⚙" to "Settings").forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = { Text(item.first, fontSize = 21.sp) },
                label = { Text(item.second, fontSize = 10.sp) }
            )
        }
    }
}
