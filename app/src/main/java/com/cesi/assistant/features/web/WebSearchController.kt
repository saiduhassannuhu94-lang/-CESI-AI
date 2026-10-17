package com.cesi.assistant.features.web

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

data class MediaSearchLaunchResult(val opened: Boolean, val message: String)

class WebSearchController(private val context: Context) {
    fun search(query: String): String {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return "Me kake son in bincika?"
        val url = Uri.parse("https://www.google.com/search?q=${Uri.encode(cleanQuery)}")
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            "Na buɗe Google na bincika: $cleanQuery."
        } catch (_: ActivityNotFoundException) {
            "Ban sami browser da zan buɗe Google ba."
        } catch (_: Exception) {
            "An samu matsala wajen buɗe Google."
        }
    }

    fun youtubeSearch(query: String): String {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return "Me kake son in nema a YouTube?"
        val url = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(cleanQuery)}")
        return try {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, url).apply {
                    setPackage("com.google.android.youtube")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: ActivityNotFoundException) {
                context.startActivity(Intent(Intent.ACTION_VIEW, url).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
            "Na buɗe YouTube na nema: $cleanQuery."
        } catch (_: ActivityNotFoundException) {
            "Ban sami YouTube ko browser da zan buɗe ba."
        } catch (_: Exception) {
            "An samu matsala wajen buɗe YouTube."
        }
    }

    fun searchMedia(platform: String, query: String, requestPlayback: Boolean): MediaSearchLaunchResult {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return MediaSearchLaunchResult(false, "Ka faɗi abin da kake son nema.")
        val label: String
        val url: Uri
        val packages: List<String>
        when (platform.uppercase(Locale.ROOT)) {
            "TIKTOK" -> {
                label = "TikTok"
                url = Uri.parse("https://www.tiktok.com/search").buildUpon()
                    .appendQueryParameter("q", cleanQuery).build()
                packages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
            }
            else -> {
                label = "YouTube"
                url = Uri.parse("https://www.youtube.com/results").buildUpon()
                    .appendQueryParameter("search_query", cleanQuery).build()
                packages = listOf("com.google.android.youtube")
            }
        }
        var opened = false
        for (packageName in packages) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, url).apply {
                    setPackage(packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                opened = true
                break
            } catch (_: Exception) {
                // Try the browser URL when an installed platform app cannot handle the link.
            }
        }
        if (!opened) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, url).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                opened = true
            } catch (_: Exception) {
                opened = false
            }
        }
        if (!opened) return MediaSearchLaunchResult(
            false, "Ban iya buɗe sakamakon neman a $label ba. Ka tabbatar app ɗin ko browser yana aiki."
        )
        val message = if (requestPlayback) {
            "Na buɗe sakamakon neman “$cleanQuery” a $label. Ban fara bidiyo ta atomatik ba a wannan sigar; ka taɓa bidiyon da kake nufi don kunnawa."
        } else {
            "Na buɗe sakamakon neman “$cleanQuery” a $label."
        }
        return MediaSearchLaunchResult(true, message)
    }
}
