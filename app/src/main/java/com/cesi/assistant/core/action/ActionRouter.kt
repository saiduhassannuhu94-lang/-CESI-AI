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
        AssistantIntent.FlashlightOn ->
            if (flashlight.setEnabled(true)) "Na kunna haske." else "Ban iya kunna haske ba."

        AssistantIntent.FlashlightOff ->
            if (flashlight.setEnabled(false)) "Na kashe haske." else "Ban iya kashe haske ba."

        AssistantIntent.Selfie -> {
            context.startActivity(Intent(context, SelfieActivity::class.java))
            "Na buɗe selfie camera."
        }

        AssistantIntent.Camera -> {
            context.startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
            "Na buɗe camera."
        }

        is AssistantIntent.AppLaunch -> apps.launch(intent.query)
        is AssistantIntent.Call -> calls.call(intent.target)
        is AssistantIntent.ContactSearch -> contacts.search(intent.query)
        AssistantIntent.Location -> location.location()
        AssistantIntent.VolumeUp -> volume.up()
        AssistantIntent.VolumeDown -> volume.down()
        AssistantIntent.Mute -> volume.mute()
        AssistantIntent.BatteryStatus -> battery.status()
        is AssistantIntent.WebSearch -> web.search(intent.query)
        is AssistantIntent.Unknown -> "Na ji: ${intent.text}. Wannan skill ɗin bai gama ba tukuna."
    }
}
