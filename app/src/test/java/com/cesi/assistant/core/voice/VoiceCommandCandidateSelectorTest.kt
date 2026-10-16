package com.cesi.assistant.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCommandCandidateSelectorTest {
    @Test
    fun returnsNullWhenAllSpeechCandidatesAreBlank() {
        assertNull(VoiceCommandCandidateSelector.select(listOf(" ", "")))
    }

    @Test
    fun keepsFirstCandidateForNonReactionCommands() {
        assertEquals(
            "open WhatsApp",
            VoiceCommandCandidateSelector.select(listOf("open WhatsApp", "react with laughing emoji"))
        )
    }

    @Test
    fun recoversReactionFromAlternativeWhenTopCandidateHasReactionSignalButIsUnparseable() {
        assertEquals(
            "ka saka reaction na dariya",
            VoiceCommandCandidateSelector.select(
                listOf(
                    "ka saka rection na dariya",
                    "ka saka reaction na dariya"
                )
            )
        )
    }

    @Test
    fun doesNotGuessWhenRecognizedAlternativesDisagreeOnEmoji() {
        assertEquals(
            "react with maybe reaction",
            VoiceCommandCandidateSelector.select(
                listOf(
                    "react with maybe reaction",
                    "react with laughing emoji",
                    "react with heart"
                )
            )
        )
    }

    @Test
    fun keepsAlreadyRecognizedFirstReactionCandidate() {
        assertEquals(
            "Hey CESI, react with laughing emoji",
            VoiceCommandCandidateSelector.select(
                listOf(
                    "Hey CESI, react with laughing emoji",
                    "react with heart"
                )
            )
        )
    }
}
