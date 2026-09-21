package com.cesi.assistant.core.action

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.provider.Settings
import com.cesi.assistant.SelfieActivity
import com.cesi.assistant.core.intent.AssistantIntent
import com.cesi.assistant.features.apps.AppLauncher
import com.cesi.assistant.features.contacts.ContactController
import com.cesi.assistant.features.device.BatteryController
import com.cesi.assistant.features.device.FlashlightController
import com.cesi.assistant.features.device.VolumeController
import com.cesi.assistant.features.location.LocationController
import com.cesi.assistant.features.phone.CallController
import com.cesi.assistant.features.web.WebSearchController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActionRouter(
    private val context: Context
) {
    private val flashlight = FlashlightController(context)
    private val apps = AppLauncher(context)
    private val calls = CallController(context)
    private val contacts = ContactController(context)
    private val location = LocationController(context)
    private val volume = VolumeController(context)
    private val battery = BatteryController(context)
    private val web = WebSearchController(context)

    fun route(intent: AssistantIntent): String {
        return when (intent) {
            AssistantIntent.FlashlightOn ->
                if (flashlight.setEnabled(true)) "Na kunna haske." else "Ban iya kunna haske ba."

            AssistantIntent.FlashlightOff ->
                if (flashlight.setEnabled(false)) "Na kashe haske." else "Ban iya kashe haske ba."

            AssistantIntent.Selfie -> try {
                context.startActivity(Intent(context, SelfieActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                "Na buɗe selfie camera."
            } catch (_: Exception) {
                "Ban iya buɗe selfie camera ba."
            }

            AssistantIntent.Camera -> try {
                context.startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                "Na buɗe camera."
            } catch (_: Exception) {
                "Ban iya buɗe camera ba."
            }

            AssistantIntent.Location -> location.location()

            AssistantIntent.Time ->
                "Yanzu lokaci " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + " ne."

            AssistantIntent.Date ->
                "Yau " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()) + " ne."

            AssistantIntent.OpenSettings ->
                openSystemSettings(Settings.ACTION_SETTINGS, "Na buɗe Settings.")

            AssistantIntent.WifiSettings ->
                openSystemSettings(Settings.ACTION_WIFI_SETTINGS, "Na buɗe Wi-Fi settings.")

            AssistantIntent.BluetoothSettings ->
                openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Na buɗe Bluetooth settings.")

            AssistantIntent.SoundSettings ->
                openSystemSettings(Settings.ACTION_SOUND_SETTINGS, "Na buɗe Sound settings.")

            AssistantIntent.DisplaySettings ->
                openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS, "Na buɗe Display settings.")

            AssistantIntent.NotificationSettings ->
                openAppNotificationSettings()

            is AssistantIntent.Call -> calls.call(intent.target)
            is AssistantIntent.ContactSearch -> contacts.search(intent.query)
            is AssistantIntent.AppLaunch -> apps.launch(intent.appName)
            AssistantIntent.VolumeUp -> volume.up()
            AssistantIntent.VolumeDown -> volume.down()
            AssistantIntent.Mute -> volume.mute()
            AssistantIntent.BatteryStatus -> battery.status()
            is AssistantIntent.WebSearch -> web.search(intent.query)
            is AssistantIntent.Message -> "Messaging bai shirya ba tukuna."
            AssistantIntent.Unknown -> "Ban gane da wannan umarnin ba tukuna."
        }
    }

    private fun openAppNotificationSettings(): String {
        return try {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            "Na buɗe Notification settings."
        } catch (_: Exception) {
            "Ban iya buɗe Notification settings ba."
        }
    }

    private fun openSystemSettings(action: String, successMessage: String): String {
        return try {
            context.startActivity(Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            successMessage
        } catch (_: Exception) {
            "Ban iya buɗe wannan settings ɗin ba."
        }
    }
}
