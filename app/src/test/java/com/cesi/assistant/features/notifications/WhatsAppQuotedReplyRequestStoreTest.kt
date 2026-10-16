package com.cesi.assistant.features.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppQuotedReplyRequestStoreTest {
    private var now = 10_000L
    private val store = WhatsAppQuotedReplyRequestStore(nowMs = { now }, ttlMs = 100L)

    @Test
    fun requestStartsUnapprovedAndCannotAdvanceBeforeConfirmation() {
        assertTrue(store.begin("old message", "my reply"))
        assertFalse(store.current()?.authorized ?: true)
        assertFalse(
            store.advance(
                WhatsAppQuotedReplyStage.FIND_TARGET_MESSAGE,
                WhatsAppQuotedReplyStage.FIND_REPLY_ACTION
            )
        )
    }

    @Test
    fun authorizationAllowsOnlyExpectedStageTransitions() {
        store.begin("old message", "my reply")
        assertTrue(store.authorize())
        assertTrue(
            store.advance(
                WhatsAppQuotedReplyStage.FIND_TARGET_MESSAGE,
                WhatsAppQuotedReplyStage.FIND_REPLY_ACTION
            )
        )
        assertFalse(
            store.advance(
                WhatsAppQuotedReplyStage.FIND_TARGET_MESSAGE,
                WhatsAppQuotedReplyStage.FILL_REPLY
            )
        )
        assertEquals(WhatsAppQuotedReplyStage.FIND_REPLY_ACTION, store.current()?.stage)
    }

    @Test
    fun rejectsBlankTargetOrReply() {
        assertFalse(store.begin(" ", "reply"))
        assertFalse(store.begin("target", " "))
        assertNull(store.current())
    }

    @Test
    fun expiresAtTtlBoundaryAndOnClockRollback() {
        store.begin("old message", "my reply")
        now += 99L
        assertTrue(store.current() != null)
        now += 1L
        assertNull(store.current())

        store.begin("old message", "my reply")
        now -= 1L
        assertNull(store.current())
    }

    @Test
    fun replacementAndClearDiscardPriorReply() {
        store.begin("first old message", "first reply")
        store.begin("second old message", "second reply")
        assertEquals("second old message", store.current()?.targetText)
        assertEquals("second reply", store.current()?.replyText)
        store.clear()
        assertNull(store.current())
    }
}
