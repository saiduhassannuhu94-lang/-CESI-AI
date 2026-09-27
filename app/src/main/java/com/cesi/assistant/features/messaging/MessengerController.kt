package com.cesi.assistant.features.messaging

import android.content.Context
import android.content.Intent

class MessengerController(private val context: Context) {
    fun draft(target: String?, text: String): String {
        val clean = text.trim()
        if (clean.isBlank()) return "Me kake son in tura?"

        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, clean)
                setPackage("com.facebook.orca")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            if (target.isNullOrBlank()) {
                "Na buɗe Messenger da saƙon. Ka zaɓi mutumin da za a tura masa."
            } else {
                "Na shirya saƙon zuwa " + target + " a Messenger. Ka zaɓi chat ɗin ka tabbatar kafin aika."
            }
        } catch (_: Exception) {
            "Ban iya buɗe Messenger ba. Ka tabbatar an girka Messenger."
        }
    }
}
