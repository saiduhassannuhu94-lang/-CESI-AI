package com.cesi.assistant.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice

class VoiceManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun selectedVoiceName(): String = prefs.getString(KEY_VOICE, "").orEmpty()

    fun saveVoice(voice: Voice?) {
        prefs.edit().putString(KEY_VOICE, voice?.name.orEmpty()).apply()
    }

    fun clearSelection() {
        prefs.edit().remove(KEY_VOICE).apply()
    }

    fun availableLocalVoices(tts: TextToSpeech): List<Voice> =
        tts.voices
            .filter { it.locale.language in SUPPORTED_LANGUAGES }
            // Prefer higher-quality voices; network voices are allowed because
            // they are usually more natural than basic embedded voices.
            .sortedWith(
                compareBy<Voice>(
                    { it.locale.language != "en" },
                    { -it.quality },
                    { it.latency },
                    { it.isNetworkConnectionRequired },
                    { it.locale.toLanguageTag() },
                    { it.name }
                )
            )
            .distinctBy { it.name }

    /**
     * Select the best available English voice for the device.
     *
     * Prefer an installed/local voice when there is no validated network,
     * otherwise allow a higher-quality network voice. This keeps CESI usable
     * offline while taking advantage of a more natural engine when available.
     */
    fun applyBestEnglishVoice(tts: TextToSpeech): Boolean {
        val english = tts.voices
            .filter { it.locale.language == "en" }
            .distinctBy { it.name }

        if (english.isEmpty()) return false

        val networkAvailable = try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = cm.activeNetwork
            val caps = network?.let { cm.getNetworkCapabilities(it) }
            caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        } catch (_: Exception) {
            false
        }

        val ranked = english.sortedWith(
            compareBy<Voice>(
                { it.isNetworkConnectionRequired && !networkAvailable },
                { -it.quality },
                { it.latency },
                { it.name }
            )
        )

        return try {
            tts.voice = ranked.first()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun applySavedVoice(tts: TextToSpeech): Boolean {
        val name = selectedVoiceName()
        if (name.isBlank()) return false

        val voice = tts.voices.firstOrNull { it.name == name } ?: return false
        return try {
            tts.voice = voice
            true
        } catch (_: Exception) {
            false
        }
    }

    fun applyVoice(tts: TextToSpeech, voice: Voice): Boolean =
        try {
            tts.voice = voice
            saveVoice(voice)
            true
        } catch (_: Exception) {
            false
        }

    fun previewVoice(tts: TextToSpeech, voice: Voice): Boolean =
        try {
            tts.voice = voice
            true
        } catch (_: Exception) {
            false
        }

    fun voiceForProfile(tts: TextToSpeech, profile: VoiceProfile): Voice? {
        val voices = availableLocalVoices(tts)
        if (voices.isEmpty()) return null

        val preferred = voices.filter { it.locale.language == profile.locale.substringBefore("-") }
        val pool = if (preferred.isNotEmpty()) preferred else voices
        val index = when (profile.id) {
            "male_1", "female_1" -> 0
            "male_2", "female_2" -> 1
            else -> 2
        }
        return pool.getOrElse(index % pool.size) { pool.first() }
    }

    companion object {
        private const val PREFS = "cesi_voice_preferences"
        private const val KEY_VOICE = "selected_voice_name"
        private val SUPPORTED_LANGUAGES = setOf("en", "ha", "yo", "ig")
    }
}
