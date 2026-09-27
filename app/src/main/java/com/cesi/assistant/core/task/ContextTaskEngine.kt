package com.cesi.assistant.core.task

import android.content.Context
import com.cesi.assistant.core.action.ActionRouter
import com.cesi.assistant.core.intent.AssistantIntent
import com.cesi.assistant.core.intent.IntentEngine
import com.cesi.assistant.core.memory.ConversationContextStore

/**
 * Plans and executes a user's request as a sequence of understood actions.
 *
 * In addition to multi-step execution, this engine keeps a small local
 * conversation context so follow-up commands such as "open it" or
 * "search that on YouTube" can refer to the immediately previous task.
 */
class ContextTaskEngine(context: Context) {
    private val intentEngine = IntentEngine()
    private val router = ActionRouter(context)
    private val conversationContext = ConversationContextStore(context)

    fun execute(input: String): String {
        val steps = splitSteps(input)
        if (steps.isEmpty()) return "Ban ji umarnin ba."

        val resolvedSteps = steps.map { resolveFollowUp(it) }
        val intents = resolvedSteps.map { intentEngine.understand(it) }
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
            rememberIntent(intent)
        }

        conversationContext.rememberCommand(input)
        return lastResult
    }

    /**
     * Resolves short natural follow-ups against the latest local context.
     * We only rewrite unambiguous references; ordinary commands are untouched.
     */
    private fun resolveFollowUp(step: String): String {
        val normalized = step.trim().lowercase()
        if (normalized.isBlank()) return step

        val lastApp = conversationContext.lastApp
        val lastSearch = conversationContext.lastSearch
        val lastContact = conversationContext.lastContact

        if (lastApp != null && normalized in setOf(
                "open it", "launch it", "start it", "bude shi", "buɗe shi",
                "bude app din", "buɗe app din"
            )
        ) {
            return "open $lastApp"
        }

        if (lastSearch != null && normalized in setOf(
                "search that", "search it", "bincika hakan", "nemo hakan"
            )
        ) {
            return "search $lastSearch"
        }

        if (lastSearch != null && normalized in setOf(
                "search that on youtube", "search it on youtube",
                "bincika hakan a youtube", "nemo hakan a youtube"
            )
        ) {
            return "search youtube $lastSearch"
        }

        if (lastContact != null && normalized in setOf(
                "call him", "call her", "call them", "kira shi", "kira ta"
            )
        ) {
            return "call $lastContact"
        }

        return step
    }

    private fun rememberIntent(intent: AssistantIntent) {
        when (intent) {
            is AssistantIntent.AppLaunch -> conversationContext.rememberApp(intent.appName)
            is AssistantIntent.WebSearch -> conversationContext.rememberSearch(intent.query)
            is AssistantIntent.YouTubeSearch -> conversationContext.rememberSearch(intent.query)
            is AssistantIntent.Call -> conversationContext.rememberContact(intent.target)
            is AssistantIntent.ContactSearch -> conversationContext.rememberContact(intent.query)
            else -> Unit
        }
    }

    private fun optimizeDependentSteps(intents: List<AssistantIntent>): List<AssistantIntent> {
        if (intents.size < 2) return intents

        val result = mutableListOf<AssistantIntent>()
        var index = 0

        while (index < intents.size) {
            val current = intents[index]
            val next = intents.getOrNull(index + 1)

            // "Open YouTube and then search for X" is one logical YouTube task.
            // YouTubeSearch already targets the destination, so the explicit launch
            // step is redundant and can race with the search intent.
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
