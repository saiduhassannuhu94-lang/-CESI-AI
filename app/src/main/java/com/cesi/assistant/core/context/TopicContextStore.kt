package com.cesi.assistant.core.context

import android.content.Context

class TopicContextStore(context: Context) {
    private val prefs = context.getSharedPreferences("cesi_topic_context", Context.MODE_PRIVATE)

    fun setTopic(topic: String) {
        if (topic.isNotBlank()) {
            prefs.edit()
                .putString("active_topic", topic.trim())
                .putLong("active_topic_saved_at", System.currentTimeMillis())
                .apply()
        }
    }

    fun getTopic(): String {
        val topic = prefs.getString("active_topic", "").orEmpty()
        val savedAt = prefs.getLong("active_topic_saved_at", -1L)
        if (!TopicMemory.isFresh(topic, savedAt, System.currentTimeMillis())) {
            clear()
            return ""
        }
        return topic
    }

    fun clear() {
        prefs.edit()
            .remove("active_topic")
            .remove("active_topic_saved_at")
            .apply()
    }
}
