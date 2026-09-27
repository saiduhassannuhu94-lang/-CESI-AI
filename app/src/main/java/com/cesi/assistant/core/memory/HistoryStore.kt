package com.cesi.assistant.core.memory

import android.content.Context

data class HistoryEntry(val user: String, val assistant: String, val time: Long)

class HistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("cesi_history", Context.MODE_PRIVATE)

    fun add(user: String, assistant: String) {
        val items = getAll().takeLast(49).toMutableList()
        items += HistoryEntry(user.trim(), assistant.trim(), System.currentTimeMillis())
        val encoded = items.joinToString("\n") { entry ->
            entry.time.toString() + "|" + escape(entry.user) + "|" + escape(entry.assistant)
        }
        prefs.edit().putString("items", encoded).apply()
    }

    fun getAll(): List<HistoryEntry> =
        prefs.getString("items", "").orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val p = line.split("|", limit = 3)
                if (p.size != 3) null
                else HistoryEntry(
                    p[1].replace("\\|", "|"),
                    p[2].replace("\\|", "|"),
                    p[0].toLongOrNull() ?: 0L
                )
            }.toList()

    fun clear() = prefs.edit().remove("items").apply()

    private fun escape(value: String) = value.replace("|", "\\|").replace("\n", " ")
}
