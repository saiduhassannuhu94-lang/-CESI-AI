package com.cesi.assistant.core.action

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
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
                if (flashlight.setEnabled(true)) {
                    "Na kunna haske."
                } else {
                    "Ban iya kunna haske ba."
                }

            AssistantIntent.FlashlightOff ->
                if (flashlight.setEnabled(false)) {
                    "Na kashe haske."
                } else {
                    "Ban iya kashe haske ba."
                }

            AssistantIntent.Selfie -> try {
                context.startActivity(
                    Intent(context, SelfieActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                "Na buɗe selfie camera."
            } catch (_: Exception) {
                "Ban iya buɗe selfie camera ba."
            }

            AssistantIntent.Camera -> try {
                context.startActivity(
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                "Na buɗe camera."
            } catch (_: Exception) {
                "Ban iya buɗe camera ba."
            }

            AssistantIntent.Location ->
                location.location()

            is AssistantIntent.Call ->
                calls.call(intent.target)

            is AssistantIntent.ContactSearch ->
                contacts.search(intent.query)

            is AssistantIntent.AppLaunch ->
                apps.launch(intent.appName)

            AssistantIntent.VolumeUp ->
                volume.up()

            AssistantIntent.VolumeDown ->
                volume.down()

            AssistantIntent.Mute ->
                volume.mute()

            AssistantIntent.BatteryStatus ->
                battery.status()

            is AssistantIntent.WebSearch ->
                web.search(intent.query)

            is AssistantIntent.Message ->
                "Messaging bai shirya ba tukuna."

            AssistantIntent.Unknown ->
                "Ban gane da wannan umarnin ba tukuna."
        }
    }
}
