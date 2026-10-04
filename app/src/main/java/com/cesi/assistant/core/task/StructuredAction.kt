package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent

/**
 * Platform-neutral description of an action CESI intends to perform.
 *
 * This is deliberately separate from Android execution. The browser test
 * lab and APK can consume the same semantic action contract.
 */
data class StructuredAction(
    val type: ActionType,
    val parameters: Map<String, String> = emptyMap()
)

enum class ActionType {
    TURN_FLASHLIGHT_ON,
    TURN_FLASHLIGHT_OFF,
    OPEN_CAMERA,
    TAKE_SELFIE,
    GET_LOCATION,
    CALL_CONTACT,
    OPEN_DIALER,
    RUN_USSD,
    SEARCH_CONTACT,
    OPEN_APP,
    SEARCH_YOUTUBE,
    VOLUME_UP,
    VOLUME_DOWN,
    MUTE_AUDIO,
    GET_BATTERY_STATUS,
    GET_TIME,
    GET_DATE,
    OPEN_SETTINGS,
    OPEN_WIFI_SETTINGS,
    OPEN_BLUETOOTH_SETTINGS,
    OPEN_SOUND_SETTINGS,
    OPEN_DISPLAY_SETTINGS,
    OPEN_NOTIFICATION_SETTINGS,
    SET_ALARM,
    WEB_SEARCH,
    VISUAL_SEARCH,
    SEND_MESSAGE,
    SEND_MESSENGER_MESSAGE,
    REPLY_TO_MESSAGE,
    FOLLOW_UP_TOPIC,
    GIVE_ADVICE,
    UNKNOWN
}

object StructuredActionMapper {
    fun from(intent: AssistantIntent): StructuredAction = when (intent) {
        AssistantIntent.FlashlightOn -> StructuredAction(ActionType.TURN_FLASHLIGHT_ON)
        AssistantIntent.FlashlightOff -> StructuredAction(ActionType.TURN_FLASHLIGHT_OFF)
        AssistantIntent.Selfie -> StructuredAction(ActionType.TAKE_SELFIE)
        AssistantIntent.Camera -> StructuredAction(ActionType.OPEN_CAMERA)
        AssistantIntent.Location -> StructuredAction(ActionType.GET_LOCATION)
        is AssistantIntent.Call -> StructuredAction(
            ActionType.CALL_CONTACT,
            mapOf("target" to intent.target)
        )
        is AssistantIntent.Dial -> StructuredAction(
            ActionType.OPEN_DIALER,
            mapOf("number" to intent.number)
        )
        is AssistantIntent.Ussd -> StructuredAction(
            ActionType.RUN_USSD,
            mapOf("code" to intent.code)
        )
        is AssistantIntent.ContactSearch -> StructuredAction(
            ActionType.SEARCH_CONTACT,
            mapOf("query" to intent.query)
        )
        is AssistantIntent.AppLaunch -> StructuredAction(
            ActionType.OPEN_APP,
            mapOf("app" to intent.appName)
        )
        is AssistantIntent.YouTubeSearch -> StructuredAction(
            ActionType.SEARCH_YOUTUBE,
            mapOf("query" to intent.query)
        )
        AssistantIntent.VolumeUp -> StructuredAction(ActionType.VOLUME_UP)
        AssistantIntent.VolumeDown -> StructuredAction(ActionType.VOLUME_DOWN)
        AssistantIntent.Mute -> StructuredAction(ActionType.MUTE_AUDIO)
        AssistantIntent.BatteryStatus -> StructuredAction(ActionType.GET_BATTERY_STATUS)
        AssistantIntent.Time -> StructuredAction(ActionType.GET_TIME)
        AssistantIntent.Date -> StructuredAction(ActionType.GET_DATE)
        AssistantIntent.OpenSettings -> StructuredAction(ActionType.OPEN_SETTINGS)
        AssistantIntent.WifiSettings -> StructuredAction(ActionType.OPEN_WIFI_SETTINGS)
        AssistantIntent.BluetoothSettings -> StructuredAction(ActionType.OPEN_BLUETOOTH_SETTINGS)
        AssistantIntent.SoundSettings -> StructuredAction(ActionType.OPEN_SOUND_SETTINGS)
        AssistantIntent.DisplaySettings -> StructuredAction(ActionType.OPEN_DISPLAY_SETTINGS)
        AssistantIntent.NotificationSettings -> StructuredAction(ActionType.OPEN_NOTIFICATION_SETTINGS)
        is AssistantIntent.SetAlarm -> StructuredAction(
            ActionType.SET_ALARM,
            buildMap {
                put("hour", intent.hour.toString())
                put("minute", intent.minute.toString())
                intent.label?.let { put("label", it) }
            }
        )
        is AssistantIntent.WebSearch -> StructuredAction(
            ActionType.WEB_SEARCH,
            mapOf("query" to intent.query)
        )
        is AssistantIntent.VisualSearch -> StructuredAction(
            ActionType.VISUAL_SEARCH,
            mapOf("query" to intent.query)
        )
        is AssistantIntent.Message -> StructuredAction(
            ActionType.SEND_MESSAGE,
            mapOf("target" to intent.target, "text" to intent.text)
        )
        is AssistantIntent.MessengerMessage -> StructuredAction(
            ActionType.SEND_MESSENGER_MESSAGE,
            buildMap {
                intent.target?.let { put("target", it) }
                put("text", intent.text)
            }
        )
        is AssistantIntent.Reply -> StructuredAction(
            ActionType.REPLY_TO_MESSAGE,
            mapOf("text" to intent.text)
        )
        is AssistantIntent.TopicFollowUp -> StructuredAction(
            ActionType.FOLLOW_UP_TOPIC,
            mapOf("text" to intent.text)
        )
        is AssistantIntent.Advice -> StructuredAction(
            ActionType.GIVE_ADVICE,
            mapOf("situation" to intent.situation)
        )
        AssistantIntent.Unknown -> StructuredAction(ActionType.UNKNOWN)
    }
}
