cat > app/src/main/java/com/cesi/assistant/core/action/ActionRouter.kt <<'EOF'
package com.cesi.assistant.core.action

import android.content.Context
import com.cesi.assistant.core.intent.AssistantIntent
import com.cesi.assistant.features.apps.AppLauncher
import com.cesi.assistant.features.camera.FlashlightController
import com.cesi.assistant.features.contacts.ContactController
import com.cesi.assistant.features.device.BatteryController
import com.cesi.assistant.features.device.VolumeController
import com.cesi.assistant.features.location.LocationController
import com.cesi.assistant.features.phone.CallController
import com.cesi.assistant.features.web.WebSearchController

class ActionRouter(
    private val context: Context
) {

    private val flashlightController =
        FlashlightController(context)

    private val appLauncher =
        AppLauncher(context)

    private val callController =
        CallController(context)

    private val contactController =
        ContactController(context)

    private val locationController =
        LocationController(context)

    private val volumeController =
        VolumeController(context)

    private val batteryController =
        BatteryController(context)

    private val webSearchController =
        WebSearchController(context)

    fun route(intent: AssistantIntent): String {

        return when (intent) {

            AssistantIntent.FlashlightOn ->
                flashlightController.turnOn()

            AssistantIntent.FlashlightOff ->
                flashlightController.turnOff()

            AssistantIntent.Selfie -> {
                val selfieIntent =
                    android.content.Intent(
                        context,
                        com.cesi.assistant.features.camera.SelfieActivity::class.java
                    )

                selfieIntent.addFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                )

                context.startActivity(selfieIntent)

                "Taking a selfie."
            }

            AssistantIntent.Camera -> {
                val cameraIntent =
                    android.content.Intent(
                        android.provider.MediaStore.ACTION_IMAGE_CAPTURE
                    )

                cameraIntent.addFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                )

                context.startActivity(cameraIntent)

                "Opening camera."
            }

            AssistantIntent.Location ->
                locationController.getLocation()

            is AssistantIntent.Call ->
                callController.call(intent.target)

            is AssistantIntent.ContactSearch ->
                contactController.findContact(intent.query)

            is AssistantIntent.AppLaunch ->
                appLauncher.launch(intent.appName)

            AssistantIntent.VolumeUp ->
                volumeController.volumeUp()

            AssistantIntent.VolumeDown ->
                volumeController.volumeDown()

            AssistantIntent.Mute ->
                volumeController.mute()

            AssistantIntent.BatteryStatus ->
                batteryController.getBatteryStatus()

            is AssistantIntent.WebSearch ->
                webSearchController.search(intent.query)

            is AssistantIntent.Message ->
                "Messaging is not available yet."

            AssistantIntent.Unknown ->
                "I don't understand that command yet."
        }
    }
}
EOF
