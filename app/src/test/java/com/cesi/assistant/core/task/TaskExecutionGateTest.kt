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
    fun outgoingMessageRequiresConfirmationBeforeExecution() {
        val result = gate.inspect(
            listOf(
                AssistantIntent.Message("Aisha", "Zan zo yanzu"),
                AssistantIntent.SetAlarm(8, 0, "class")
            )
        )

        assertTrue(result is TaskExecutionResult.ConfirmationRequired)
        val prompt = (result as TaskExecutionResult.ConfirmationRequired).message
        assertTrue(prompt.contains("Aisha"))
        assertTrue(prompt.contains("Zan zo yanzu"))
    }

    @Test
    fun confirmationDisclosureListsEveryProtectedActionInMultiStepRequest() {
        val result = gate.inspect(
            listOf(
                AssistantIntent.Call("Abdul"),
                AssistantIntent.Ussd("*123#"),
                AssistantIntent.Message("Aisha", "Zan zo yanzu")
            )
        )

        assertTrue(result is TaskExecutionResult.ConfirmationRequired)
        val prompt = (result as TaskExecutionResult.ConfirmationRequired).message
        assertTrue(prompt.contains("Abdul"))
        assertTrue(prompt.contains("*123#"))
        assertTrue(prompt.contains("Aisha"))
        assertTrue(prompt.contains("Zan zo yanzu"))
        assertTrue(prompt.contains("1)"))
        assertTrue(prompt.contains("2)"))
        assertTrue(prompt.contains("3)"))
    }
    @Test
    fun confirmationDisclosureUsesReadableNamesForLowRiskActionsToo() {
        val result = gate.inspect(
            listOf(
                AssistantIntent.AppLaunch("WhatsApp"),
                AssistantIntent.FlashlightOn,
                AssistantIntent.Time,
                AssistantIntent.Call("Abdul")
            )
        )

        assertTrue(result is TaskExecutionResult.ConfirmationRequired)
        val prompt = (result as TaskExecutionResult.ConfirmationRequired).message
        assertTrue(prompt.contains("Buɗe app: WhatsApp"))
        assertTrue(prompt.contains("Kunna flashlight"))
        assertTrue(prompt.contains("Duba lokaci"))
        assertTrue(prompt.contains("Kira Abdul"))
        assertTrue(prompt.contains("[LOW RISK]"))
        assertTrue(prompt.contains("[NEEDS CONFIRMATION]"))
    }

}
