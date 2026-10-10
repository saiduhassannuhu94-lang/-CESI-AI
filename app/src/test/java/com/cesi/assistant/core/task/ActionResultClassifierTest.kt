package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionResultClassifierTest {
    @Test
    fun knownCameraOpenIsSuccessOnlyForCameraIntent() {
        val result = ActionResultClassifier.classify(
            "Na buɗe camera.",
            AssistantIntent.Camera
        )

        assertEquals(ExecutionStatus.SUCCESS, result.status)
        assertFalse(result.retryable)
    }

    @Test
    fun permissionPromptIsBlockedEvenWhenItSaysAnActivityOpened() {
        val result = ActionResultClassifier.classify(
            "Na buɗe permission na Location. Ka danna Allow, sannan ka sake cewa location ɗinka.",
            AssistantIntent.Location
        )

        assertEquals(ExecutionStatus.BLOCKED, result.status)
        assertTrue(result.retryable)
    }

    @Test
    fun positiveLookingButKnownFailureIsNotSuccess() {
        val result = ActionResultClassifier.classify(
            "Na sami WhatsApp, amma Android bai bani damar buɗe shi ba.",
            AssistantIntent.AppLaunch("WhatsApp")
        )

        assertEquals(ExecutionStatus.FAILED, result.status)
        assertTrue(result.retryable)
    }

    @Test
    fun unsupportedReactionCannotBeMisclassifiedAsSuccess() {
        val result = ActionResultClassifier.classify(
            "Na gane kana son reaction 😂, amma wannan ba a aiwatar da shi ba tukuna.",
            AssistantIntent.Reply("react with 😂")
        )

        assertEquals(ExecutionStatus.FAILED, result.status)
    }

    @Test
    fun preparedMessageIsPartialNotSentSuccess() {
        val result = ActionResultClassifier.classify(
            "Na shirya saƙon WhatsApp zuwa Aisha. Ka duba ka tabbatar kafin ka aika.",
            AssistantIntent.Message("Aisha", "Sannu")
        )

        assertEquals(ExecutionStatus.PARTIAL, result.status)
        assertTrue(result.message.contains("Ka duba"))
    }

    @Test
    fun dialerOpeningIsPartialNotCallCompletion() {
        val result = ActionResultClassifier.classify(
            "Na buɗe dialer da lambar 08012345678.",
            AssistantIntent.Dial("08012345678")
        )

        assertEquals(ExecutionStatus.PARTIAL, result.status)
    }

    @Test
    fun unknownOutcomeIsNeverSilentlySuccessful() {
        val result = ActionResultClassifier.classify(
            "Everything is probably fine.",
            AssistantIntent.Camera
        )

        assertEquals(ExecutionStatus.UNKNOWN, result.status)
        assertTrue(result.message.contains("Ban iya tabbatar"))
    }

    @Test
    fun emptyMessageIsFailed() {
        val result = ActionResultClassifier.classify("")
        assertEquals(ExecutionStatus.FAILED, result.status)
        assertTrue(result.retryable)
    }

    @Test
    fun genericUnrecognizedMessageIsUnknown() {
        val result = ActionResultClassifier.classify("Task finished maybe.")
        assertEquals(ExecutionStatus.UNKNOWN, result.status)
    }
}
