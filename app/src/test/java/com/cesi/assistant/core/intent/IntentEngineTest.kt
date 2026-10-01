package com.cesi.assistant.core.intent

import com.cesi.assistant.VoiceWakeService
import org.junit.Assert.assertEquals
import org.junit.Test

class IntentEngineTest {
    private val engine = IntentEngine()

    @Test
    fun naturalLocationPhrase() {
        assertEquals(AssistantIntent.Location, engine.understand("CESI, could you tell me where I am right now?"))
    }

    @Test
    fun naturalBatteryPhrase() {
        assertEquals(AssistantIntent.BatteryStatus, engine.understand("CESI, how much charge do I have left?"))
    }

    @Test
    fun naturalFlashlightPhrase() {
        assertEquals(AssistantIntent.FlashlightOn, engine.understand("CESI, would you please turn on the flashlight?"))
    }

    @Test
    fun naturalVolumePhrase() {
        assertEquals(AssistantIntent.VolumeUp, engine.understand("CESI, make it louder please"))
    }

    @Test
    fun naturalSearchPhrase() {
        assertEquals(
            AssistantIntent.WebSearch("android voice assistant design"),
            engine.understand("CESI, look up android voice assistant design")
        )
    }

    @Test
    fun naturalAppLaunchPhrase() {
        assertEquals(
            AssistantIntent.AppLaunch("youtube"),
            engine.understand("CESI, could you launch YouTube?")
        )
    }

    @Test
    fun naturalCallPhrase() {
        assertEquals(
            AssistantIntent.Call("ahmed"),
            engine.understand("CESI, please call my Ahmed")
        )
    }


    @Test
    fun multiTaskPlannerSplitsIndependentCommands() {
        val engine = com.cesi.assistant.core.task.ContextTaskEngine
        val method = engine::class.java.getDeclaredMethod("splitSteps", String::class.java)
        method.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val steps = method.invoke(engine, "open WhatsApp, turn on flashlight, and tell me my battery") as List<String>

        assertEquals(
            listOf("open WhatsApp", "turn on flashlight", "tell me my battery"),
            steps
        )
    }

    @Test
    fun multiTaskPlannerDoesNotSplitMessageContent() {
        val engine = com.cesi.assistant.core.task.ContextTaskEngine
        val method = engine::class.java.getDeclaredMethod("splitSteps", String::class.java)
        method.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val steps = method.invoke(
            engine,
            "send Ahmed a message saying call me later and bring the charger"
        ) as List<String>

        assertEquals(
            listOf("send Ahmed a message saying call me later and bring the charger"),
            steps
        )
    }

    @Test
    fun wakePhraseCarriesInlineCommand() {
        assertEquals(
            "open WhatsApp",
            VoiceWakeService.extractCommandAfterWake("Hey CESI, open WhatsApp")
        )
    }

    @Test
    fun wakeWordOnlyProducesNoCommand() {
        assertEquals(
            "",
            VoiceWakeService.extractCommandAfterWake("CESI")
        )
    }
}
