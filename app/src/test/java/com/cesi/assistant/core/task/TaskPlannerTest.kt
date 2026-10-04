package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskPlannerTest {
    private val planner = TaskPlanner()

    @Test
    fun marksExternalSideEffectsAsConfirmationRequired() {
        val plan = planner.plan(
            listOf(
                AssistantIntent.Reply("I'll call later"),
                AssistantIntent.Call("Ahmed")
            )
        )

        assertEquals(2, plan.size)
        assertEquals(TaskRisk.EXTERNAL_SIDE_EFFECT, plan[0].risk)
        assertEquals(TaskRisk.EXTERNAL_SIDE_EFFECT, plan[1].risk)
        assertTrue(plan[0].requiresConfirmation)
        assertTrue(plan[1].requiresConfirmation)
    }

    @Test
    fun keepsReadOnlyActionsWithoutConfirmation() {
        val plan = planner.plan(
            listOf(
                AssistantIntent.BatteryStatus,
                AssistantIntent.Time,
                AssistantIntent.FlashlightOn
            )
        )

        assertEquals(TaskRisk.NONE, plan[0].risk)
        assertEquals(TaskRisk.NONE, plan[1].risk)
        assertEquals(TaskRisk.NONE, plan[2].risk)
        assertTrue(plan.none { it.requiresConfirmation })
    }

    @Test
    fun alarmIsLowRiskButStillConfirmationRequired() {
        val plan = planner.plan(
            listOf(AssistantIntent.SetAlarm(8, 0, "class"))
        )

        assertEquals(TaskRisk.LOW, plan.single().risk)
        assertTrue(plan.single().requiresConfirmation)
    }
}
