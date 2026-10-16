package com.cesi.assistant.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceInputLanguagePolicyTest {
    @Test
    fun defaultsUnknownPreferenceToNigerianEnglish() {
        val config = VoiceInputLanguagePolicy.configuration("invalid", 31)

        assertEquals(VoiceInputPreferences.ENGLISH_NIGERIA, config.selectedPreference)
        assertEquals(VoiceInputPreferences.ENGLISH_NIGERIA, config.languageTag)
        assertFalse(config.automaticSwitch)
    }

    @Test
    fun allowsExplicitHausaInputOnAndroid12() {
        val config = VoiceInputLanguagePolicy.configuration(
            VoiceInputPreferences.HAUSA_NIGERIA,
            31
        )

        assertEquals(VoiceInputPreferences.HAUSA_NIGERIA, config.languageTag)
        assertFalse(config.automaticSwitch)
    }

    @Test
    fun enablesAutomaticEnglishHausaSwitchOnlyOnAndroid14OrLater() {
        val supported = VoiceInputLanguagePolicy.configuration(
            VoiceInputPreferences.AUTOMATIC,
            34
        )
        val older = VoiceInputLanguagePolicy.configuration(
            VoiceInputPreferences.AUTOMATIC,
            31
        )

        assertTrue(supported.automaticSwitch)
        assertEquals(
            listOf("en-NG", "ha-NG"),
            supported.allowedLanguages
        )
        assertFalse(older.automaticSwitch)
        assertEquals("en-NG", older.languageTag)
    }
}
