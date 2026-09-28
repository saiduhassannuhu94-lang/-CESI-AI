package com.cesi.assistant.features.web

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

class VisualSearchController(private val context: Context) {
    fun search(query: String): String {
        val clean = query.trim()
        if (clean.isBlank()) return "Me kake son in nuna maka hoto?"

        val url = Uri.parse("https://www.google.com/search?tbm=isch&q=" + Uri.encode(clean))
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, url).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            "Na buɗe hotunan da suka dace da " + clean + "."
        } catch (_: ActivityNotFoundException) {
            "Ban sami browser da zan nuna hotunan ba."
        } catch (_: Exception) {
            "An samu matsala wajen buɗe hotunan."
        }
    }
}
