package com.cesi.assistant.core.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class CesiSpeechFormatterTest {
    @Test
    fun emptyTextRemainsEmpty() {
        assertEquals("", CesiSpeechFormatter.format("   "))
    }

    @Test
    fun removesMarkdownHeadingsAndBulletMarkersButAddsPause() {
        assertEquals("Overview. First point. Second point", CesiSpeechFormatter.format("# Overview\n- First point\n- Second point"))
    }

    @Test
    fun markdownLinksSpeakOnlyTheirLabel() {
        assertEquals("Open settings", CesiSpeechFormatter.format("[Open settings](https://example.com/settings)"))
    }

    @Test
    fun urlIsNotReadCharacterByCharacter() {
        assertEquals("Visit link", CesiSpeechFormatter.format("Visit https://example.com now"))
    }

    @Test
    fun codeBlocksAreOmittedFromSpokenResponse() {
        assertEquals("Here is code omitted. Then continue", CesiSpeechFormatter.format("Here is:\n\u0060\u0060\u0060kotlin\nval x = 1\n\u0060\u0060\u0060\nThen continue"))
    }

    @Test
    fun rateIsClampedToSupportedRange() {
        assertEquals(CesiSpeechRate.MIN, CesiSpeechRate.clamp(0.1f))
        assertEquals(CesiSpeechRate.MAX, CesiSpeechRate.clamp(2.0f))
        assertEquals(0.9f, CesiSpeechRate.clamp(0.9f))
    }
}
