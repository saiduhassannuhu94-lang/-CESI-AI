package com.cesi.assistant.core.task

import android.content.Context
import com.cesi.assistant.core.action.ActionRouter
import com.cesi.assistant.core.intent.AssistantIntent
import com.cesi.assistant.core.intent.IntentEngine

/**
 * Plans and executes a user's request as a sequence of understood actions.
 *
 * For dependent app tasks, the engine collapses the redundant launch step when
 * the following action already identifies the destination app. This prevents
 * a race where Android is still opening an app while the next intent is fired.
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

        val planned = optimizeDependentSteps(intents)

        var lastResult = "An kammala aikin."
        for ((index, intent) in planned.withIndex()) {
            lastResult = router.route(intent)
            if (isFailure(lastResult)) {
                return "Na tsaya a mataki na " + (index + 1) + ": " + lastResult
            }
        }
        return lastResult
    }

    private fun optimizeDependentSteps(intents: List<AssistantIntent>): List<AssistantIntent> {
        if (intents.size < 2) return intents

        val result = mutableListOf<AssistantIntent>()
        var index = 0

        while (index < intents.size) {
            val current = intents[index]
            val next = intents.getOrNull(index + 1)

            // "Open YouTube and then search for X" is one logical YouTube task.
            // Executing both independently can race; YouTubeSearch already targets
            // the destination, so the explicit launch step is redundant.
            if (current is AssistantIntent.AppLaunch &&
                current.appName.equals("youtube", ignoreCase = true) &&
                next is AssistantIntent.YouTubeSearch
            ) {
                result += next
                index += 2
                continue
            }

            result += current
            index++
        }

        return result
    }

    private fun splitSteps(input: String): List<String> =
        input.trim()
            .split(Regex("""\\s+(?:sannan|sai|daga nan|then|and then|after that)\\s+""", RegexOption.IGNORE_CASE))
            .map(String::trim)
            .filter(String::isNotBlank)

    private fun isFailure(result: String): Boolean =
        result.startsWith("Ban iya") ||
        result.startsWith("Ban sami") ||
        result.startsWith("Ban gane") ||
        result.startsWith("Ban samu") ||
        result.startsWith("Ina bukatar")
}