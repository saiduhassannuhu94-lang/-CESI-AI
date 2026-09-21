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
 */
class TaskExecutionGate(private val planner: TaskPlanner = TaskPlanner()) {
    fun inspect(intents: List<AssistantIntent>): TaskExecutionResult {
        if (intents.isEmpty()) return TaskExecutionResult.Failed("Ban ji umarnin ba.")
        val plan = planner.plan(intents)
        val confirmation = plan.firstOrNull { it.requiresConfirmation }
        return if (confirmation != null) {
            TaskExecutionResult.ConfirmationRequired(
                action = confirmation,
                message = confirmationMessage(confirmation.intent)
            )
        } else {
            TaskExecutionResult.Completed("An shirya aikin.")
        }
    }

    private fun confirmationMessage(intent: AssistantIntent): String = when (intent) {
        is AssistantIntent.Call -> "Ina shirin kiran ${'$'}{intent.target}. Ka tabbatar?"
        is AssistantIntent.Dial -> "Ina shirin buɗe dialer da ${'$'}{intent.number}. Ka tabbatar?"
        is AssistantIntent.Ussd -> "Ina shirin buɗe dialer da ${'$'}{intent.code}. Ka duba lambar kafin ka ci gaba?"
        is AssistantIntent.Message -> "Ina shirin buɗe WhatsApp zuwa ${'$'}{intent.target} da saƙon. Ka tabbatar kafin aika?"
        is AssistantIntent.SetAlarm -> "Ina shirin buɗe alarm na %02d:%02d. Ka tabbatar kafin ka ajiye shi?".format(intent.hour, intent.minute)
        else -> "Wannan aikin yana bukatar tabbatarwa."
    }
}