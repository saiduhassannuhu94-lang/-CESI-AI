package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent

/**
 * Lightweight pre-execution planner.
 *
 * It keeps the current voice and permission flow untouched while giving CESI
 * a structured plan that can later be consumed by UI, confirmation, and
 * verification layers.
 */
data class PlannedAction(
    val intent: AssistantIntent,
    val capabilities: Set<ActionCapability>,
    val requiresConfirmation: Boolean
)

class TaskPlanner(
    private val registry: ActionCapabilityRegistry = ActionCapabilityRegistry()
) {
    fun plan(intents: List<AssistantIntent>): List<PlannedAction> =
        intents.map { intent ->
            PlannedAction(
                intent = intent,
                capabilities = registry.capabilitiesFor(intent),
                requiresConfirmation = requiresConfirmation(intent)
            )
        }

    private fun requiresConfirmation(intent: AssistantIntent): Boolean =
        intent is AssistantIntent.Call ||
        intent is AssistantIntent.Dial ||
        intent is AssistantIntent.Ussd ||
        intent is AssistantIntent.Message ||
        intent is AssistantIntent.SetAlarm
}
