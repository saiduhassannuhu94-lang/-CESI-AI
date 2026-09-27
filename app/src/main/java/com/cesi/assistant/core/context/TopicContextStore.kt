package com.cesi.assistant.core.context

import android.content.Context

class TopicContextStore(context: Context) {
    private val prefs = context.getSharedPreferences("cesi_topic_context", Context.MODE_PRIVATE)

    fun setTopic(topic: String) {
        if (topic.isNotBlank()) prefs.edit().putString("active_topic", topic.trim()).apply()
    }

    fun getTopic(): String = prefs.getString("active_topic", "").orEmpty()

    fun clear() {
        prefs.edit().remove("active_topic").apply()
    }
}
