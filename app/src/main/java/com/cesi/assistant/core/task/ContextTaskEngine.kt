package com.cesi.assistant.core.task

import android.content.Context
import android.content.Intent
import com.cesi.assistant.core.action.ActionRouter
import com.cesi.assistant.core.intent.AssistantIntent
import com.cesi.assistant.core.intent.IntentEngine
import com.cesi.assistant.core.memory.ConversationContextStore
import com.cesi.assistant.features.messaging.MessageAction
import com.cesi.assistant.features.messaging.MessageCopilotEngine
import com.cesi.assistant.features.messaging.MessageCopilotParser
import com.cesi.assistant.features.notifications.CesiNotificationListenerService
import com.cesi.assistant.features.notifications.CesiWhatsAppAccessibilityService
import com.cesi.assistant.features.notifications.WhatsAppAutomationDisclosureActivity
import com.cesi.assistant.features.notifications.WhatsAppReactionCoordinator

/**
 * Plans and executes a user's request as a sequence of understood actions.
 *
 * Planning is conservative: unknown tasks stop instead of silently disappearing.
 * Android side effects remain delegated to explicit executors and are verified
 * separately where the platform exposes a meaningful result.
 */
class ContextTaskEngine(private val context: Context) {
    private val intentEngine = IntentEngine()
    private val taskPlanner = TaskPlanner()
    private val router = ActionRouter(context)
    private val verificationEvaluator = ActionVerificationEvaluator()
    private val conversationContext = ConversationContextStore(context)
    private val messageCopilot = MessageCopilotEngine()
    private val confirmationManager = TaskConfirmationManager()

    fun execute(input: String): String {
        when (val confirmation = confirmationManager.resolve(input)) {
            is ConfirmationResolution.Confirmed ->
                return executePlannedIntents(confirmation.intents, confirmation.sourceText)
            ConfirmationResolution.Declined -> return "To, ba zan aiwatar da aikin ba."
            ConfirmationResolution.Expired ->
                return "Lokacin tabbatarwar ya wuce. Ba zan aiwatar da aikin da ya gabata ba."
            ConfirmationResolution.Cancelled -> return "An soke aikin da ke jiran tabbatarwa."
            is ConfirmationResolution.StillPending -> return confirmation.message
            ConfirmationResolution.NoPending -> Unit
        }

        messageCopilot.parseCommand(input)?.let { action ->
            return executeMessageCopilotAction(action)
        }

        val steps = splitSteps(input)
        if (steps.isEmpty()) return "Ban ji umarnin ba."

        val resolvedSteps = steps.map { resolveFollowUp(it) }
        val intents = resolvedSteps.map { intentEngine.understand(it) }
        val unknownIndex = intents.indexOfFirst { it == AssistantIntent.Unknown }
        if (unknownIndex >= 0) {
            return "Na tsaya a mataki na " + (unknownIndex + 1) +
                " saboda ban gane: " + steps[unknownIndex]
        }

        val plannedIntents = optimizeDependentSteps(intents)
        return when (val gateResult = confirmationManager.prepare(plannedIntents, input)) {
            is TaskExecutionResult.ConfirmationRequired -> gateResult.message
            is TaskExecutionResult.Failed -> gateResult.message
            is TaskExecutionResult.Completed -> executePlannedIntents(plannedIntents, input)
        }
    }

    private fun executePlannedIntents(intents: List<AssistantIntent>, sourceText: String): String {
        val taskPlan = taskPlanner.planTask(intents)
        val completedMessages = mutableListOf<String>()
        for ((index, plannedAction) in taskPlan.actions.withIndex()) {
            val execution = router.routeResult(plannedAction.intent)
            val verification = verificationEvaluator.evaluate(plannedAction.intent, execution)
            if (execution.status != ExecutionStatus.SUCCESS) {
                val previous = if (completedMessages.isEmpty()) "" else {
                    "Sakamakon matakan da suka gabata: " + completedMessages.joinToString(" ") + " "
                }
                val stopped = when (execution.status) {
                    ExecutionStatus.PARTIAL -> "Mataki na ${index + 1} ya tsaya a matakin shiri kawai: ${execution.message}"
                    ExecutionStatus.BLOCKED -> "An toshe mataki na ${index + 1}; ba a aiwatar da shi ba: ${execution.message}"
                    ExecutionStatus.UNKNOWN -> "Ban iya tabbatar da sakamakon mataki na ${index + 1} ba: ${execution.message}"
                    ExecutionStatus.NEEDS_CONFIRMATION -> "Mataki na ${index + 1} yana jiran tabbatarwa: ${execution.message}"
                    ExecutionStatus.FAILED -> "Mataki na ${index + 1} ya gaza: ${execution.message}"
                    ExecutionStatus.SUCCESS -> execution.message
                }
                return previous + stopped
            }
            completedMessages += formatExecutionMessage(execution, verification)
            rememberIntent(plannedAction.intent)
        }

        conversationContext.rememberCommand(sourceText)
        return if (completedMessages.size == 1) completedMessages.single() else {
            "An aiwatar da matakan da aka shirya. " + completedMessages.mapIndexed { index, message ->
                "${index + 1}) $message"
            }.joinToString(" ")
        }
    }

    private fun formatExecutionMessage(
        execution: ExecutionResult,
        verification: VerificationResult
    ): String = when (verification.status) {
        VerificationStatus.VERIFIED, VerificationStatus.SURFACE_OPENED -> execution.message
        VerificationStatus.PARTIAL -> "Aikin bai kammala ba tukuna: ${execution.message}"
        VerificationStatus.NOT_CHECKED -> "${execution.message} Ba a tabbatar da sakamakon ƙarshe ba."
        VerificationStatus.FAILED -> "Ba a tabbatar da nasarar aikin ba: ${execution.message}"
    }

    private fun executeMessageCopilotAction(action: MessageAction): String = when (action) {
        MessageAction.Suggest -> {
            val current = messageCopilot.latest()
                ?: return "Babu sabon WhatsApp message da nake da context yanzu."
            val suggestions = MessageCopilotParser.suggestionsFor(current.message)
            if (suggestions.isEmpty()) {
                "Na karɓi sabon saƙo, amma ban sami shawarar amsa da ta dace ba. Ka faɗi amsar da kake son tsara."
            } else {
                "Ga amsoshin da za ka iya amfani da su: " +
                    suggestions.mapIndexed { index, value -> "${index + 1}) $value" }.joinToString("  ")
            }
        }
        MessageAction.Ignore -> "To, ba zan tura reply ba."
        is MessageAction.TextReply -> {
            val current = messageCopilot.latest()
                ?: return "Ban san wanda zan reply wa ba. Ka fara karɓar saƙon daga chat ɗin."
            val gateResult = confirmationManager.prepare(
                listOf(AssistantIntent.Reply(action.text)),
                "reply: " + current.sender
            )
            when (gateResult) {
                is TaskExecutionResult.ConfirmationRequired -> gateResult.message
                is TaskExecutionResult.Failed -> gateResult.message
                is TaskExecutionResult.Completed -> router.route(AssistantIntent.Reply(action.text))
            }
        }
        is MessageAction.React -> executeWhatsAppReaction(action.emoji)
        MessageAction.Sticker ->
            "Na gane kana son sticker reply, amma CESI bai haɗa hanyar aika sticker ba tukuna. Ba zan yi kamar an aika shi ba."
        MessageAction.Gif ->
            "Na gane kana son GIF reply, amma CESI bai haɗa hanyar aika GIF ba tukuna. Ba zan yi kamar an aika shi ba."
        is MessageAction.Image ->
            "Na gane kana son hoton “${action.query}”, amma nemo hoto da aika shi a WhatsApp matakai ne daban. Wannan hanyar ba ta haɗu ba tukuna."
    }

    private fun executeWhatsAppReaction(emoji: String): String {
        val current = messageCopilot.latest()
            ?: return "Ban da sabon saƙon WhatsApp da zan yi reaction a kansa."

        if (!WhatsAppAutomationDisclosureActivity.hasConsent(context) ||
            !CesiWhatsAppAccessibilityService.isEnabled(context)
        ) {
            return try {
                context.startActivity(
                    Intent(context, WhatsAppAutomationDisclosureActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                "Na buɗe bayanin izinin WhatsApp reaction. Idan ka amince kuma ka kunna izinin CESI, ka sake ba da umarnin reaction ɗin."
            } catch (_: Exception) {
                "Ba zan iya buɗe saitin izinin reaction ba."
            }
        }

        if (!WhatsAppReactionCoordinator.begin(current.message, emoji)) {
            return "Ban iya shirya reaction ɗin ba saboda bayanin saƙon bai cika ba."
        }
        if (!CesiNotificationListenerService.openLatestWhatsAppConversationForReaction()) {
            WhatsAppReactionCoordinator.clear()
            return "Ban sami hanyar buɗe chat ɗin daga sabon WhatsApp notification ba. Ka sake karɓar notification ɗin, sannan ka sake gwadawa."
        }
        return "Na buɗe WhatsApp don ƙoƙarin saka reaction $emoji a saƙon da ya dace. Duba chat ɗin don tabbatar; ban tabbatar da an aika shi ba."
    }

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
        ) return "open $lastApp"
        if (lastSearch != null && normalized in setOf(
                "search that", "search it", "bincika hakan", "nemo hakan"
            )
        ) return "search $lastSearch"
        if (lastSearch != null && normalized in setOf(
                "search that on youtube", "search it on youtube",
                "bincika hakan a youtube", "nemo hakan a youtube"
            )
        ) return "search youtube $lastSearch"
        if (lastContact != null && normalized in setOf(
                "call him", "call her", "call them", "kira shi", "kira ta"
            )
        ) return "call $lastContact"
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
}
