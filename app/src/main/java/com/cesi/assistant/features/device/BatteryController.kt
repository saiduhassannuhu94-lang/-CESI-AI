package com.cesi.assistant.features.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.cesi.assistant.core.task.ExecutionResult
import com.cesi.assistant.core.task.ExecutionStatus
import java.util.Locale

/**
 * Reads the current Android battery broadcast and returns a typed result.
 * A missing or malformed broadcast is a failure, never a successful empty string.
 */
class BatteryController(private val context: Context) {
    fun status(): ExecutionResult {
        val intent = try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: RuntimeException) {
            null
        } ?: return unavailable()

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return unavailable()

        val percent = level * 100 / scale
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over-voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "too cold"
            else -> "unknown"
        }

        val temperatureRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temperature = if (temperatureRaw != Int.MIN_VALUE) {
            String.format(Locale.US, "%.1f°C", temperatureRaw / 10f)
        } else {
            "unknown temperature"
        }

        val powerSource = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
            else -> "battery"
        }

        val chargeText = if (charging) "ana caji ta $powerSource" else "ba ya caji"
        return ExecutionResult(
            status = ExecutionStatus.SUCCESS,
            message = "Battery ɗinka yana kan $percent percent, $chargeText, health $health, temperature $temperature."
        )
    }

    private fun unavailable() = ExecutionResult(
        status = ExecutionStatus.FAILED,
        message = "Ban iya karanta battery ba.",
        retryable = true
    )
}
