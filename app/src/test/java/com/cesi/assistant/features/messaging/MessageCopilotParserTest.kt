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
    fun recognizesCommonHausaMessageFirstReactionCommands() {
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("CESI ka saka wa wannan sakon reaction na dariya")
        )
        assertEquals(
            MessageAction.React("❤️"),
            MessageCopilotParser.parse("saka wa saƙon nan reaction da soyayya")
        )
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("yi reaction a kan wannan saƙon da dariya")
        )
    }

    @Test
    fun recognizesLastMessageReactionCommands() {
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("react to the last message with laughing emoji")
        )
        assertEquals(
            MessageAction.React("❤️"),
            MessageCopilotParser.parse("put heart reaction on the latest message")
        )
    }

    @Test
    fun recognizesAdditionalHausaReactionTargetWordOrders() {
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("CESI ka yi reaction din dariya a kan saƙon da ya shigo")
        )
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("ka saka wa saƙon da ya shigo reaction na dariya")
        )
        assertEquals(
            MessageAction.React("❤️"),
            MessageCopilotParser.parse("yi reaction da soyayya a kan saƙon da aka aiko")
        )
    }

    @Test
    fun recognizesReactionOnThisMessageInEnglish() {
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("please put laughing reaction on this message")
        )
        assertEquals(
            MessageAction.React("👍"),
            MessageCopilotParser.parse("add thumbs up reaction to the message")
        )
    }

    @Test
    fun preservesUnknownCustomEmojiReaction() {
        assertEquals(MessageAction.React("🫶"), MessageCopilotParser.parse("react with 🫶"))
    }

    @Test
    fun recognizesHausaReactionUsingReactionDinDariyaForm() {
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("CESI ka yi reaction din dariya")
        )
        assertEquals(
            MessageAction.React("❤️"),
            MessageCopilotParser.parse("saka reaction ɗin soyayya")
        )
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("yi reaction da dariya a kan wannan saƙon")
        )
    }

    @Test
    fun parsesHausaSendMessageCommandsWithoutCorruptingTheMessageBody() {
        assertEquals(
            MessageAction.TextReply("Zan zo yanzu"),
            MessageCopilotParser.parse("tura masa sako cewa Zan zo yanzu")
        )
        assertEquals(
            MessageAction.TextReply("Ka jira ni a gida"),
            MessageCopilotParser.parse("Hey CESI, tura mata saƙo Ka jira ni a gida")
        )
        assertEquals(
            MessageAction.TextReply("Zan kira ka daga baya"),
            MessageCopilotParser.parse("aika masa Zan kira ka daga baya")
        )
    }

    @Test
    fun parsesHausaDirectWhatsAppMessageUsingSakoZuwaNumber() {
        assertEquals(
            MessageAction.DirectMessage("2348012345678", "Ina zuwa yanzu"),
            MessageCopilotParser.parse("tura sako zuwa +2348012345678 cewa Ina zuwa yanzu")
        )
    }

    @Test
    fun normalizesReactionSuffixesAndTrailingMessageReferences() {
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("ka saka reaction na dariya a kan wannan saƙon")
        )
        assertEquals(
            MessageAction.React("❤️"),
            MessageCopilotParser.parse("please react with heart reaction")
        )
    }

    @Test
    fun rejectsUnrecognizedReactionLabelInsteadOfTreatingItAsAnEmoji() {
        assertNull(MessageCopilotParser.parse("react with send message"))
    }

    @Test
    fun recognizesReactionCommandsThatReferToTheSendersMessage() {
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("Please put laughing reaction on his message")
        )
        assertEquals(
            MessageAction.React("👍"),
            MessageCopilotParser.parse("React with thumbs up on the latest WhatsApp message")
        )
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("React to the latest WhatsApp message with laughing emoji")
        )
        assertEquals(
            MessageAction.React("👍"),
            MessageCopilotParser.parse("React to his message with thumbs up")
        )
        assertEquals(
            MessageAction.React("❤️"),
            MessageCopilotParser.parse("ka yi masa reaction na soyayya a kan sakonsa")
        )
        assertEquals(
            MessageAction.React("😂"),
            MessageCopilotParser.parse("ka saka reaction na dariya a kan sakon da ya turo")
        )
    }

    @Test
    fun parsesWhatsAppMediaUploadWithCaption() {
        assertEquals(
            MessageAction.ShareMedia(
                caption = "New project trailer",
                mediaKind = "video",
                asStatus = false,
                captionProvided = true
            ),
            MessageCopilotParser.parse("upload a video with caption: New project trailer")
        )
    }

    @Test
    fun parsesWhatsAppStatusMediaWithCaption() {
        assertEquals(
            MessageAction.ShareMedia(
                caption = "Eid Mubarak to everyone",
                mediaKind = "image",
                asStatus = true,
                captionProvided = true
            ),
            MessageCopilotParser.parse("post this photo as WhatsApp status with caption: Eid Mubarak to everyone")
        )
    }

    @Test
    fun doesNotTurnOrdinaryPhotoSearchIntoMediaUpload() {
        assertNull(MessageCopilotParser.parse("send a photo"))
    }

    @Test
    fun parsesQuotedReplyToAnOlderMessageInEnglish() {
        assertEquals(
            MessageAction.QuotedReply(
                targetText = "I'll call you later",
                replyText = "Sure, talk soon"
            ),
            MessageCopilotParser.parse("reply to the old message \"I'll call you later\" with \"Sure, talk soon\"")
        )
    }

    @Test
    fun parsesQuotedReplyToAnOlderMessageInHausa() {
        assertEquals(
            MessageAction.QuotedReply(
                targetText = "Ina zuwa gobe",
                replyText = "To, zan jira ka"
            ),
            MessageCopilotParser.parse("amsa saƙon \"Ina zuwa gobe\" da \"To, zan jira ka\"")
        )
    }

    @Test
    fun doesNotParseQuotedReplyWithoutBothQuotedTexts() {
        assertNull(MessageCopilotParser.parse("reply to the old message \"Hello\" with"))
    }

    @Test
    fun parsesCaptionStyleLearningCommandsInEnglishAndHausa() {
        assertEquals(
            MessageAction.RememberCaptionStyle("FUNNY"),
            MessageCopilotParser.parse("remember my caption style as funny")
        )
        assertEquals(
            MessageAction.RememberCaptionStyle("PROFESSIONAL"),
            MessageCopilotParser.parse("for future captions use professional")
        )
        assertEquals(
            MessageAction.RememberCaptionStyle("FUNNY"),
            MessageCopilotParser.parse("ka rika amfani da salon barkwanci wajen captions")
        )
        assertEquals(
            MessageAction.ClearCaptionStyle,
            MessageCopilotParser.parse("clear caption style")
        )
    }

    @Test
    fun captionStyleLearningDoesNotOverrideAnExplicitCaption() {
        val parsed = MessageCopilotParser.parse(
            "post this photo as WhatsApp status with caption: Eid Mubarak"
        )
        assertEquals(
            MessageAction.ShareMedia(
                caption = "Eid Mubarak",
                mediaKind = "image",
                asStatus = true,
                captionProvided = true
            ),
            parsed
        )
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