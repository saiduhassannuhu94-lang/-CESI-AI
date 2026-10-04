package com.cesi.assistant.core.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestUnderstandingTest {
    private val engine = RequestUnderstandingEngine()

    @Test
    fun classifiesHausaEnglishDeviceRequest() {
        val result = engine.understand(
            AssistantRequest(
                rawText = "ka rage brightness zuwa 20%",
                normalizedText = "ka rage brightness zuwa 20%"
            )
        )

        assertEquals(AssistantGoal.DEVICE_CONTROL, result.goal)
        assertTrue(result.confidence > 0.8f)
    }

    @Test
    fun classifiesCommunicationRequest() {
        val result = engine.understand(
            AssistantRequest(
                rawText = "tura wa Abdul cewa zan zo bayan class",
                normalizedText = "tura wa Abdul cewa zan zo bayan class"
            )
        )

        assertEquals(AssistantGoal.COMMUNICATION, result.goal)
    }

    @Test
    fun doesNotGuessUnknownRequest() {
        val result = engine.understand(
            AssistantRequest(
                rawText = "something completely unsupported",
                normalizedText = "something completely unsupported"
            )
        )

        assertEquals(AssistantGoal.UNKNOWN, result.goal)
        assertEquals(0f, result.confidence)
    }
}
