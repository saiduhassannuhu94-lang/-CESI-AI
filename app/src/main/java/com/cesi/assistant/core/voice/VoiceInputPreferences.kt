package com.cesi.assistant.core.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent

/**
 * Speech-input language is separate from Text-to-Speech output language.
 * English remains the default; Hausa can be selected explicitly on older
 * Android versions, while automatic switching is offered on API 34+.
 */
object VoiceInputPreferences {
    const val ENGLISH_NIGERIA = "en-NG"
    const val HAUSA_NIGERIA = "ha-NG"
    const val AUTOMATIC = "auto"

    private const val PREFERENCES_NAME = "cesi_voice_input"
    private const val LANGUAGE_KEY = "recognition_language"

    fun selectedLanguage(context: Context): String {
        val stored = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(LANGUAGE_KEY, ENGLISH_NIGERIA)
        return VoiceInputLanguagePolicy.configuration(stored.orEmpty(), Build.VERSION.SDK_INT)
            .selectedPreference
    }

    fun setLanguage(context: Context, language: String) {
        require(language == ENGLISH_NIGERIA || language == HAUSA_NIGERIA ||
            (language == AUTOMATIC && Build.VERSION.SDK_INT >= 34)
        ) { "Unsupported voice-input language option" }
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_KEY, language)
            .apply()
    }

    fun configureRecognizerIntent(context: Context, intent: Intent) {
        val configuration = VoiceInputLanguagePolicy.configuration(
            selectedLanguage(context),
            Build.VERSION.SDK_INT
        )
        if (configuration.automaticSwitch) {
            // Start in Nigerian English, while allowing supported recognizers to
            // switch to Hausa for the current utterance.
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, ENGLISH_NIGERIA)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, ENGLISH_NIGERIA)
            intent.putExtra(
                RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH,
                RecognizerIntent.LANGUAGE_SWITCH_BALANCED
            )
            intent.putStringArrayListExtra(
                RecognizerIntent.EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES,
                ArrayList(configuration.allowedLanguages)
            )
        } else {
            val language = configuration.languageTag ?: ENGLISH_NIGERIA
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
        }
    }
}

internal data class VoiceInputConfiguration(
    val selectedPreference: String,
    val languageTag: String?,
    val automaticSwitch: Boolean,
    val allowedLanguages: List<String>
)

/** Pure policy so supported API-level behavior can be regression-tested. */
internal object VoiceInputLanguagePolicy {
    fun configuration(selected: String, sdkInt: Int): VoiceInputConfiguration =
        when {
            selected == VoiceInputPreferences.HAUSA_NIGERIA ->
                VoiceInputConfiguration(
                    selectedPreference = VoiceInputPreferences.HAUSA_NIGERIA,
                    languageTag = VoiceInputPreferences.HAUSA_NIGERIA,
                    automaticSwitch = false,
                    allowedLanguages = emptyList()
                )

            selected == VoiceInputPreferences.AUTOMATIC && sdkInt >= 34 ->
                VoiceInputConfiguration(
                    selectedPreference = VoiceInputPreferences.AUTOMATIC,
                    languageTag = null,
                    automaticSwitch = true,
                    allowedLanguages = listOf(
                        VoiceInputPreferences.ENGLISH_NIGERIA,
                        VoiceInputPreferences.HAUSA_NIGERIA
                    )
                )

            else ->
                VoiceInputConfiguration(
                    selectedPreference = VoiceInputPreferences.ENGLISH_NIGERIA,
                    languageTag = VoiceInputPreferences.ENGLISH_NIGERIA,
                    automaticSwitch = false,
                    allowedLanguages = emptyList()
                )
        }
}
