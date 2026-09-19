package com.cesi.assistant.features.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryController(private val context: Context) {
    fun status(): String {
        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return "Ban iya karanta battery ba."

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        if (level < 0 || scale <= 0) return "Ban iya karanta battery ba."

        return "Battery ɗinka yana kan ${level * 100 / scale} percent."
    }
}
