package com.cesi.assistant.features.web

import android.content.Context
import android.content.Intent
import android.net.Uri

class WebSearchController(private val context: Context) {
    fun search(query: String): String {
        if (query.isBlank()) return "Me kake son in bincika?"
        val url = "https://www.google.com/search?q=" +
            Uri.encode(query)
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        return "Na buɗe Google na bincika $query."
    }
}
