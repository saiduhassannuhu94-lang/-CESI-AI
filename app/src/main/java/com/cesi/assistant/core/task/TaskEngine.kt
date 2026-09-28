package com.cesi.assistant.core.task

import android.content.Context
import com.cesi.assistant.core.action.ActionRouter
import com.cesi.assistant.core.intent.AssistantIntent
import com.cesi.assistant.core.intent.IntentEngine

/** Executes understood intents sequentially; planner expansion will be added without changing voice/permission flow. */
class TaskEngine(context: Context) {
    private val intentEngine = IntentEngine()
    private val router = ActionRouter(context)

    fun execute(input: String): String {
        val original = input.trim()
        if (original.isBlank()) return "Ban ji umarnin ba."

        val steps = splitSteps(original)
        if (steps.size == 1) return router.route(intentEngine.understand(original))

        val results = mutableListOf<String>()
        for (step in steps) {
            val intent = intentEngine.understand(step)
            if (intent == AssistantIntent.Unknown) {
                return "Na tsaya a mataki na \${results.size + 1} saboda ban gane: $step"
            }
            val result = router.route(intent)
            results += result
            if (result.startsWith("Ban iya") || result.startsWith("Ban sami") ||
                result.startsWith("Ban gane") || result.startsWith("Ban samu")) return result
        }
        return results.lastOrNull() ?: "An kammala aikin."
    }

    private fun splitSteps(input: String): List<String> =
        input.trim().split(
            Regex("""\s+(?:sannan|sai|daga nan|then|and then|after that)\s+""", RegexOption.IGNORE_CASE)
        ).map { it.trim() }.filter { it.isNotBlank() }
}
