package com.cesi.assistant.core.ai

import com.cesi.assistant.core.intent.AssistantIntent
import com.cesi.assistant.core.intent.VideoPlatform
import com.cesi.assistant.core.task.ActionType
import com.cesi.assistant.core.task.PlannedAction
import com.cesi.assistant.core.task.StructuredActionMapper
import com.cesi.assistant.core.task.TaskPlan
import com.cesi.assistant.core.task.TaskPlanner

/**
 * Validates AI action proposals against CESI's locally defined action contract.
 * This creates a plan only. It does not invoke Android actions or bypass
 * permissions, confirmations, or execution verification.
 */
sealed interface AstraProposalValidation {
    data class Accepted(val plan: TaskPlan) : AstraProposalValidation
    data class Rejected(val reason: String) : AstraProposalValidation
}

class AstraProposalValidator(
    private val taskPlanner: TaskPlanner = TaskPlanner(),
    private val minimumConfidence: Float = 0.60f,
    private val maximumActions: Int = 5
) {
    fun validate(result: AstraResult.ActionProposal): AstraProposalValidation {
        if (!result.confidence.isFinite() || result.confidence !in 0f..1f) {
            return AstraProposalValidation.Rejected("Invalid confidence value.")
        }
        if (result.confidence < minimumConfidence) {
            return AstraProposalValidation.Rejected("Confidence is too low; clarification is required.")
        }
        if (result.proposals.isEmpty()) {
            return AstraProposalValidation.Rejected("The AI returned an empty action plan.")
        }
        if (result.proposals.size > maximumActions) {
            return AstraProposalValidation.Rejected("The AI proposed too many actions in one request.")
        }

        val intents = mutableListOf<AssistantIntent>()
        for (proposal in result.proposals) {
            val intent = convert(proposal) ?: return AstraProposalValidation.Rejected(
                "Unsupported action or invalid parameters: ${proposal.action}"
            )
            val locallyPlanned = taskPlanner.plan(listOf(intent)).single()
            // The AI-provided risk value is deliberately ignored. CESI calculates risk.
            if (locallyPlanned.action != StructuredActionMapper.from(intent)) {
                return AstraProposalValidation.Rejected("Local action mapping mismatch.")
            }
            intents += intent
        }

        return AstraProposalValidation.Accepted(taskPlanner.planTask(intents))
    }

    private fun convert(proposal: AstraActionProposal): AssistantIntent? {
        val p = proposal.parameters
        fun exact(vararg keys: String): Boolean = p.keys == keys.toSet()
        fun required(key: String): String? = p[key]?.trim()?.takeIf { it.isNotEmpty() }
        fun noParams(): Boolean = p.isEmpty()

        return when (proposal.action) {
            ActionType.TURN_FLASHLIGHT_ON -> if (noParams()) AssistantIntent.FlashlightOn else null
            ActionType.TURN_FLASHLIGHT_OFF -> if (noParams()) AssistantIntent.FlashlightOff else null
            ActionType.OPEN_CAMERA -> if (noParams()) AssistantIntent.Camera else null
            ActionType.TAKE_SELFIE -> if (noParams()) AssistantIntent.Selfie else null
            ActionType.GET_LOCATION -> if (noParams()) AssistantIntent.Location else null
            ActionType.CALL_CONTACT -> if (exact("target")) required("target")?.let(AssistantIntent::Call) else null
            ActionType.OPEN_DIALER -> if (exact("number")) required("number")?.let(AssistantIntent::Dial) else null
            ActionType.RUN_USSD -> if (exact("code")) required("code")?.let(AssistantIntent::Ussd) else null
            ActionType.SEARCH_CONTACT -> if (exact("query")) required("query")?.let(AssistantIntent::ContactSearch) else null
            ActionType.OPEN_APP -> if (exact("app")) required("app")?.let(AssistantIntent::AppLaunch) else null
            ActionType.SEARCH_YOUTUBE -> if (exact("query")) required("query")?.let(AssistantIntent::YouTubeSearch) else null
            ActionType.SEARCH_AND_PLAY_MEDIA -> {
                if (!exact("platform", "query", "requestPlayback")) null
                else {
                    val platform = when (required("platform")?.uppercase()) {
                        "YOUTUBE" -> VideoPlatform.YOUTUBE
                        "TIKTOK" -> VideoPlatform.TIKTOK
                        else -> null
                    }
                    val query = required("query")
                    val play = required("requestPlayback")?.toBooleanStrictOrNull()
                    if (platform == null || query == null || play == null) null
                    else AssistantIntent.MediaSearchAndPlay(platform, query, play)
                }
            }
            ActionType.VOLUME_UP -> if (noParams()) AssistantIntent.VolumeUp else null
            ActionType.VOLUME_DOWN -> if (noParams()) AssistantIntent.VolumeDown else null
            ActionType.MUTE_AUDIO -> if (noParams()) AssistantIntent.Mute else null
            ActionType.GET_BATTERY_STATUS -> if (noParams()) AssistantIntent.BatteryStatus else null
            ActionType.GET_TIME -> if (noParams()) AssistantIntent.Time else null
            ActionType.GET_DATE -> if (noParams()) AssistantIntent.Date else null
            ActionType.OPEN_SETTINGS -> if (noParams()) AssistantIntent.OpenSettings else null
            ActionType.OPEN_WIFI_SETTINGS -> if (noParams()) AssistantIntent.WifiSettings else null
            ActionType.OPEN_BLUETOOTH_SETTINGS -> if (noParams()) AssistantIntent.BluetoothSettings else null
            ActionType.OPEN_SOUND_SETTINGS -> if (noParams()) AssistantIntent.SoundSettings else null
            ActionType.OPEN_DISPLAY_SETTINGS -> if (noParams()) AssistantIntent.DisplaySettings else null
            ActionType.OPEN_NOTIFICATION_SETTINGS -> if (noParams()) AssistantIntent.NotificationSettings else null
            ActionType.SET_ALARM -> {
                if (!(p.keys == setOf("hour", "minute") || p.keys == setOf("hour", "minute", "label"))) null
                else {
                    val hour = required("hour")?.toIntOrNull()
                    val minute = required("minute")?.toIntOrNull()
                    val label = p["label"]?.trim()?.takeIf { it.isNotEmpty() }
                    if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) null
                    else AssistantIntent.SetAlarm(hour, minute, label)
                }
            }
            ActionType.WEB_SEARCH -> if (exact("query")) required("query")?.let(AssistantIntent::WebSearch) else null
            ActionType.VISUAL_SEARCH -> if (exact("query")) required("query")?.let(AssistantIntent::VisualSearch) else null
            ActionType.SEND_MESSAGE -> if (exact("target", "text")) {
                val target = required("target")
                val text = required("text")
                if (target == null || text == null) null else AssistantIntent.Message(target, text)
            } else null
            ActionType.SEND_MESSENGER_MESSAGE -> if (exact("target", "text")) {
                val target = required("target")
                val text = required("text")
                if (target == null || text == null) null else AssistantIntent.MessengerMessage(target, text)
            } else null
            ActionType.REPLY_TO_MESSAGE -> if (exact("text")) required("text")?.let(AssistantIntent::Reply) else null
            ActionType.FOLLOW_UP_TOPIC -> if (exact("text")) required("text")?.let(AssistantIntent::TopicFollowUp) else null
            ActionType.GIVE_ADVICE -> if (exact("situation")) required("situation")?.let(AssistantIntent::Advice) else null
            ActionType.UNKNOWN -> null
        }
    }
}
