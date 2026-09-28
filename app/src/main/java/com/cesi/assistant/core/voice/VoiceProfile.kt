package com.cesi.assistant.core.voice

data class VoiceProfile(
    val id: String,
    val name: String,
    val gender: Gender,
    val style: String,
    val sample: String,
    val locale: String = "en-NG"
) {
    enum class Gender { MALE, FEMALE }

    companion object {
        val defaults = listOf(
            VoiceProfile("male_1", "David", Gender.MALE, "Calm • Professional • Clear", "Hello, I'm CESI. How can I help you today?"),
            VoiceProfile("male_2", "James", Gender.MALE, "Friendly • Energetic • Smart", "Hey there. This is CESI. What can I do for you?"),
            VoiceProfile("male_3", "Tunde", Gender.MALE, "Deep • Confident • Mature", "Greetings. I'm CESI. Let's get things done."),
            VoiceProfile("female_1", "Aisha", Gender.FEMALE, "Warm • Caring • Friendly", "Hi! I'm CESI. How can I make your day better?"),
            VoiceProfile("female_2", "Zainab", Gender.FEMALE, "Energetic • Positive • Supportive", "Hello! I'm CESI. What are we solving today?"),
            VoiceProfile("female_3", "Fatima", Gender.FEMALE, "Gentle • Calm • Wise", "Hello. I'm CESI. Ask me anything; I'm here for you.")
        )
    }
}
