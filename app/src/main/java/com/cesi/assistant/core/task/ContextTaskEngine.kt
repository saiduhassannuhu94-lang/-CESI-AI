package com.cesi.assistant.core.task

import android.content.Context
import com.cesi.assistant.core.action.ActionRouter
import com.cesi.assistant.core.intent.AssistantIntent
import com.cesi.assistant.core.intent.IntentEngine

/**
 * Plans and executes a user's request as a sequence of understood actions.
 */
class ContextTaskEngine(context: Context) {
    private val intentEngine = IntentEngine()
    private val router = ActionRouter(context)

    fun execute(input: String): String {
        val steps = splitSteps(input)
        if (steps.isEmpty()) return "Ban ji umarnin ba."

        val intents = steps.map { intentEngine.understand(it) }
        val unknownIndex = intents.indexOfFirst { it == AssistantIntent.Unknown }

        if (unknownIndex >= 0) {
            return "Na tsaya a mataki na " + (unknownIndex + 1) +
                " saboda ban gane: " + steps[unknownIndex]
        }

        var lastResult = "An kammala aikin."
        for (intent in intents) {
            lastResult = router.route(intent)
            if (isFailure(lastResult)) return lastResult
        }
        return lastResult
    }

    private fun splitSteps(input: String): List<String> =
        input.trim()
            .split(Regex("""\s+(?:sannan|sai|daga nan|then|and then|after that)\s+""", RegexOption.IGNORE_CASE))
            .map(String::trim)
            .filter(String::isNotBlank)

    private fun isFailure(result: String): Boolean =
        result.startsWith("Ban iya") ||
        result.startsWith("Ban sami") ||
        result.startsWith("Ban gane") ||
        result.startsWith("Ban samu") ||
        result.startsWith("Ina bukatar")
}
