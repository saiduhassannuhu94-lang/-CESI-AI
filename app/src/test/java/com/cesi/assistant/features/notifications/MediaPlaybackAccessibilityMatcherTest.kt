package com.cesi.assistant.features.notifications

import org.junit.Assert.*
import org.junit.Test

class MediaPlaybackAccessibilityMatcherTest {
    @Test fun queryMustBeVisibleInSearchContext() {
        assertTrue(MediaPlaybackAccessibilityMatcher.isSearchQueryVisible("Hausa comedy","com.google.android.youtube:id/search_edit_text","Hausa comedy",""))
        assertTrue(MediaPlaybackAccessibilityMatcher.isSearchQueryVisible("comedy","some_id","comedy",""))
        assertFalse(MediaPlaybackAccessibilityMatcher.isSearchQueryVisible("comedy","com.google.android.youtube:id/search_edit_text","cats",""))
        assertFalse(MediaPlaybackAccessibilityMatcher.isSearchQueryVisible(" ","search_edit_text","comedy",""))
    }
    @Test fun recognizesVideoLabelsAndRejectsNavigationLabels() {
        assertTrue(MediaPlaybackAccessibilityMatcher.hasResultLabel("com.google.android.youtube:id/video_title","",""))
        assertTrue(MediaPlaybackAccessibilityMatcher.hasResultLabel("some_tiktok_view","Comedy challenge",""))
        assertTrue(MediaPlaybackAccessibilityMatcher.hasResultLabel("some_tiktok_view","","Video by @creator"))
        assertFalse(MediaPlaybackAccessibilityMatcher.hasResultLabel("some_view","Search",""))
        assertFalse(MediaPlaybackAccessibilityMatcher.hasResultLabel("some_view","","More options"))
        assertFalse(MediaPlaybackAccessibilityMatcher.hasResultLabel("some_view","",""))
    }
    @Test fun resultCardMustBeLargeAndOnScreen() {
        assertTrue(MediaPlaybackAccessibilityMatcher.isPlausibleResultCard(540,700,250,950,1080,2400,3f))
        assertFalse(MediaPlaybackAccessibilityMatcher.isPlausibleResultCard(100,40,250,290,1080,2400,3f))
        assertFalse(MediaPlaybackAccessibilityMatcher.isPlausibleResultCard(1080,2200,0,2200,1080,2400,3f))
    }
}
