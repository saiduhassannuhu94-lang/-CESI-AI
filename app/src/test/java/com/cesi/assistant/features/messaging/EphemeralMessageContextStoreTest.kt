package com.cesi.assistant.features.messaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EphemeralMessageContextStoreTest {
    @Test
    fun remembersTrimmedContextUntilTtlExpires() {
        var now = 1_000L
        val store = EphemeralMessageContextStore(nowMs = { now }, ttlMs = 100L)

        store.remember("  Saidu  ", "  Hello there  ")

        assertEquals("Saidu", store.latest()?.sender)
        assertEquals("Hello there", store.latest()?.message)
        now = 1_099L
        assertEquals("Hello there", store.latest()?.message)
        now = 1_100L
        assertNull(store.latest())
        assertNull(store.latest())
    }

    @Test
    fun newerNotificationReplacesPreviousPrivateContext() {
        var now = 2_000L
        val store = EphemeralMessageContextStore(nowMs = { now }, ttlMs = 100L)

        store.remember("Contact A", "private A")
        now += 1L
        store.remember("Contact B", "private B")

        assertEquals("Contact B", store.latest()?.sender)
        assertEquals("private B", store.latest()?.message)
    }

    @Test
    fun blankContextClearsPreviousMessage() {
        val store = EphemeralMessageContextStore(nowMs = { 1_000L }, ttlMs = 100L)
        store.remember("Contact A", "private A")
        store.remember(" ", " ")

        assertNull(store.latest())
    }

    @Test
    fun explicitClearDropsContext() {
        val store = EphemeralMessageContextStore(nowMs = { 1_000L }, ttlMs = 100L)
        store.remember("Contact A", "private A")
        store.clear()

        assertNull(store.latest())
    }

    @Test
    fun clockMovingBackwardsExpiresContextSafely() {
        var now = 1_000L
        val store = EphemeralMessageContextStore(nowMs = { now }, ttlMs = 100L)
        store.remember("Contact A", "private A")
        now = 999L

        assertNull(store.latest())
    }
}
