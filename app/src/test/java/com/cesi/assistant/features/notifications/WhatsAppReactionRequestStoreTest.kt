package com.cesi.assistant.features.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppReactionRequestStoreTest {
    private var now = 1_000L
    private val store = WhatsAppReactionRequestStore(nowMs = { now }, ttlMs = 30L)

    @Test
    fun startsWithExactMessageAndEmoji() {
        assertTrue(store.begin("  hello  ", " 😂 "))
        val request = store.current()
        assertEquals("hello", request?.message)
        assertEquals("😂", request?.emoji)
        assertEquals(WhatsAppReactionStage.FIND_MESSAGE, request?.stage)
    }

    @Test
    fun refusesBlankTargetOrEmoji() {
        assertFalse(store.begin("  ", "😂"))
        assertFalse(store.begin("hello", " "))
        assertNull(store.current())
    }

    @Test
    fun stageMustMatchExpectedStage() {
        store.begin("hello", "😂")
        assertFalse(store.advance(WhatsAppReactionStage.FIND_REACTION, WhatsAppReactionStage.FIND_MESSAGE))
        assertTrue(store.advance(WhatsAppReactionStage.FIND_MESSAGE, WhatsAppReactionStage.FIND_REACTION))
        assertEquals(WhatsAppReactionStage.FIND_REACTION, store.current()?.stage)
    }

    @Test
    fun requestExpiresAtTtlBoundary() {
        store.begin("hello", "😂")
        now += 29L
        assertTrue(store.current() != null)
        now += 1L
        assertNull(store.current())
    }

    @Test
    fun requestExpiresWhenMonotonicClockMovesBackwards() {
        store.begin("hello", "😂")
        now -= 1L
        assertNull(store.current())
    }

    @Test
    fun replacingRequestDropsOldTarget() {
        store.begin("first message", "😂")
        store.begin("second message", "❤️")
        assertEquals("second message", store.current()?.message)
        assertEquals("❤️", store.current()?.emoji)
    }

    @Test
    fun clearRemovesPendingRequest() {
        store.begin("hello", "😂")
        store.clear()
        assertNull(store.current())
    }
}
