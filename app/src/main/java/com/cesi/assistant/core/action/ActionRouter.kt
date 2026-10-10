package com.cesi.assistant.core.action

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.cesi.assistant.PermissionRequestActivity
import com.cesi.assistant.SelfieActivity
import com.cesi.assistant.core.context.TopicContextStore
import com.cesi.assistant.core.advice.AdviceEngine
import com.cesi.assistant.core.intent.AssistantIntent
import com.cesi.assistant.core.task.ActionResultClassifier
import com.cesi.assistant.core.task.ExecutionResult
import com.cesi.assistant.core.task.ExecutionStatus
import com.cesi.assistant.features.apps.AppLauncher
import com.cesi.assistant.features.contacts.ContactController
import com.cesi.assistant.features.device.BatteryController
import com.cesi.assistant.features.device.FlashlightController
import com.cesi.assistant.features.device.VolumeController
import com.cesi.assistant.features.location.LocationController
import com.cesi.assistant.features.messaging.MessengerController
import com.cesi.assistant.features.notifications.CesiNotificationListenerService
import com.cesi.assistant.features.phone.CallController
import com.cesi.assistant.features.web.VisualSearchController
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
    private val visual = VisualSearchController(context)
    private val messenger = MessengerController(context)
    private val topics = TopicContextStore(context)
    private val advice = AdviceEngine()

    /**
     * Structured execution boundary used by the task engine.
     *
     * Individual Android executors will eventually return ExecutionResult
     * directly. Until then, this adapter prevents the task engine from
     * parsing human-facing strings itself.
     */
    fun routeResult(intent: AssistantIntent): ExecutionResult = when (intent) {
        // These capabilities now return typed outcomes directly. The legacy
        // string classifier is only used for executors that have not migrated.
        AssistantIntent.FlashlightOn -> flashlight.setEnabled(true)
        AssistantIntent.FlashlightOff -> flashlight.setEnabled(false)
        AssistantIntent.VolumeUp -> volume.up().message
        AssistantIntent.VolumeDown -> volume.down().message
        AssistantIntent.Mute -> volume.mute().message
        AssistantIntent.BatteryStatus -> battery.status().message

        AssistantIntent.Time -> ExecutionResult(
            status = ExecutionStatus.SUCCESS,
            message = timeMessage()
        )
        AssistantIntent.Date -> ExecutionResult(
            status = ExecutionStatus.SUCCESS,
            message = dateMessage()
        )

        else -> ActionResultClassifier.classify(route(intent), intent)
    }

    fun route(intent: AssistantIntent): String = when (intent) {
        AssistantIntent.FlashlightOn -> flashlight.setEnabled(true).message
        AssistantIntent.FlashlightOff -> flashlight.setEnabled(false).message
        AssistantIntent.Selfie -> try { context.startActivity(Intent(context, SelfieActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }); "Na buɗe selfie camera." } catch (_: Exception) { "Ban iya buɗe selfie camera ba." }
        AssistantIntent.Camera -> try { context.startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }); "Na buɗe camera." } catch (_: Exception) { "Ban iya buɗe camera ba." }

        AssistantIntent.Location ->
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                requestPermission(PermissionRequestActivity.KIND_LOCATION)
                "Na buɗe permission na Location. Ka danna Allow, sannan ka sake cewa location ɗinka."
            } else location.location()

        AssistantIntent.Time -> timeMessage()
        AssistantIntent.Date -> dateMessage()
        AssistantIntent.OpenSettings -> openSystemSettings(Settings.ACTION_SETTINGS, "Na buɗe Settings.")
        AssistantIntent.WifiSettings -> openSystemSettings(Settings.ACTION_WIFI_SETTINGS, "Na buɗe Wi-Fi settings.")
        AssistantIntent.BluetoothSettings -> openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Na buɗe Bluetooth settings.")
        AssistantIntent.SoundSettings -> openSystemSettings(Settings.ACTION_SOUND_SETTINGS, "Na buɗe Sound settings.")
        AssistantIntent.DisplaySettings -> openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS, "Na buɗe Display settings.")
        AssistantIntent.NotificationSettings -> openAppNotificationSettings()

        is AssistantIntent.Call -> {
            val numberLike = intent.target.trim().matches(Regex("[+0-9][0-9 ()-]{5,}"))
            if (!hasPermission(Manifest.permission.CALL_PHONE) || (!numberLike && !hasPermission(Manifest.permission.READ_CONTACTS))) {
                requestPermission(PermissionRequestActivity.KIND_CALL)
                "Na buɗe permission na kira. Ka danna Allow, sannan ka sake cewa a kira " + intent.target + "."
            } else calls.call(intent.target)
        }

        is AssistantIntent.Dial -> dial(intent.number)

        is AssistantIntent.Ussd -> dial(intent.code)

        is AssistantIntent.SetAlarm -> setAlarm(intent.hour, intent.minute, intent.label)

        is AssistantIntent.ContactSearch ->
            if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
                requestPermission(PermissionRequestActivity.KIND_CONTACTS)
                "Na buɗe permission na Contacts. Ka danna Allow, sannan ka sake neman contact ɗin."
            } else contacts.search(intent.query)

        is AssistantIntent.AppLaunch -> apps.launch(intent.appName)
        is AssistantIntent.YouTubeSearch -> web.youtubeSearch(intent.query)
        AssistantIntent.VolumeUp -> volume.up()
        AssistantIntent.VolumeDown -> volume.down()
        AssistantIntent.Mute -> volume.mute()
        AssistantIntent.BatteryStatus -> battery.status()

        is AssistantIntent.WebSearch -> {
            topics.setTopic(intent.query)
            web.search(intent.query)
        }

        is AssistantIntent.VisualSearch -> {
            topics.setTopic(intent.query)
            visual.search(intent.query)
        }

        is AssistantIntent.TopicFollowUp -> {
            val topic = topics.getTopic()
            if (topic.isBlank()) {
                "Ban da wani topic a context yanzu. Ka faɗa min topic ɗin farko."
            } else if (
                intent.text.contains("picture") || intent.text.contains("image") ||
                intent.text.contains("photo") || intent.text.contains("diagram") ||
                intent.text.contains("hoto") || intent.text.contains("hotuna")
            ) {
                visual.search(topic)
            } else {
                web.search(topic + " " + intent.text)
            }
        }

        is AssistantIntent.Message -> prepareWhatsAppMessage(intent.target, intent.text)
        is AssistantIntent.MessengerMessage -> messenger.draft(intent.target, intent.text)
        is AssistantIntent.Reply -> if (CesiNotificationListenerService.replyLatestWhatsApp(intent.text)) {
            "Na tura reply kai tsaye ta WhatsApp notification."
        } else {
            prepareWhatsAppDraft(intent.text)
        }
        is AssistantIntent.Advice -> advice.respond(intent.situation)

        AssistantIntent.Unknown -> "Ban gane da wannan umarnin ba tukuna."
    }

    private fun timeMessage(): String =
        "Yanzu lokaci " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + " ne."

    private fun dateMessage(): String =
        "Yau " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()) + " ne."

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission(kind: String) {
        try {
            context.startActivity(
                Intent(context, PermissionRequestActivity::class.java).apply {
                    putExtra(PermissionRequestActivity.EXTRA_KIND, kind)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
        } catch (_: Exception) {}
    }

    private fun prepareWhatsAppMessage(target: String, text: String): String {
        if (!hasInternet()) return "WhatsApp yana bukatar internet. Ka kunna data ko Wi-Fi sannan ka sake cewa a tura."

        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            requestPermission(PermissionRequestActivity.KIND_CONTACTS)
            return "Na buɗe permission na Contacts. Ka danna Allow, sannan ka sake neman lambar $target."
        }

        val number = findPhoneNumber(target) ?: return "Ban sami lambar $target ba."
        return try {
            val cleanNumber = number.filter { it.isDigit() || it == '+' }
            val uri = Uri.parse("https://wa.me/" + cleanNumber.removePrefix("+") + "?text=" + Uri.encode(text))
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            "Na shirya saƙon WhatsApp zuwa $target. Ka duba ka tabbatar kafin ka aika."
        } catch (_: Exception) {
            "Ban iya buɗe WhatsApp ba."
        }
    }

    private fun prepareWhatsAppDraft(text: String): String {
        if (!hasInternet()) return "WhatsApp yana bukatar internet. Ka kunna data ko Wi-Fi sannan ka sake cewa a reply."

        return try {
            val uri = Uri.parse("https://wa.me/?text=" + Uri.encode(text))
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            "Na shirya reply a WhatsApp. Ka zaɓi chat ɗin da za a tura masa sannan ka tabbatar."
        } catch (_: Exception) {
            "Ban iya buɗe WhatsApp ba."
        }
    }

    private fun findPhoneNumber(query: String): String? {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            requestPermission(PermissionRequestActivity.KIND_CONTACTS)
            return null
        }

        return try {
            val cursor = context.contentResolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(0)
                } else {
                    null
                }
            }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun hasInternet(): Boolean =
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }

    private fun dial(raw: String): String {
        val value = raw.trim()
        if (value.isBlank()) return "Ban sami lambar da zan kira ba."
        return try {
            context.startActivity(
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(value))).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            if (value.contains("*") || value.contains("#")) "Na buɗe dialer da $value. Duba lambar kafin ka danna kira."
            else "Na buɗe dialer da lambar $value."
        } catch (_: Exception) {
            "Ban iya buɗe dialer ba."
        }
    }

    private fun setAlarm(hour: Int, minute: Int, label: String?): String = try {
        context.startActivity(Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
            putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
            label?.takeIf { it.isNotBlank() }?.let { putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        "Na buɗe alarm na " + "%02d:%02d".format(hour, minute) + ". Ka tabbatar kafin ka ajiye shi."
    } catch (_: Exception) {
        "Ban iya buɗe alarm ba."
    }

    private fun openAppNotificationSettings(): String = try {
        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        "Na buɗe Notification settings."
    } catch (_: Exception) {
        "Ban iya buɗe Notification settings ba."
    }

    private fun openSystemSettings(action: String, successMessage: String): String = try {
        context.startActivity(Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        successMessage
    } catch (_: Exception) {
        "Ban iya buɗe wannan settings ɗin ba."
    }
}
