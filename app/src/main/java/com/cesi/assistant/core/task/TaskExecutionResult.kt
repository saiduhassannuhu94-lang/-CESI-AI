package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent

sealed class TaskExecutionResult {
    data class Completed(val message: String) : TaskExecutionResult()
    data class ConfirmationRequired(
        val action: PlannedAction,
        val message: String
    ) : TaskExecutionResult()
    data class Failed(val message: String) : TaskExecutionResult()
}

/**
 * Turns planned actions into a safe execution boundary.
 * Confirmation-required actions are surfaced to the UI instead of being
 * executed automatically. Existing voice/permission flow remains untouched.
 *
 * A single confirmation must disclose every side-effect/high-impact action
 * in a multi-step request, not just the first one. The plan is then executed
 * as a whole only after explicit confirmation.
 */
class TaskExecutionGate(private val planner: TaskPlanner = TaskPlanner()) {
    fun inspect(intents: List<AssistantIntent>): TaskExecutionResult {
        if (intents.isEmpty()) return TaskExecutionResult.Failed("Ban ji umarnin ba.")
        val plan = planner.plan(intents)
        val confirmationActions = plan.filter { it.requiresConfirmation }
        val firstConfirmation = confirmationActions.firstOrNull()
        return if (firstConfirmation != null) {
            TaskExecutionResult.ConfirmationRequired(
                action = firstConfirmation,
                message = confirmationMessage(plan)
            )
        } else {
            TaskExecutionResult.Completed("An shirya aikin.")
        }
    }

    private fun confirmationMessage(actions: List<PlannedAction>): String {
        val disclosure = actions.mapIndexed { index, action ->
            val marker = if (action.requiresConfirmation) "[NEEDS CONFIRMATION]" else "[LOW RISK]"
            "${index + 1}) $marker ${describe(action.intent)}"
        }.joinToString("\n")
        return "Wannan aikin yana bukatar tabbatarwa. Ga cikakken jerin matakan CESI:\n" +
            disclosure + "\nKa tabbatar da wannan shirin?"
    }
    private fun describe(intent: AssistantIntent): String = when (intent) {
        is AssistantIntent.Call -> "Kira ${intent.target}"
        is AssistantIntent.Dial -> "Buɗe dialer da ${intent.number}"
        is AssistantIntent.Ussd -> "Buɗe dialer da USSD ${intent.code} (high impact)"
        is AssistantIntent.Message -> "Shirya WhatsApp zuwa ${intent.target}: “${intent.text}” (ba a aika kai tsaye)"
        is AssistantIntent.MessengerMessage -> "Shirya saƙo zuwa ${intent.target}: “${intent.text}”"
        is AssistantIntent.Reply -> "Amsa da: “${intent.text}”"
        else -> "Aikin ${intent.javaClass.simpleName}"
    }
}
