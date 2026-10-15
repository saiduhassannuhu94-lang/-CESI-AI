package com.cesi.assistant.features.messaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageCopilotParserTest {
    @Test
    fun parsesNaturalTextReply() {
        assertEquals(
            MessageAction.TextReply("I'll call you later"),
            MessageCopilotParser.parse("Tell him I'll call you later")
        )
    }

    @Test
    fun preservesAndInsideReplyText() {
        assertEquals(
            MessageAction.TextReply("call me later and bring the charger"),
            MessageCopilotParser.parse("send him a message saying call me later and bring the charger")
        )
    }

    @Test
    fun parsesEmojiReaction() {
        assertEquals(MessageAction.React("😂"), MessageCopilotParser.parse("react with 😂"))
    }

    @Test
    fun mapsEnglishReactionNamesToEmoji() {
        assertEquals(MessageAction.React("😂"), MessageCopilotParser.parse("react with laughing emoji"))
        assertEquals(MessageAction.React("❤️"), MessageCopilotParser.parse("react with heart"))
        assertEquals(MessageAction.React("👍"), MessageCopilotParser.parse("react with thumbs up"))
    }

    @Test
    fun mapsHausaReactionNamesToEmoji() {
        assertEquals(MessageAction.React("😂"), MessageCopilotParser.parse("yi reaction da dariya"))
        assertEquals(MessageAction.React("❤️"), MessageCopilotParser.parse("yi reaction da soyayya"))
        assertEquals(MessageAction.React("😢"), MessageCopilotParser.parse("yi react da kuka"))
    }

    @Test
    fun preservesUnknownCustomEmojiReaction() {
        assertEquals(MessageAction.React("🫶"), MessageCopilotParser.parse("react with 🫶"))
    }

    @Test
    fun parsesSticker() {
        assertEquals(MessageAction.Sticker, MessageCopilotParser.parse("reply with a sticker"))
    }

    @Test
    fun parsesGif() {
        assertEquals(MessageAction.Gif, MessageCopilotParser.parse("send a GIF"))
    }

    @Test
    fun parsesImageQuery() {
        assertEquals(
            MessageAction.Image("a Toyota Camry"),
            MessageCopilotParser.parse("send him a picture a Toyota Camry")
        )
    }

    @Test
    fun parsesIgnore() {
        assertEquals(MessageAction.Ignore, MessageCopilotParser.parse("don't reply"))
    }

    @Test
    fun returnsNoSuggestionForUnknownMessage() {
        assertTrue(MessageCopilotParser.suggestionsFor("Can you send the document?").isEmpty())
    }

    @Test
    fun suggestsForKnownQuestion() {
        assertEquals(3, MessageCopilotParser.suggestionsFor("Where are you?").size)
    }
}
