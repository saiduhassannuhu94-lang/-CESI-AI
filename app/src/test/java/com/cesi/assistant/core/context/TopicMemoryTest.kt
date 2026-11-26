package com.cesi.assistant.core.context

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicMemoryTest {
    @Test
    fun freshTopicIsAccepted() {
        assertTrue(TopicMemory.isFresh("Java", 1_000L, 1_500L))
    }

    @Test
    fun topicAtExpiryBoundaryIsAccepted() {
        assertTrue(TopicMemory.isFresh("Java", 1_000L, 1_000L + TopicMemory.MAX_AGE_MILLIS))
    }

    @Test
    fun topicOlderThanExpiryIsRejected() {
        assertFalse(TopicMemory.isFresh("Java", 1_000L, 1_001L + TopicMemory.MAX_AGE_MILLIS))
    }

    @Test
    fun blankTopicIsRejected() {
        assertFalse(TopicMemory.isFresh("   ", 1_000L, 1_500L))
    }

    @Test
    fun missingTimestampIsRejected() {
        assertFalse(TopicMemory.isFresh("Java", -1L, 1_500L))
    }

    @Test
    fun futureTimestampIsRejected() {
        assertFalse(TopicMemory.isFresh("Java", 2_000L, 1_500L))
    }
}
