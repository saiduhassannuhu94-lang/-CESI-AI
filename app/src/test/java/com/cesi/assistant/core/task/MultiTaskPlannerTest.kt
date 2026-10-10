package com.cesi.assistant.core.task

import org.junit.Assert.assertEquals
import org.junit.Test

class MultiTaskPlannerTest {
    private val planner = MultiTaskPlanner()

    @Test
    fun splitsMultipleIndependentCommandsSeparatedByCommasAndAnd() {
        assertEquals(
            listOf("open WhatsApp", "turn on flashlight", "tell me my battery"),
            planner.split("open WhatsApp, turn on flashlight, and tell me my battery")
        )
    }

    @Test
    fun splitsExplicitEnglishSequencing() {
        assertEquals(
            listOf("open YouTube", "search for Android development", "tell me the time"),
            planner.split("open YouTube and then search for Android development, then tell me the time")
        )
    }

    @Test
    fun splitsHausaSequencing() {
        assertEquals(
            listOf("buɗe WhatsApp", "kunna haske", "rage sauti"),
            planner.split("buɗe WhatsApp sannan kunna haske, sai rage sauti")
        )
    }

    @Test
    fun preservesConnectorsInsideMessageBody() {
        assertEquals(
            listOf("send Ahmed a message saying call me later and bring the charger"),
            planner.split("send Ahmed a message saying call me later and bring the charger")
        )
        assertEquals(
            listOf("send Ahmed a message saying call me later and then bring the charger"),
            planner.split("send Ahmed a message saying call me later and then bring the charger")
        )
    }

    @Test
    fun splitsBeforeMessageButNeverInsideItsExplicitBody() {
        assertEquals(
            listOf(
                "open WhatsApp",
                "send Ahmed a message saying call me later and bring the charger"
            ),
            planner.split("open WhatsApp, send Ahmed a message saying call me later and bring the charger")
        )
    }

    @Test
    fun requiresARecognizableNextCommand() {
        assertEquals(
            listOf("open WhatsApp and my friend is waiting"),
            planner.split("open WhatsApp and my friend is waiting")
        )
        assertEquals(
            listOf("What is CESI and how does it work?"),
            planner.split("What is CESI and how does it work?")
        )
    }

    @Test
    fun handlesWhitespaceAndCapitalization() {
        assertEquals(
            listOf("OPEN WhatsApp", "Turn on flashlight"),
            planner.split("  OPEN WhatsApp,   AND Turn on flashlight  ")
        )
    }

    @Test
    fun handlesCommaThenConnector() {
        assertEquals(
            listOf("open WhatsApp", "turn on flashlight"),
            planner.split("open WhatsApp, then turn on flashlight")
        )
    }

    @Test
    fun blankInputProducesNoTasks() {
        assertEquals(emptyList<String>(), planner.split("   \n\t "))
    }

    @Test
    fun ordinaryAndWithoutCommandStarterStaysTogether() {
        assertEquals(
            listOf("open WhatsApp and make sure the battery lasts"),
            planner.split("open WhatsApp and make sure the battery lasts")
        )
    }
}
