package com.cesi.assistant.features.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppReactionAccessibilityMatcherTest {
    @Test
    fun mapsCommonWhatsAppAccessibleReactionLabels() {
        assertEquals("😂", WhatsAppReactionAccessibilityMatcher.canonicalReaction("Laughing Face"))
        assertEquals("😂", WhatsAppReactionAccessibilityMatcher.canonicalReaction("React with Face with Tears of Joy"))
        assertEquals("❤️", WhatsAppReactionAccessibilityMatcher.canonicalReaction("React with Heart"))
        assertEquals("👍", WhatsAppReactionAccessibilityMatcher.canonicalReaction("Thumbs up"))
        assertEquals("😢", WhatsAppReactionAccessibilityMatcher.canonicalReaction("Crying Face"))
        assertEquals("🙏", WhatsAppReactionAccessibilityMatcher.canonicalReaction("Thanks"))
    }

    @Test
    fun matchesRequestedEmojiByAccessibleLabelOrLiteralGlyph() {
        assertTrue(WhatsAppReactionAccessibilityMatcher.matchesRequestedReaction("React with Laughing", "😂"))
        assertTrue(WhatsAppReactionAccessibilityMatcher.matchesRequestedReaction("❤️", "❤️"))
        assertFalse(WhatsAppReactionAccessibilityMatcher.matchesRequestedReaction("Like", "😂"))
    }

    @Test
    fun preservesCustomEmojiWhenAccessibleLabelIsLiteralEmoji() {
        assertTrue(WhatsAppReactionAccessibilityMatcher.matchesRequestedReaction("🫶", "🫶"))
        assertEquals(null, WhatsAppReactionAccessibilityMatcher.canonicalReaction("🫶"))
    }

    @Test
    fun normalizesWhitespaceAndUnicodeBeforeMatchingMessageText() {
        assertTrue(
            WhatsAppReactionAccessibilityMatcher.matchesMessageText(
                "Sannu\nina kwana?",
                "  Sannu   ina kwana?  "
            )
        )
        assertFalse(
            WhatsAppReactionAccessibilityMatcher.matchesMessageText(
                "Sannu",
                "Sannu ina kwana?"
            )
        )
        assertFalse(
            WhatsAppReactionAccessibilityMatcher.matchesMessageText(
                "   ",
                "   "
            )
        )
    }
}
