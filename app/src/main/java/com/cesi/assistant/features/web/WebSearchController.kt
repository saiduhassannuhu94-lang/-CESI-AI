package com.cesi.assistant.features.web

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

class WebSearchController(private val context: Context) {

    fun search(query: String): String {
        val cleanQuery = query.trim()

        if (cleanQuery.isBlank()) {
            return "Me kake son in bincika?"
        }

        val url = Uri.parse(
            "https://www.google.com/search?q=${Uri.encode(cleanQuery)}"
        )

        return try {
            val intent = Intent(Intent.ACTION_VIEW, url).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            "Na buɗe Google na bincika: $cleanQuery."
        } catch (_: ActivityNotFoundException) {
            "Ban sami browser da zan buɗe Google ba."
        } catch (_: Exception) {
            "An samu matsala wajen buɗe Google."
        }
    }
}
