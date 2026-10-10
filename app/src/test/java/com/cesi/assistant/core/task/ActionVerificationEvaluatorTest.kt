package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionVerificationEvaluatorTest {
    private val evaluator = ActionVerificationEvaluator()

    @Test
    fun executorResultCanBeVerifiedWithoutClaimingIndependentHardwareReadback() {
        val result = evaluator.evaluate(
            AssistantIntent.FlashlightOn,
            ExecutionResult(ExecutionStatus.SUCCESS, "Na kunna haske.")
        )

        assertEquals(VerificationStatus.VERIFIED, result.status)
        assertTrue(result.message.contains("not independent device-state readback"))
    }

    @Test
    fun launchingAnAppOnlyVerifiesThatTheUiWasOpened() {
        val result = evaluator.evaluate(
            AssistantIntent.AppLaunch("WhatsApp"),
            ExecutionResult(ExecutionStatus.SUCCESS, "Na buɗe WhatsApp.")
        )

        assertEquals(VerificationStatus.SURFACE_OPENED, result.status)
        assertTrue(result.message.contains("not independently checked"))
    }

    @Test
    fun externalCallOutcomeRemainsUnchecked() {
        val result = evaluator.evaluate(
            AssistantIntent.Call("Aisha"),
            ExecutionResult(ExecutionStatus.SUCCESS, "Ina kira Aisha.")
        )

        assertEquals(VerificationStatus.NOT_CHECKED, result.status)
        assertTrue(result.message.contains("final outcome"))
    }

    @Test
    fun preparedMessageRemainsPartial() {
        val result = evaluator.evaluate(
            AssistantIntent.Message("Aisha", "Sannu"),
            ExecutionResult(ExecutionStatus.PARTIAL, "Na shirya saƙon WhatsApp.")
        )

        assertEquals(VerificationStatus.PARTIAL, result.status)
    }

    @Test
    fun unknownOutcomeCannotBecomeVerified() {
        val result = evaluator.evaluate(
            AssistantIntent.FlashlightOn,
            ExecutionResult(ExecutionStatus.UNKNOWN, "Sakamako marar tabbas.")
        )

        assertEquals(VerificationStatus.NOT_CHECKED, result.status)
    }

    @Test
    fun blockedPermissionIsNotReportedAsFailedPostActionVerification() {
        val result = evaluator.evaluate(
            AssistantIntent.Location,
            ExecutionResult(ExecutionStatus.BLOCKED, "Na buɗe permission.")
        )

        assertEquals(VerificationStatus.NOT_CHECKED, result.status)
    }

    @Test
    fun volumeResultIsNotVerifiedWithoutReadback() {
        val result = evaluator.evaluate(
            AssistantIntent.VolumeUp,
            ExecutionResult(ExecutionStatus.SUCCESS, "Na ƙara sauti.")
        )

        assertEquals(VerificationStatus.NOT_CHECKED, result.status)
    }

    @Test
    fun explicitExecutorFailureIsFailed() {
        val result = evaluator.evaluate(
            AssistantIntent.Camera,
            ExecutionResult(ExecutionStatus.FAILED, "Ban iya buɗe camera ba.")
        )

        assertEquals(VerificationStatus.FAILED, result.status)
    }
}
