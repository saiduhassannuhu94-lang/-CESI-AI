package com.cesi.assistant.core.task

import java.util.Locale

/**
 * Resolves only explicitly supported short follow-ups against the latest local
 * references. Unknown or ambiguous phrases are returned unchanged so the intent
 * parser, not this resolver, can ask for clarification.
 */
object FollowUpResolver {
    private val appFollowUps = setOf(
        "open it", "launch it", "start it", "open that", "launch that",
        "bude shi", "buɗe shi", "bude app din", "buɗe app din",
        "open the app", "launch the app"
    )

    private val searchOnYouTubeFollowUps = setOf(
        "search that on youtube", "search it on youtube",
        "find that on youtube", "find it on youtube",
        "search for that on youtube", "search for it on youtube",
        "bincika hakan a youtube", "nemo hakan a youtube",
        "nemo shi a youtube", "nemo ta a youtube"
    )

    private val searchFollowUps = setOf(
        "search that", "search it", "search for that", "search for it",
        "find that", "find it", "bincika hakan", "nemo hakan",
        "nemo shi", "nemo ta"
    )

    private val contactFollowUps = setOf(
        "call him", "call her", "call them", "call that person",
        "kira shi", "kira ta", "kira su", "kira wannan mutumin"
    )

    fun resolve(step: String, lastApp: String?, lastSearch: String?, lastContact: String?): String {
        val normalized = normalize(step)
        if (normalized.isBlank()) return step

        if (!lastSearch.isNullOrBlank() && normalized in searchOnYouTubeFollowUps) {
            return "search youtube $lastSearch"
        }
        if (!lastApp.isNullOrBlank() && normalized in appFollowUps) {
            return "open $lastApp"
        }
        if (!lastSearch.isNullOrBlank() && normalized in searchFollowUps) {
            return "search $lastSearch"
        }
        if (!lastContact.isNullOrBlank() && normalized in contactFollowUps) {
            return "call $lastContact"
        }
        return step
    }

    private fun normalize(text: String): String =
        text.trim().lowercase(Locale.ROOT)
            .replace(Regex("[?!.,]+$"), "")
            .replace(Regex("\\s+"), " ")
}
