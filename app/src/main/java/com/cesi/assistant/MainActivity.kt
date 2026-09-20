package com.cesi.assistant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var text by remember { mutableStateOf("") }
            var reply by remember { mutableStateOf("Cesi v6 tana sauraro! Ka ce 'Cesi'") }
            val context = LocalContext.current
            Column(Modifier.fillMaxSize().background(Color(0xFF0F0F1E)).padding(16.dp)) {
                Button(onClick = {
                    if (Settings.canDrawOverlays(context)) {
                        context.startService(Intent(context, FloatingCesiService::class.java))
                    } else {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                    }
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))) {
                    Text("✨ KIRA CESI POP-UP")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    context.startForegroundService(Intent(context, VoiceWakeService::class.java))
                    reply = "Yanzu ina sauraro... ka ce 'Cesi' da baki!"
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))) {
                    Text("🎤 KUNNA SAURARO - Hey Cesi")
                }
                Spacer(Modifier.height(16.dp))
                Text(reply, color = Color.Cyan)
            }
        }
    }
}
