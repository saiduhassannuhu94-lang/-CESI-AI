package com.cesi.assistant.core.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionResultClassifierTest {
    @Test
    fun successfulRouterMessageBecomesSuccess() {
        val result = ActionResultClassifier.classify("Na buɗe camera.")
        assertEquals(ExecutionStatus.SUCCESS, result.status)
        assertFalse(result.retryable)
    }

    @Test
    fun permissionMessageBecomesBlocked() {
        val result = ActionResultClassifier.classify(
            "Na buɗe permission na Location. Ka danna Allow, sannan ka sake cewa location ɗinka."
        )
        assertEquals(ExecutionStatus.BLOCKED, result.status)
        assertTrue(result.retryable)
    }

    @Test
    fun executorFailureBecomesFailed() {
        val result = ActionResultClassifier.classify("Ban iya buɗe camera ba.")
        assertEquals(ExecutionStatus.FAILED, result.status)
        assertTrue(result.retryable)
    }

    @Test
    fun emptyMessageIsFailed() {
        val result = ActionResultClassifier.classify("")
        assertEquals(ExecutionStatus.FAILED, result.status)
        assertTrue(result.retryable)
    }
}
