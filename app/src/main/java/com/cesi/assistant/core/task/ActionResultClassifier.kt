package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent
import java.util.Locale

/**
 * Transitional adapter for executors that still return display strings.
 *
 * This is deliberately conservative: a response is not successful just because
 * it does not begin with a short list of error phrases. The intent and known
 * executor response contract must both match. Unknown wording is UNKNOWN, not
 * success. As executors are migrated, this adapter should be deleted.
 */
object ActionResultClassifier {
    fun classify(message: String): ExecutionResult = classify(message, intent = null)

    fun classify(message: String, intent: AssistantIntent?): ExecutionResult {
        val normalized = message.trim()
        if (normalized.isBlank()) {
            return ExecutionResult(
                status = ExecutionStatus.FAILED,
                message = "Ba a samu sakamakon execution ba.",
                retryable = true
            )
        }

        val lower = normalized.lowercase(Locale.ROOT)
            .replace('’', '\'')

        // Permission-gated responses have to be handled before positive prefixes
        // such as "Na buɗe", because the app may only have opened the permission UI.
        if (isPermissionBlocked(lower)) {
            return ExecutionResult(
                status = ExecutionStatus.BLOCKED,
                message = normalized,
                retryable = true
            )
        }

        if (isKnownFailure(lower)) {
            return ExecutionResult(
                status = ExecutionStatus.FAILED,
                message = normalized,
                retryable = isRetryableFailure(lower)
            )
        }

        if (intent == AssistantIntent.Unknown) {
            return ExecutionResult(
                status = ExecutionStatus.FAILED,
                message = normalized,
                retryable = false
            )
        }

        val expected = intent?.let { knownOutcome(it, lower) }
            ?: knownGenericOutcome(lower)

        return when (expected) {
            Outcome.SUCCESS -> ExecutionResult(ExecutionStatus.SUCCESS, normalized)
            Outcome.PARTIAL -> ExecutionResult(ExecutionStatus.PARTIAL, normalized)
            Outcome.FAILURE -> ExecutionResult(ExecutionStatus.FAILED, normalized, retryable = true)
            Outcome.UNKNOWN -> ExecutionResult(
                status = ExecutionStatus.UNKNOWN,
                message = "Ban iya tabbatar da sakamakon aikin ba. Sakamakon executor: $normalized",
                retryable = false
            )
        }
    }

    private enum class Outcome { SUCCESS, PARTIAL, FAILURE, UNKNOWN }

    private fun isPermissionBlocked(lower: String): Boolean =
        lower.contains("ka danna allow") ||
        lower.contains("danna allow") ||
        lower.startsWith("ina bukatar permission") ||
        lower.startsWith("ina bukatar izini") ||
        lower.startsWith("na bude permission") ||
        lower.startsWith("na buɗe permission") ||
        lower.startsWith("ba a ba cesi permission") ||
        lower.startsWith("permission na ") ||
        lower.startsWith("permission ba")

    private fun isKnownFailure(lower: String): Boolean {
        val startsWithFailure = listOf(
            "ban iya",
            "ban sami",
            "ban gane",
            "ban samu",
            "ban da wani topic",
            "ba a ba cesi",
            "ba a iya",
            "an samu matsala",
            "whatsapp yana bukatar internet",
            "me kake son in",
            "me kake son in bincika",
            "wane app kake son",
            "na gane kana son reaction",
            "na gane kana son sticker",
            "na gane kana son gif",
            "na gane kana son hoton"
        ).any(lower::startsWith)

        return startsWithFailure ||
            lower.contains("android bai bani damar") ||
            lower.contains("ban samu location fix ba") ||
            lower.contains("gps a kashe yake") ||
            lower.contains("location/gps a kashe yake") ||
            lower.contains("ban iya samun location manager") ||
            lower.contains("ban iya karanta battery ba") ||
            lower.contains("ban sami browser") ||
            lower.contains("ban sami youtube") ||
            lower.contains("ban iya buɗe") ||
            lower.contains("ban iya bude") ||
            lower.contains("ban iya yin wannan call") ||
            lower.contains("ban sami lambar") ||
            lower.contains("ban sami contact")
    }

    private fun isRetryableFailure(lower: String): Boolean =
        lower.contains("permission") ||
        lower.contains("izini") ||
        lower.contains("internet") ||
        lower.contains("matsala") ||
        lower.contains("ban iya") ||
        lower.contains("a kashe yake")

    private fun knownOutcome(intent: AssistantIntent, lower: String): Outcome = when (intent) {
        AssistantIntent.FlashlightOn -> if (lower.startsWith("na kunna haske")) Outcome.SUCCESS else Outcome.UNKNOWN
        AssistantIntent.FlashlightOff -> if (lower.startsWith("na kashe haske")) Outcome.SUCCESS else Outcome.UNKNOWN

        AssistantIntent.Selfie ->
            if (lower.startsWithAny("na buɗe selfie camera", "na bude selfie camera")) Outcome.SUCCESS else Outcome.UNKNOWN
        AssistantIntent.Camera ->
            if (lower.startsWithAny("na buɗe camera", "na bude camera")) Outcome.SUCCESS else Outcome.UNKNOWN

        AssistantIntent.Location ->
            if (lower.startsWith("location ɗinka:") || lower.startsWith("location dinka:")) Outcome.SUCCESS else Outcome.UNKNOWN

        is AssistantIntent.Call ->
            if (lower.startsWith("ina kira ")) Outcome.SUCCESS else Outcome.UNKNOWN
        is AssistantIntent.Dial, is AssistantIntent.Ussd ->
            if (lower.startsWithAny("na buɗe dialer", "na bude dialer")) Outcome.PARTIAL else Outcome.UNKNOWN

        is AssistantIntent.ContactSearch ->
            if (':' in lower) Outcome.SUCCESS else Outcome.UNKNOWN
        is AssistantIntent.AppLaunch ->
            if (lower.startsWithAny("na buɗe ", "na bude ")) Outcome.SUCCESS else Outcome.UNKNOWN
        is AssistantIntent.YouTubeSearch ->
            if (lower.startsWithAny("na buɗe youtube na nema:", "na bude youtube na nema:")) Outcome.SUCCESS else Outcome.UNKNOWN
        is AssistantIntent.WebSearch ->
            if (lower.startsWithAny("na buɗe google na bincika:", "na bude google na bincika:")) Outcome.SUCCESS else Outcome.UNKNOWN
        is AssistantIntent.VisualSearch ->
            if (lower.startsWithAny("na buɗe hotunan da suka dace", "na bude hotunan da suka dace")) Outcome.SUCCESS else Outcome.UNKNOWN

        AssistantIntent.VolumeUp ->
            if (lower.startsWithAny("na ƙara sauti", "na kara sauti")) Outcome.SUCCESS else Outcome.UNKNOWN
        AssistantIntent.VolumeDown ->
            if (lower.startsWith("na rage sauti")) Outcome.SUCCESS else Outcome.UNKNOWN
        AssistantIntent.Mute ->
            if (lower.startsWith("na yi shiru")) Outcome.SUCCESS else Outcome.UNKNOWN
        AssistantIntent.BatteryStatus ->
            if (lower.startsWith("battery ɗinka yana kan") || lower.startsWith("battery dinka yana kan")) Outcome.SUCCESS else Outcome.UNKNOWN
        AssistantIntent.Time ->
            if (lower.startsWith("yanzu lokaci ")) Outcome.SUCCESS else Outcome.UNKNOWN
        AssistantIntent.Date ->
            if (lower.startsWith("yau ")) Outcome.SUCCESS else Outcome.UNKNOWN

        AssistantIntent.OpenSettings,
        AssistantIntent.WifiSettings,
        AssistantIntent.BluetoothSettings,
        AssistantIntent.SoundSettings,
        AssistantIntent.DisplaySettings,
        AssistantIntent.NotificationSettings ->
            if (lower.startsWithAny("na buɗe ", "na bude ")) Outcome.SUCCESS else Outcome.UNKNOWN

        is AssistantIntent.SetAlarm ->
            if (lower.startsWithAny("na buɗe alarm", "na bude alarm")) Outcome.PARTIAL else Outcome.UNKNOWN
        is AssistantIntent.Message ->
            if (lower.startsWithAny("na shirya saƙon whatsapp", "na shirya sakon whatsapp")) Outcome.PARTIAL else Outcome.UNKNOWN
        is AssistantIntent.MessengerMessage ->
            if (lower.startsWithAny("na buɗe messenger", "na bude messenger", "na shirya saƙon zuwa", "na shirya sakon zuwa")) Outcome.PARTIAL else Outcome.UNKNOWN
        is AssistantIntent.Reply ->
            when {
                lower.startsWith("na tura reply kai tsaye ta whatsapp notification") -> Outcome.SUCCESS
                lower.startsWithAny("na shirya reply", "na buɗe whatsapp", "na bude whatsapp") -> Outcome.PARTIAL
                else -> Outcome.UNKNOWN
            }

        is AssistantIntent.Advice ->
            if (lower.isNotBlank()) Outcome.SUCCESS else Outcome.UNKNOWN

        is AssistantIntent.TopicFollowUp ->
            if (lower.startsWithAny("na buɗe ", "na bude ")) Outcome.SUCCESS else Outcome.UNKNOWN

        AssistantIntent.Unknown -> Outcome.FAILURE
    }

    private fun knownGenericOutcome(lower: String): Outcome = when {
        lower.startsWithAny(
            "na kunna haske", "na kashe haske",
            "na buɗe ", "na bude ",
            "na shirya ", "na tura reply kai tsaye",
            "ina kira ", "yanzu lokaci ", "yau ",
            "battery ɗinka yana kan", "battery dinka yana kan",
            "location ɗinka:", "location dinka:",
            "na ƙara sauti", "na kara sauti", "na rage sauti", "na yi shiru"
        ) -> Outcome.SUCCESS
        else -> Outcome.UNKNOWN
    }

    private fun String.startsWithAny(vararg prefixes: String): Boolean =
        prefixes.any { startsWith(it) }
}
