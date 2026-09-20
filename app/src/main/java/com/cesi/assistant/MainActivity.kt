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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val voicePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            if (results[Manifest.permission.RECORD_AUDIO] == true) {
                startVoiceWakeService()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestVoicePermissionsIfNeeded()

        setContent {
            var reply by remember {
                mutableStateOf("Cesi v6 tana sauraro! Ka ba ta izinin microphone.")
            }
            val context = LocalContext.current

            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F0F1E))
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (Settings.canDrawOverlays(context)) {
                            context.startService(
                                Intent(context, FloatingCesiService::class.java)
                            )
                        } else {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + context.packageName)
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C4DFF)
                    )
                ) {
                    Text("✨ KIRA CESI POP-UP")
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (hasVoicePermission()) {
                            startVoiceWakeService()
                            reply = "Yanzu ina sauraro... ka ce 'Hey Cesi'!"
                        } else {
                            requestVoicePermissionsIfNeeded()
                            reply = "Ina bukatar izinin microphone kafin in saurare ka."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00C853)
                    )
                ) {
                    Text("🎤 KUNNA SAURARO - Hey Cesi")
                }

                Spacer(Modifier.height(16.dp))
                Text(reply, color = Color.Cyan)
            }
        }
    }

    private fun hasVoicePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

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

        if (missing.isNotEmpty()) {
            voicePermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startVoiceWakeService() {
        if (!hasVoicePermission()) return

        ContextCompat.startForegroundService(
            this,
            Intent(this, VoiceWakeService::class.java)
        )
    }
}
