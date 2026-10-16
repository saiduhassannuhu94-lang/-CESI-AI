package com.cesi.assistant.core.voice

import com.cesi.assistant.features.messaging.MessageAction
import com.cesi.assistant.features.messaging.MessageCopilotParser
import java.util.Locale

/**
 * Selects a command from speech-recognition N-best hypotheses without executing
 * anything. A lower-ranked hypothesis may replace the first only when the
 * first already looks like a reaction command and all recognized alternatives
 * agree on the same emoji. Ambiguous alternatives never trigger a guessed action.
 */
object VoiceCommandCandidateSelector {
    fun select(candidates: List<String>): String? {
        val clean = candidates.map(String::trim).filter(String::isNotBlank)
        val first = clean.firstOrNull() ?: return null

        if (MessageCopilotParser.parse(first) is MessageAction.React) return first
        if (!hasReactionSignal(first)) return first

        val recognized = clean.drop(1).mapNotNull { candidate ->
            val action = MessageCopilotParser.parse(candidate)
            if (action is MessageAction.React) candidate to action.emoji else null
        }
        if (recognized.isEmpty()) return first

        val distinctEmojis = recognized.map { it.second }.distinct()
        return if (distinctEmojis.size == 1) recognized.first().first else first
    }

    private fun hasReactionSignal(value: String): Boolean {
        val normalized = value.lowercase(Locale.ROOT)
        return listOf(
            "reaction", "react", "emoji", "dariya", "soyayya", "zuciya",
            "thumbs up", "thumb up", "laugh", "laughing", "heart",
            "kuka", "bakin ciki", "baƙin ciki", "fushi", "mamaki"
        ).any(normalized::contains)
    }
}
