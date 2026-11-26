package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent

enum class TaskRisk {
    NONE,
    LOW,
    EXTERNAL_SIDE_EFFECT,
    HIGH_IMPACT
}

/**
 * Central risk policy. Risk is derived from the local intent, never trusted
 * from AI-generated metadata. Actions that communicate externally require
 * confirmation; USSD is high impact because carrier codes may trigger
 * subscriptions, purchases, or account changes.
 */
object TaskRiskPolicy {
    fun riskFor(intent: AssistantIntent): TaskRisk = when (intent) {
        is AssistantIntent.Call,
        is AssistantIntent.Message,
        is AssistantIntent.MessengerMessage,
        is AssistantIntent.Reply -> TaskRisk.EXTERNAL_SIDE_EFFECT

        is AssistantIntent.Ussd -> TaskRisk.HIGH_IMPACT

        is AssistantIntent.Dial,
        is AssistantIntent.SetAlarm -> TaskRisk.LOW

        else -> TaskRisk.NONE
    }
}
