package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent

enum class ExecutionSupport {
    EXECUTABLE,
    PARTIAL,
    CONDITIONAL,
    NOT_IMPLEMENTED
}

enum class VerificationMode {
    ACTION_RESULT,
    UI_OPEN_ONLY,
    EXTERNAL_SIDE_EFFECT,
    NOT_AVAILABLE
}

data class ActionExecutionProfile(
    val support: ExecutionSupport,
    val verification: VerificationMode,
    val requiresRuntimePermission: Boolean = false,
    val notes: String
)

/**
 * Describes what the current Android implementation can actually do.
 * This is intentionally separate from ActionCapabilityRegistry, which describes
 * what a task needs rather than whether its executor is complete.
 */
class ActionExecutionProfileRegistry {
    fun profileFor(intent: AssistantIntent): ActionExecutionProfile = when (intent) {
        AssistantIntent.FlashlightOn,
        AssistantIntent.FlashlightOff ->
            profile(ExecutionSupport.EXECUTABLE, VerificationMode.ACTION_RESULT,
                notes = "Flashlight controller returns whether the requested state was accepted.")

        AssistantIntent.Selfie,
        AssistantIntent.Camera ->
            profile(ExecutionSupport.EXECUTABLE, VerificationMode.UI_OPEN_ONLY,
                notes = "Camera UI is opened; CESI does not verify photo capture.")

        AssistantIntent.Location ->
            profile(ExecutionSupport.EXECUTABLE, VerificationMode.ACTION_RESULT, true,
                "Location can execute after location permission is granted.")

        is AssistantIntent.Call ->
            profile(ExecutionSupport.EXECUTABLE, VerificationMode.EXTERNAL_SIDE_EFFECT, true,
                "Call execution is implemented; phone/contact permissions may be required.")

        is AssistantIntent.Dial ->
            profile(ExecutionSupport.PARTIAL, VerificationMode.UI_OPEN_ONLY, false,
                "CESI opens the dialer; the user still places the call.")

        is AssistantIntent.Ussd ->
            profile(ExecutionSupport.PARTIAL, VerificationMode.UI_OPEN_ONLY, false,
                "CESI currently opens the dialer with the code; it does not run the session.")

        is AssistantIntent.ContactSearch ->
            profile(ExecutionSupport.EXECUTABLE, VerificationMode.ACTION_RESULT, true,
                "Contact search is implemented after contacts permission.")

        is AssistantIntent.AppLaunch ->
            profile(ExecutionSupport.EXECUTABLE, VerificationMode.UI_OPEN_ONLY,
                notes = "CESI resolves and opens a launcher activity.")

        is AssistantIntent.YouTubeSearch,
        is AssistantIntent.WebSearch,
        is AssistantIntent.VisualSearch,
        is AssistantIntent.TopicFollowUp ->
            profile(ExecutionSupport.EXECUTABLE, VerificationMode.UI_OPEN_ONLY,
                notes = "The current search path opens the requested search destination.")

        AssistantIntent.VolumeUp,
        AssistantIntent.VolumeDown,
        AssistantIntent.Mute,
        AssistantIntent.BatteryStatus,
        AssistantIntent.Time,
        AssistantIntent.Date ->
            profile(ExecutionSupport.EXECUTABLE, VerificationMode.ACTION_RESULT,
                notes = "The current local controller or response path is implemented.")

        AssistantIntent.OpenSettings,
        AssistantIntent.WifiSettings,
        AssistantIntent.BluetoothSettings,
        AssistantIntent.SoundSettings,
        AssistantIntent.DisplaySettings,
        AssistantIntent.NotificationSettings ->
            profile(ExecutionSupport.EXECUTABLE, VerificationMode.UI_OPEN_ONLY,
                notes = "CESI opens the requested Android settings surface.")

        is AssistantIntent.SetAlarm ->
            profile(ExecutionSupport.PARTIAL, VerificationMode.UI_OPEN_ONLY,
                notes = "CESI opens the alarm UI; the user still confirms or saves it.")

        is AssistantIntent.Message ->
            profile(ExecutionSupport.PARTIAL, VerificationMode.UI_OPEN_ONLY, true,
                "CESI prepares a WhatsApp message and opens the app; it does not send automatically.")

        is AssistantIntent.MessengerMessage ->
            profile(ExecutionSupport.PARTIAL, VerificationMode.UI_OPEN_ONLY,
                notes = "CESI opens Messenger with the text; the user chooses the chat and sends it.")

        is AssistantIntent.Reply ->
            profile(ExecutionSupport.CONDITIONAL, VerificationMode.EXTERNAL_SIDE_EFFECT,
                notes = "Direct notification reply depends on notification-listener state; otherwise CESI falls back to a draft.")

        is AssistantIntent.Advice ->
            profile(ExecutionSupport.EXECUTABLE, VerificationMode.ACTION_RESULT,
                notes = "Advice is a local response with no Android side effect.")

        AssistantIntent.Unknown ->
            profile(ExecutionSupport.NOT_IMPLEMENTED, VerificationMode.NOT_AVAILABLE,
                notes = "Unknown intents must never be treated as executable.")
    }

    private fun profile(
        support: ExecutionSupport,
        verification: VerificationMode,
        permission: Boolean = false,
        notes: String
    ) = ActionExecutionProfile(support, verification, permission, notes)
}
