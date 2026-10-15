package com.cesi.assistant.features.messaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun parsesEnglishDirectWhatsAppMessageWithoutSendingAutomatically() {
        assertEquals(
            MessageAction.DirectMessage("2348012345678", "Sannu, how are you?"),
            MessageCopilotParser.parse("send WhatsApp message to +234 801 234 5678 saying Sannu, how are you?")
        )
    }

    @Test
    fun parsesHausaDirectWhatsAppMessage() {
        assertEquals(
            MessageAction.DirectMessage("2348012345678", "Ina zuwa yanzu"),
            MessageCopilotParser.parse("tura saƙon WhatsApp zuwa +2348012345678 cewa Ina zuwa yanzu")
        )
    }

    @Test
    fun rejectsLocalNumberForDirectWhatsAppMessage() {
        assertEquals(
            MessageAction.InvalidDirectMessage,
            MessageCopilotParser.parse("send WhatsApp message to 08012345678 saying Hello")
        )
    }

    @Test
    fun preservesMessageTextContainingAnd() {
        assertEquals(
            MessageAction.DirectMessage("2348012345678", "come home and bring the charger"),
            MessageCopilotParser.parse("message +2348012345678 saying come home and bring the charger")
        )
    }

    @Test
    fun acceptsInternationalNumberWithInternationalDialPrefix() {
        assertEquals(
            MessageAction.DirectMessage("2348012345678", "Hello"),
            MessageCopilotParser.parse("message 00 234 801 234 5678 saying Hello")
        )
    }

    @Test
    fun rejectsDirectWhatsAppCommandWithoutMessageBody() {
        assertEquals(
            MessageAction.InvalidDirectMessage,
            MessageCopilotParser.parse("send WhatsApp message to +2348012345678 saying")
        )
    }

    @Test
    fun rejectsMalformedDirectWhatsAppNumberInsteadOfTreatingItAsReplyText() {
        assertEquals(
            MessageAction.InvalidDirectMessage,
            MessageCopilotParser.parse("tura saƙon WhatsApp zuwa 123 cewa Sannu")
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
    fun recognizesNaturalEnglishReactionToMessageCommand() {
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("React to this message with laughing emoji")
        )
        assertEquals(
            MessageAction.React("❤️"),
            MessageCopilotParser.parse("Please add a reaction to this message with heart")
        )
        assertEquals(
            MessageAction.React("👍"),
            MessageCopilotParser.parse("Put reaction to the message with thumbs up")
        )
    }

    @Test
    fun recognizesNaturalHausaReactionCommands() {
        assertEquals(MessageAction.React("😂"), MessageCopilotParser.parse("saka reaction da dariya"))
        assertEquals(MessageAction.React("❤️"), MessageCopilotParser.parse("ka saka masa reaction da soyayya"))
        assertEquals(MessageAction.React("😢"), MessageCopilotParser.parse("yi masa reaction da kuka"))
    }

    @Test
    fun recognizesEmojiAfterReactToAndIgnoresSpeechPunctuation() {
        assertEquals(MessageAction.React("😂"), MessageCopilotParser.parse("react to 😂"))
        assertEquals(MessageAction.React("😂"), MessageCopilotParser.parse("React with a laughing emoji."))
    }

    @Test
    fun doesNotTreatMessageTargetAsAnEmoji() {
        assertNull(MessageCopilotParser.parse("react to this message"))
    }

    @Test
    fun recognizesWakePhraseAndPoliteLeadInForReaction() {
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("Hey CESI, please react with laughing emoji")
        )
        assertEquals(
            MessageAction.React("👍"),
            MessageCopilotParser.parse("Okay CESI: can you react to this message with thumbs up?")
        )
    }

    @Test
    fun recognizesWakePhraseWithHausaReactionCommand() {
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("CESI ka yi reaction da dariya")
        )
        assertEquals(
            MessageAction.React("❤️"),
            MessageCopilotParser.parse("Hey CESI, ka saka masa reaction da soyayya")
        )
    }

    @Test
    fun recognizesHausaReactionExplicitlyTargetingCurrentMessage() {
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("yi wa saƙon nan reaction da dariya")
        )
        assertEquals(
            MessageAction.React("❤️"),
            MessageCopilotParser.parse("Hey CESI, yi wa wannan saƙon reaction da soyayya")
        )
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