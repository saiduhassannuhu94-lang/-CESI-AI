package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent

enum class TaskRisk {
    NONE,
    LOW,
    EXTERNAL_SIDE_EFFECT,
    HIGH_IMPACT
}

/**
 * Central risk policy. The planner can use this independently from Android
 * execution so risky actions are never hidden inside individual controllers.
 */
object TaskRiskPolicy {
    fun riskFor(intent: AssistantIntent): TaskRisk = when (intent) {
        is AssistantIntent.Call,
        is AssistantIntent.Dial,
        is AssistantIntent.Ussd,
        is AssistantIntent.Message,
        is AssistantIntent.MessengerMessage,
        is AssistantIntent.Reply -> TaskRisk.EXTERNAL_SIDE_EFFECT

        is AssistantIntent.SetAlarm -> TaskRisk.LOW

        else -> TaskRisk.NONE
    }
}
