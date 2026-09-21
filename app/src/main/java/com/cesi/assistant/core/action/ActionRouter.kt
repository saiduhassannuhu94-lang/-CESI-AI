package com.cesi.assistant.core.action

import android.content.Context
import android.content.Intent
import android.net.Uri
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

class ActionRouter(private val context: Context) {
    private val flashlight = FlashlightController(context)
    private val apps = AppLauncher(context)
    private val calls = CallController(context)
    private val contacts = ContactController(context)
    private val location = LocationController(context)
    private val volume = VolumeController(context)
    private val battery = BatteryController(context)
    private val web = WebSearchController(context)

    fun route(intent: AssistantIntent): String = when (intent) {
        AssistantIntent.FlashlightOn -> if (flashlight.setEnabled(true)) "Na kunna haske." else "Ban iya kunna haske ba."
        AssistantIntent.FlashlightOff -> if (flashlight.setEnabled(false)) "Na kashe haske." else "Ban iya kashe haske ba."
        AssistantIntent.Selfie -> try { context.startActivity(Intent(context, SelfieActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }); "Na buɗe selfie camera." } catch (_: Exception) { "Ban iya buɗe selfie camera ba." }
        AssistantIntent.Camera -> try { context.startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }); "Na buɗe camera." } catch (_: Exception) { "Ban iya buɗe camera ba." }
        AssistantIntent.Location -> location.location()
        AssistantIntent.Time -> "Yanzu lokaci " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + " ne."
        AssistantIntent.Date -> "Yau " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()) + " ne."
        AssistantIntent.OpenSettings -> openSystemSettings(Settings.ACTION_SETTINGS, "Na buɗe Settings.")
        AssistantIntent.WifiSettings -> openSystemSettings(Settings.ACTION_WIFI_SETTINGS, "Na buɗe Wi-Fi settings.")
        AssistantIntent.BluetoothSettings -> openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Na buɗe Bluetooth settings.")
        AssistantIntent.SoundSettings -> openSystemSettings(Settings.ACTION_SOUND_SETTINGS, "Na buɗe Sound settings.")
        AssistantIntent.DisplaySettings -> openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS, "Na buɗe Display settings.")
        AssistantIntent.NotificationSettings -> openAppNotificationSettings()
        is AssistantIntent.Call -> calls.call(intent.target)
        is AssistantIntent.Dial -> dial(intent.number)
        is AssistantIntent.Ussd -> dial(intent.code)
        is AssistantIntent.SetAlarm -> setAlarm(intent.hour, intent.minute, intent.label)
        is AssistantIntent.ContactSearch -> contacts.search(intent.query)
        is AssistantIntent.AppLaunch -> apps.launch(intent.appName)
        is AssistantIntent.YouTubeSearch -> web.search("site:youtube.com " + intent.query)
        AssistantIntent.VolumeUp -> volume.up()
        AssistantIntent.VolumeDown -> volume.down()
        AssistantIntent.Mute -> volume.mute()
        AssistantIntent.BatteryStatus -> battery.status()
        is AssistantIntent.WebSearch -> web.search(intent.query)
        is AssistantIntent.Message -> prepareWhatsAppMessage(intent.target, intent.text)
        AssistantIntent.Unknown -> "Ban gane da wannan umarnin ba tukuna."
    }

    private fun prepareWhatsAppMessage(target: String, text: String): String {
        val number = findPhoneNumber(target) ?: return "Ban sami lambar $target ba."
        return try {
            val cleanNumber = number.filter { it.isDigit() || it == '+' }
            val uri = Uri.parse("https://wa.me/" + cleanNumber.removePrefix("+") + "?text=" + Uri.encode(text))
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.whatsapp"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            "Na buɗe WhatsApp na $target tare da saƙon. Ka duba ka tabbatar kafin ka aika."
        } catch (_: Exception) { "Ban iya buɗe WhatsApp ba." }
    }

    private fun findPhoneNumber(query: String): String? {
        val cursor = context.contentResolver.query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$query%"), null)
        cursor?.use { if (it.moveToFirst()) return it.getString(0) }
        return null
    }

    private fun dial(raw: String): String {
        val value = raw.trim()
        if (value.isBlank()) return "Ban sami lambar da zan kira ba."
        return try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(value))).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            if (value.contains("*") || value.contains("#")) "Na buɗe dialer da $value. Duba lambar kafin ka danna kira." else "Na buɗe dialer da lambar $value."
        } catch (_: Exception) { "Ban iya buɗe dialer ba." }
    }

    private fun setAlarm(hour: Int, minute: Int, label: String?): String = try {
        context.startActivity(Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
            putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
            label?.takeIf { it.isNotBlank() }?.let { putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        "Na buɗe alarm na " + "%02d:%02d".format(hour, minute) + ". Ka tabbatar kafin ka ajiye shi."
    } catch (_: Exception) { "Ban iya buɗe alarm ba." }

    private fun openAppNotificationSettings(): String = try {
        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        "Na buɗe Notification settings."
    } catch (_: Exception) { "Ban iya buɗe Notification settings ba." }

    private fun openSystemSettings(action: String, successMessage: String): String = try {
        context.startActivity(Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        successMessage
    } catch (_: Exception) { "Ban iya buɗe wannan settings ɗin ba." }
}
