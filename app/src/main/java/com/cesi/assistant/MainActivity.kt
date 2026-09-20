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

    companion object {
        private const val REQUEST_VOICE_PERMISSIONS = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestVoicePermissionsIfNeeded()

        setContent {
            var reply by remember { mutableStateOf("Cesi v6 tana sauraro! Ka ba ta izinin microphone.") }
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
                                    Uri.parse("package:${context.packageName}")
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
                            context.startForegroundService(
                                Intent(context, VoiceWakeService::class.java)
                            )
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

    private fun hasVoicePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestVoicePermissionsIfNeeded() {
        val permissions = mutableListOf<String>()

        if (!hasVoicePermission()) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.isNotEmpty()) {
            requestPermissions(
                permissions.toTypedArray(),
                REQUEST_VOICE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_VOICE_PERMISSIONS) {
            if (hasVoicePermission()) {
                startForegroundService(
                    Intent(this, VoiceWakeService::class.java)
                )
            }
        }
    }
}
