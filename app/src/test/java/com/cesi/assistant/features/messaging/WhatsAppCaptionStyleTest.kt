package com.cesi.assistant.features.messaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppCaptionStyleTest {
    @Test
    fun recognizesSupportedCaptionStylesInEnglishAndHausa() {
        assertEquals(WhatsAppCaptionStyle.FUNNY, WhatsAppCaptionStyle.fromPhrase("barkwanci"))
        assertEquals(WhatsAppCaptionStyle.PROFESSIONAL, WhatsAppCaptionStyle.fromPhrase("professional"))
        assertEquals(WhatsAppCaptionStyle.ISLAMIC, WhatsAppCaptionStyle.fromPhrase("na musulunci"))
        assertEquals(WhatsAppCaptionStyle.ROMANTIC, WhatsAppCaptionStyle.fromPhrase("soyayya"))
        assertEquals(WhatsAppCaptionStyle.SHORT, WhatsAppCaptionStyle.fromPhrase("gajere"))
        assertNull(WhatsAppCaptionStyle.fromPhrase("random mystery style"))
    }

    @Test
    fun createsDistinctCaptionsForMediaAndStatus() {
        assertTrue(WhatsAppCaptionStyle.FUNNY.captionFor("image", true).contains("😂"))
        assertTrue(WhatsAppCaptionStyle.PROFESSIONAL.captionFor("video", false).contains("bidiyon"))
        assertTrue(WhatsAppCaptionStyle.ISLAMIC.captionFor("image", true).contains("Alhamdulillah"))
        assertTrue(WhatsAppCaptionStyle.SHORT.captionFor("video", false).length < 30)
    }
}
