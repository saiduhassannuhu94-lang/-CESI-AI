package com.cesi.assistant.features.notifications

import java.util.Locale

/** Conservative Android-free checks for query context and visible result cards. */
object MediaPlaybackAccessibilityMatcher {
    private val navigationLabels = setOf(
        "search", "home", "shorts", "subscriptions", "library", "you", "settings", "back",
        "more options", "share", "like", "comment", "comments", "subscribe", "following",
        "friends", "inbox", "profile", "create", "upload", "notifications", "downloads",
        "history", "play", "pause", "play video", "pause video", "close", "cancel"
    )
    fun normalize(value: String): String = value.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()

    fun isSearchQueryVisible(expectedQuery: String, viewId: String, text: String, description: String): Boolean {
        val expected = normalize(expectedQuery)
        if (expected.isBlank()) return false
        val id = normalize(viewId)
        val values = listOf(normalize(text), normalize(description)).filter { it.isNotBlank() }
        return values.any { it.contains(expected) } && (id.contains("search") || values.any { it == expected })
    }

    fun hasResultLabel(viewId: String, text: String, description: String): Boolean {
        val id = normalize(viewId)
        if (listOf("video_title", "video_thumbnail", "video_renderer", "video_card", "video_item", "item_video", "aweme", "cover_image", "search_result").any(id::contains)) return true
        val labels = listOf(normalize(text), normalize(description)).filter { it.isNotBlank() }
        return labels.any { it !in navigationLabels && !it.startsWith("more options") && !it.startsWith("search suggestions") && !it.startsWith("navigate to ") && it.length >= 2 }
    }

    fun isPlausibleResultCard(widthPx: Int, heightPx: Int, topPx: Int, bottomPx: Int, screenWidthPx: Int, screenHeightPx: Int, density: Float): Boolean {
        if (screenWidthPx <= 0 || screenHeightPx <= 0 || density <= 0f) return false
        val minWidth = maxOf((screenWidthPx * 0.30f).toInt(), (120f * density).toInt())
        val minHeight = maxOf((screenHeightPx * 0.10f).toInt(), (72f * density).toInt())
        val maxHeight = (screenHeightPx * 0.70f).toInt()
        return widthPx >= minWidth && heightPx >= minHeight && heightPx <= maxHeight &&
            topPx >= (45f * density).toInt() && bottomPx <= screenHeightPx - (32f * density).toInt() && bottomPx > topPx
    }
}
