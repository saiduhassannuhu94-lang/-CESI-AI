package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskExecutionGateTest {
    private val gate = TaskExecutionGate()

    @Test
    fun lowRiskMultiActionPlanDoesNotRequireConfirmation() {
        val result = gate.inspect(
            listOf(
                AssistantIntent.AppLaunch("WhatsApp"),
                AssistantIntent.FlashlightOn,
                AssistantIntent.Time
            )
        )

        assertTrue(result is TaskExecutionResult.Completed)
    }

    @Test
    fun multiActionPlanRequiresConfirmationForActualExternalSideEffect() {
        val result = gate.inspect(
            listOf(
                AssistantIntent.AppLaunch("WhatsApp"),
                AssistantIntent.FlashlightOn,
                AssistantIntent.Call("Abdul")
            )
        )

        assertTrue(result is TaskExecutionResult.ConfirmationRequired)
        assertTrue((result as TaskExecutionResult.ConfirmationRequired).message.contains("Abdul"))
    }

    @Test
    fun draftAndAlarmUiActionsRemainUserControlledWithoutConfirmation() {
        val result = gate.inspect(
            listOf(
                AssistantIntent.Message("Aisha", "Zan zo yanzu"),
                AssistantIntent.SetAlarm(8, 0, "class")
            )
        )

        assertTrue(result is TaskExecutionResult.Completed)
    }
}
