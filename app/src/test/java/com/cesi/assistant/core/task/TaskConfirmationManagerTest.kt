package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskConfirmationManagerTest {
    @Test
    fun externalSideEffectCreatesPendingConfirmation() {
        var now = 1_000L
        val manager = TaskConfirmationManager(nowMs = { now })

        val result = manager.prepare(listOf(AssistantIntent.Call("Abdul")))

        assertTrue(result is TaskExecutionResult.ConfirmationRequired)
        assertTrue(manager.hasPending())
    }

    @Test
    fun confirmationExecutesPendingIntentOnlyOnce() {
        val manager = TaskConfirmationManager(nowMs = { 1_000L })
        manager.prepare(listOf(AssistantIntent.Message("Abdul", "Zan zo bayan class")))

        val confirmed = manager.resolve("eh")

        assertEquals(
            listOf(AssistantIntent.Message("Abdul", "Zan zo bayan class")),
            (confirmed as ConfirmationResolution.Confirmed).intents
        )
        assertFalse(manager.hasPending())
        assertTrue(manager.resolve("eh") is ConfirmationResolution.NoPending)
    }

    @Test
    fun declineClearsPendingConfirmationWithoutExecution() {
        val manager = TaskConfirmationManager(nowMs = { 1_000L })
        manager.prepare(listOf(AssistantIntent.Call("Abdul")))

        assertTrue(manager.resolve("a'a") is ConfirmationResolution.Declined)
        assertFalse(manager.hasPending())
    }

    @Test
    fun unclearAnswerKeepsConfirmationPending() {
        val manager = TaskConfirmationManager(nowMs = { 1_000L })
        val result = manager.prepare(listOf(AssistantIntent.Call("Abdul")))

        val prompt = (result as TaskExecutionResult.ConfirmationRequired).message
        val resolution = manager.resolve("ban sani ba")

        assertEquals(ConfirmationResolution.StillPending(prompt), resolution)
        assertTrue(manager.hasPending())
    }

    @Test
    fun expiredConfirmationCannotExecute() {
        var now = 1_000L
        val manager = TaskConfirmationManager(nowMs = { now })
        manager.prepare(listOf(AssistantIntent.Call("Abdul")))

        now += TaskConfirmationManager.DEFAULT_TTL_MS + 1

        assertTrue(manager.resolve("eh") is ConfirmationResolution.Expired)
        assertFalse(manager.hasPending())
    }

    @Test
    fun cancelClearsPendingConfirmation() {
        val manager = TaskConfirmationManager(nowMs = { 1_000L })
        manager.prepare(listOf(AssistantIntent.Call("Abdul")))

        assertTrue(manager.resolve("cancel") is ConfirmationResolution.Cancelled)
        assertFalse(manager.hasPending())
    }
}
