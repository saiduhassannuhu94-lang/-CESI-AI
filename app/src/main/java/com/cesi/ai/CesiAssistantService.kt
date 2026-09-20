package com.cesi.ai

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.util.*

class CesiAssistantService : Service(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    fun handleCommand(command: String) {
        val lower = command.lowercase(Locale.ROOT)
        
        when {
            // 1. OPEN WHATSAPP
            lower.contains("open whatsapp") -> {
                try {
                    val intent = packageManager.getLaunchIntentForPackage("com.whatsapp")
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        speak("Opening WhatsApp")
                    } else {
                        speak("WhatsApp not installed")
                    }
                } catch (e: Exception) {
                    speak("Cannot open WhatsApp")
                }
            }

            // 2. OPEN YOUTUBE
            lower.contains("open youtube") -> {
                try {
                    val intent = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    } else {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
                        webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(webIntent)
                    }
                    speak("Opening YouTube")
                } catch (e: Exception) {
                    speak("Cannot open YouTube")
                }
            }

            // 3. GOOGLE SEARCH FOR ME
            lower.contains("google search") || lower.contains("search for me") -> {
                var query = lower.replace("google search for me", "")
                    .replace("search for me", "")
                    .replace("google search", "")
                    .trim()
                if (query.isEmpty()) query = "CESI AI"
                
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                speak("Searching for $query")
            }

            // 4. WHERE I AM - MY LOCATION
            lower.contains("where i am") || lower.contains("my location") || lower.contains("ina nake") -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=my+location"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                speak("Showing your location")
            }

            // 5. BATTERY STATUS
            lower.contains("battery") || lower.contains("baturi") -> {
                val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                speak("Your battery is $level percent")
                Toast.makeText(this, "Battery: $level%", Toast.LENGTH_LONG).show()
            }

            // DEFAULT
            else -> {
                speak("Command not found: $command")
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
