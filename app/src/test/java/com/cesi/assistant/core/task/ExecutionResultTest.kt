package com.cesi.assistant.core.task

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionResultTest {

    @Test
    fun successfulExecutionIsExplicit() {
        val result = ExecutionResult(
            status = ExecutionStatus.SUCCESS,
            message = "Action completed."
        )

        assertEquals(ExecutionStatus.SUCCESS, result.status)
        assertFalse(result.retryable)
    }

    @Test
    fun confirmationIsDifferentFromFailure() {
        val result = ExecutionResult(
            status = ExecutionStatus.NEEDS_CONFIRMATION,
            message = "Confirmation required."
        )

        assertEquals(ExecutionStatus.NEEDS_CONFIRMATION, result.status)
    }

    @Test
    fun notCheckedVerificationIsNotSuccess() {
        val result = VerificationResult(
            status = VerificationStatus.NOT_CHECKED,
            message = "No reliable signal was available."
        )

        assertEquals(VerificationStatus.NOT_CHECKED, result.status)
        assertTrue(result.status != VerificationStatus.VERIFIED)
    }
}
