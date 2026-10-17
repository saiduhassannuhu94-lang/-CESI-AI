package com.cesi.assistant.core.intent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaCommandParserTest {
    @Test
    fun parsesYouTubeSearchAndPlay() {
        assertEquals(AssistantIntent.MediaSearchAndPlay(VideoPlatform.YOUTUBE, "comedy", true),
            MediaCommandParser.parse("open YouTube search comedy and play it"))
    }
    @Test
    fun parsesTikTokSearchAndPlayWithCommonSpeechTypo() {
        assertEquals(AssistantIntent.MediaSearchAndPlay(VideoPlatform.TIKTOK, "comedy", true),
            MediaCommandParser.parse("open TikTok, serach comedy then play the first video"))
    }
    @Test
    fun parsesQueryFirstSearchAndPlay() {
        assertEquals(AssistantIntent.MediaSearchAndPlay(VideoPlatform.YOUTUBE, "Hausa funny skits", true),
            MediaCommandParser.parse("search for Hausa funny skits on YouTube and play it"))
    }
    @Test
    fun parsesHausaSearchWithoutClaimingPlayback() {
        assertEquals(AssistantIntent.MediaSearchAndPlay(VideoPlatform.TIKTOK, "barkwanci Hausa", false),
            MediaCommandParser.parse("bincika barkwanci Hausa a TikTok"))
    }
    @Test
    fun noPlaybackRequestIsNotInvented() {
        assertEquals(AssistantIntent.MediaSearchAndPlay(VideoPlatform.YOUTUBE, "comedy", false),
            MediaCommandParser.parse("YouTube search comedy"))
    }
    @Test
    fun openingAnAppAloneIsNotMediaSearch() {
        assertNull(MediaCommandParser.parse("open YouTube"))
        assertNull(MediaCommandParser.parse("open TikTok"))
    }
}
