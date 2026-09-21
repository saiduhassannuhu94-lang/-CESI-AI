package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent

/**
 * Maps understood intents to capabilities so the task layer can reason about
 * what an action needs without maintaining a hard-coded command list.
 */
class ActionCapabilityRegistry {

    fun capabilitiesFor(intent: AssistantIntent): Set<ActionCapability> = when (intent) {
        AssistantIntent.FlashlightOn,
        AssistantIntent.FlashlightOff,
        AssistantIntent.VolumeUp,
        AssistantIntent.VolumeDown,
        AssistantIntent.Mute,
        AssistantIntent.BatteryStatus,
        AssistantIntent.OpenSettings,
        AssistantIntent.WifiSettings,
        AssistantIntent.BluetoothSettings,
        AssistantIntent.SoundSettings,
        AssistantIntent.DisplaySettings,
        AssistantIntent.NotificationSettings ->
            setOf(ActionCapability.DEVICE_SETTINGS)

        AssistantIntent.Selfie,
        AssistantIntent.Camera ->
            setOf(ActionCapability.APP_CONTROL, ActionCapability.MEDIA)

        AssistantIntent.Location ->
            setOf(ActionCapability.LOCATION)

        is AssistantIntent.Call,
        is AssistantIntent.Dial ->
            setOf(ActionCapability.COMMUNICATION, ActionCapability.TELEPHONY)

        is AssistantIntent.Ussd ->
            setOf(ActionCapability.USSD, ActionCapability.TELEPHONY)

        is AssistantIntent.ContactSearch ->
            setOf(ActionCapability.CONTACTS)

        is AssistantIntent.AppLaunch ->
            setOf(ActionCapability.APP_CONTROL)

        is AssistantIntent.YouTubeSearch ->
            setOf(ActionCapability.APP_CONTROL, ActionCapability.WEB_SEARCH, ActionCapability.MEDIA)

        is AssistantIntent.SetAlarm ->
            setOf(ActionCapability.ALARM)

        is AssistantIntent.WebSearch ->
            setOf(ActionCapability.WEB_SEARCH)

        is AssistantIntent.Message ->
            setOf(ActionCapability.COMMUNICATION, ActionCapability.MESSAGING)

        AssistantIntent.Time,
        AssistantIntent.Date ->
            emptySet()

        AssistantIntent.Unknown ->
            emptySet()
    }
}
