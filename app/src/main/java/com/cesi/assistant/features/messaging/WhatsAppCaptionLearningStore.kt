package com.cesi.assistant.features.messaging

import android.content.Context

/**
 * Learns only the user's chosen caption tone. It deliberately does not retain
 * media, message text, or previous captions. The preference stays on-device.
 */
enum class WhatsAppCaptionStyle {
    NATURAL,
    FUNNY,
    PROFESSIONAL,
    ISLAMIC,
    ROMANTIC,
    SHORT;

    companion object {
        fun fromPhrase(value: String): WhatsAppCaptionStyle? {
            val normalized = value.trim().lowercase(java.util.Locale.ROOT)
                .replace(Regex("""\s+"""), " ")
                .removeSuffix(" style")
                .removeSuffix(" tone")
                .trim()
            return when (normalized) {
                "natural", "simple", "normal", "na dabi'a", "na dabi’a", "sauki", "mai sauki" -> NATURAL
                "funny", "humorous", "playful", "barkwanci", "dariya" -> FUNNY
                "professional", "business", "formal", "kwararre", "na kasuwanci" -> PROFESSIONAL
                "islamic", "muslim", "faith-based", "addini", "na musulunci" -> ISLAMIC
                "romantic", "love", "soyayya", "kauna", "ƙauna" -> ROMANTIC
                "short", "brief", "minimal", "gajere", "a takaice", "a taƙaice" -> SHORT
                else -> null
            }
        }
    }

    fun captionFor(mediaKind: String, asStatus: Boolean): String {
        val video = mediaKind == "video"
        return when (this) {
            NATURAL -> when {
                asStatus && video -> "Ga ɗan abin da nake son rabawa. 🎬"
                asStatus -> "Wani sabon abu da nake son rabawa. ✨"
                video -> "Ga bidiyon da na zaɓa."
                mediaKind == "image" -> "Ga hoton da na zaɓa."
                else -> "Ga wannan abin da nake son rabawa."
            }
            FUNNY -> when {
                asStatus && video -> "Ku kalla, ku bani rating, kada ku yi min dariya 😂🎬"
                asStatus -> "Ku yi min kallon hoton, amma kada ku yi dariya sosai 😂"
                video -> "Ga bidiyon, ku kalla ku bani ra'ayi 😂🎬"
                mediaKind == "image" -> "Ga hoton, ku yi hukunci 😂"
                else -> "Ga abin da na kawo muku 😄"
            }
            PROFESSIONAL -> when {
                asStatus && video -> "Ina farin cikin raba wannan bidiyo da ku."
                asStatus -> "Ina farin cikin raba wannan hoto da ku."
                video -> "Ga bidiyon da nake son rabawa."
                mediaKind == "image" -> "Ga hoton da nake son rabawa."
                else -> "Ga wannan media da nake son rabawa."
            }
            ISLAMIC -> when {
                asStatus && video -> "Alhamdulillah, ga wani abu da nake son rabawa. 🤲"
                asStatus -> "Alhamdulillah a kan ni'imar yau. 🤲"
                video -> "Alhamdulillah, ga bidiyon da nake son rabawa."
                mediaKind == "image" -> "Alhamdulillah, ga hoton da nake son rabawa."
                else -> "Alhamdulillah, ga wannan abin da nake rabawa."
            }
            ROMANTIC -> when {
                asStatus && video -> "Ƙananan lokuta, manyan tunani. ❤️🎬"
                asStatus -> "Ƙananan lokuta, manyan tunani. ❤️"
                video -> "Ga wannan lokaci mai kyau. ❤️🎬"
                mediaKind == "image" -> "Wani lokaci mai kyau da zan so in tuna. ❤️"
                else -> "Wani lokaci mai kyau. ❤️"
            }
            SHORT -> when {
                asStatus && video -> "Alhamdulillah 🎬"
                asStatus -> "Alhamdulillah ✨"
                video -> "Ga bidiyon."
                mediaKind == "image" -> "Ga hoton."
                else -> "Ga shi."
            }
        }
    }
}

class WhatsAppCaptionLearningStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun remember(stylePhrase: String): Boolean {
        val style = WhatsAppCaptionStyle.fromPhrase(stylePhrase) ?: return false
        preferences.edit().putString(KEY_STYLE, style.name).apply()
        return true
    }

    fun clear() {
        preferences.edit().remove(KEY_STYLE).apply()
    }

    fun currentStyle(): WhatsAppCaptionStyle =
        preferences.getString(KEY_STYLE, null)
            ?.let { stored -> runCatching { WhatsAppCaptionStyle.valueOf(stored) }.getOrNull() }
            ?: WhatsAppCaptionStyle.NATURAL

    fun captionFor(mediaKind: String, asStatus: Boolean): String =
        currentStyle().captionFor(mediaKind, asStatus)

    companion object {
        private const val PREFERENCES_NAME = "cesi_whatsapp_caption_preferences"
        private const val KEY_STYLE = "caption_style"
    }
}
