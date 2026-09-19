package com.cesi.assistant.features.device

import android.content.Context
import android.media.AudioManager

class VolumeController(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun up(): String {
        audio.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        return "Na ƙara sauti."
    }

    fun down(): String {
        audio.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        return "Na rage sauti."
    }

    fun mute(): String {
        audio.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
        return "Na yi shiru."
    }
}
