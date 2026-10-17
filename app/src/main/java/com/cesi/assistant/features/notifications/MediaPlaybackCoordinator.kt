package com.cesi.assistant.features.notifications

import android.os.SystemClock
import com.cesi.assistant.core.intent.VideoPlatform

/** One-shot in-memory request. It is created only by an explicit user command. */
data class PendingMediaPlayback(val platform: VideoPlatform, val query: String, val createdAtMs: Long) {
    fun allowsPackage(packageName: String): Boolean = when (platform) {
        VideoPlatform.YOUTUBE -> packageName == YOUTUBE_PACKAGE
        VideoPlatform.TIKTOK -> packageName == TIKTOK_PACKAGE || packageName == TIKTOK_LEGACY_PACKAGE
    }
    companion object {
        const val TTL_MS = 45_000L
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        const val TIKTOK_PACKAGE = "com.zhiliaoapp.musically"
        const val TIKTOK_LEGACY_PACKAGE = "com.ss.android.ugc.trill"
    }
}

internal class MediaPlaybackRequestStore(private val nowMs: () -> Long, private val ttlMs: Long = PendingMediaPlayback.TTL_MS) {
    private var pending: PendingMediaPlayback? = null
    private var claimed = false

    @Synchronized fun begin(platform: VideoPlatform, query: String): Boolean {
        val clean = query.trim()
        if (clean.isBlank() || ttlMs <= 0L) {
            pending = null
            claimed = false
            return false
        }
        pending = PendingMediaPlayback(platform, clean, nowMs())
        claimed = false
        return true
    }

    @Synchronized fun current(): PendingMediaPlayback? {
        val request = pending ?: return null
        val now = nowMs()
        if (now < request.createdAtMs || now - request.createdAtMs >= ttlMs) {
            pending = null
            claimed = false
            return null
        }
        return request
    }

    /** At most one accessibility event may try this request at a time. */
    @Synchronized fun claim(packageName: String): PendingMediaPlayback? {
        val request = current() ?: return null
        if (claimed || !request.allowsPackage(packageName)) return null
        claimed = true
        return request
    }

    @Synchronized fun release(request: PendingMediaPlayback) {
        if (pending == request) claimed = false
    }

    @Synchronized fun clear() {
        pending = null
        claimed = false
    }
}

object MediaPlaybackCoordinator {
    private val store = MediaPlaybackRequestStore({ SystemClock.elapsedRealtime() })
    fun begin(platform: VideoPlatform, query: String): Boolean = store.begin(platform, query)
    fun currentForPackage(packageName: String): PendingMediaPlayback? = store.current()?.takeIf { it.allowsPackage(packageName) }
    fun claimForPackage(packageName: String): PendingMediaPlayback? = store.claim(packageName)
    fun release(request: PendingMediaPlayback) = store.release(request)
    fun clear() = store.clear()
}
