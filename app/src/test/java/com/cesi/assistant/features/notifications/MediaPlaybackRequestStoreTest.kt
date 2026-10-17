package com.cesi.assistant.features.notifications

import com.cesi.assistant.core.intent.VideoPlatform
import org.junit.Assert.*
import org.junit.Test

class MediaPlaybackRequestStoreTest {
    @Test fun storesOnlyNonBlankRequests() {
        var now=100L
        val store=MediaPlaybackRequestStore({ now })
        assertFalse(store.begin(VideoPlatform.YOUTUBE," "))
        assertNull(store.current())
        assertTrue(store.begin(VideoPlatform.YOUTUBE,"comedy"))
        assertEquals("comedy",store.current()?.query)
    }
    @Test fun expiresAtExactTtlBoundary() {
        var now=100L
        val store=MediaPlaybackRequestStore({ now },100L)
        assertTrue(store.begin(VideoPlatform.YOUTUBE,"comedy"))
        now=199L; assertEquals("comedy",store.current()?.query)
        now=200L; assertNull(store.current())
    }
    @Test fun backwardsClockInvalidatesRequest() {
        var now=200L
        val store=MediaPlaybackRequestStore({ now },100L)
        assertTrue(store.begin(VideoPlatform.TIKTOK,"comedy"))
        now=199L; assertNull(store.current())
    }
    @Test fun newerRequestReplacesAndClearIsIdempotent() {
        var now=200L
        val store=MediaPlaybackRequestStore({ now })
        assertTrue(store.begin(VideoPlatform.YOUTUBE,"cats")); now++
        assertTrue(store.begin(VideoPlatform.TIKTOK,"comedy"))
        assertEquals(VideoPlatform.TIKTOK,store.current()?.platform)
        assertEquals("comedy",store.current()?.query)
        store.clear(); store.clear(); assertNull(store.current())
    }
    @Test fun onlyOneAccessibilityEventCanClaimTheSamePendingRequest() {
        var now = 100L
        val store = MediaPlaybackRequestStore({ now }, 1000L)
        assertTrue(store.begin(VideoPlatform.YOUTUBE, "comedy"))
        assertEquals("comedy", store.claim("com.google.android.youtube")?.query)
        assertNull(store.claim("com.google.android.youtube"))
        store.release(PendingMediaPlayback(VideoPlatform.YOUTUBE, "comedy", 100L))
        assertEquals("comedy", store.claim("com.google.android.youtube")?.query)
    }

    @Test fun claimRejectsOtherPlatformAndClearResetsClaim() {
        val store = MediaPlaybackRequestStore({ 100L }, 1000L)
        assertTrue(store.begin(VideoPlatform.TIKTOK, "comedy"))
        assertNull(store.claim("com.google.android.youtube"))
        assertEquals("comedy", store.claim("com.zhiliaoapp.musically")?.query)
        store.clear()
        assertNull(store.claim("com.zhiliaoapp.musically"))
    }

    @Test fun requestIsLimitedToItsPlatformPackages() {
        val youtube=PendingMediaPlayback(VideoPlatform.YOUTUBE,"comedy",0)
        val tiktok=PendingMediaPlayback(VideoPlatform.TIKTOK,"comedy",0)
        assertTrue(youtube.allowsPackage("com.google.android.youtube"))
        assertFalse(youtube.allowsPackage("com.whatsapp"))
        assertTrue(tiktok.allowsPackage("com.zhiliaoapp.musically"))
        assertTrue(tiktok.allowsPackage("com.ss.android.ugc.trill"))
        assertFalse(tiktok.allowsPackage("com.whatsapp"))
    }
}
