package com.cesi.assistant.core.task

import org.junit.Assert.assertEquals
import org.junit.Test

class FollowUpResolverTest {
    @Test
    fun resolvesEnglishAppReferences() {
        assertEquals(
            "open WhatsApp",
            FollowUpResolver.resolve("open it!", "WhatsApp", null, null)
        )
    }

    @Test
    fun resolvesHausaAppReferences() {
        assertEquals(
            "open Instagram",
            FollowUpResolver.resolve("buɗe shi", "Instagram", null, null)
        )
    }

    @Test
    fun resolvesSearchReferenceToYouTube() {
        assertEquals(
            "search youtube Java basics",
            FollowUpResolver.resolve("find it on YouTube", null, "Java basics", null)
        )
    }

    @Test
    fun resolvesPlainSearchReference() {
        assertEquals(
            "search quantum computing",
            FollowUpResolver.resolve("search for that", null, "quantum computing", null)
        )
    }

    @Test
    fun resolvesContactReference() {
        assertEquals(
            "call Abdul",
            FollowUpResolver.resolve("call him", null, null, "Abdul")
        )
    }

    @Test
    fun leavesAmbiguousFollowUpUnchangedWithoutContext() {
        assertEquals(
            "open it",
            FollowUpResolver.resolve("open it", null, null, null)
        )
    }

    @Test
    fun leavesUnsupportedPhraseUnchangedEvenWithContext() {
        assertEquals(
            "do the same thing",
            FollowUpResolver.resolve("do the same thing", "WhatsApp", "Java", "Abdul")
        )
    }
}
