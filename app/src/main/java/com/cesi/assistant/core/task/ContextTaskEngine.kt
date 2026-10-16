package com.cesi.assistant.core.task

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import com.cesi.assistant.features.notifications.WhatsAppQuotedReplyCoordinator

private const val QUOTED_REPLY_SOURCE_PREFIX = "quoted-whatsapp-reply:"

/**
 * Plans and executes a user's request as a sequence of understood actions.
 *
 * In addition to multi-step execution, this engine keeps a small local
 * conversation context so follow-up commands such as "open it" or
 * "search that on YouTube" can refer to the immediately previous task.
 *
 * Planning is platform-neutral; Android execution remains delegated to the
 * existing ActionRouter until each capability has a verified executor.
 */
class ContextTaskEngine(private val context: Context) {
    private val intentEngine = IntentEngine()
    private val taskPlanner = TaskPlanner()
    private val multiTaskPlanner = MultiTaskPlanner()
    private val router = ActionRouter(context)
    private val verificationEvaluator = ActionVerificationEvaluator()
    private val conversationContext = ConversationContextStore(context)
    private val messageCopilot = MessageCopilotEngine()
    private val confirmationManager = TaskConfirmationManager()

    fun execute(input: String): String {
        when (val confirmation = confirmationManager.resolve(
            input,
            isNewCommand = confirmationManager.hasPending() && isNewCommand(input)
        )) {
            is ConfirmationResolution.Confirmed -> {
                if (confirmation.sourceText.startsWith(QUOTED_REPLY_SOURCE_PREFIX)) {
                    return startConfirmedQuotedReply(
                        confirmation.sourceText.removePrefix(QUOTED_REPLY_SOURCE_PREFIX)
                    )
                }
                return executePlannedIntents(confirmation.intents, confirmation.sourceText)
            }

            ConfirmationResolution.Declined -> {
                WhatsAppQuotedReplyCoordinator.clear()
                return "To, ba zan aiwatar da aikin ba."
            }

            ConfirmationResolution.Expired -> {
                WhatsAppQuotedReplyCoordinator.clear()
                return "Lokacin tabbatarwar ya wuce. Ba zan aiwatar da aikin da ya gabata ba."
            }

            ConfirmationResolution.Cancelled -> {
                WhatsAppQuotedReplyCoordinator.clear()
                return "An soke aikin da ke jiran tabbatarwa."
            }

            ConfirmationResolution.NotAConfirmation -> {
                WhatsAppQuotedReplyCoordinator.clear()
            }

            is ConfirmationResolution.StillPending ->
                return confirmation.message

            ConfirmationResolution.NoPending -> Unit
        }

        messageCopilot.parseCommand(input)?.let { action ->
            return executeMessageCopilotAction(action)
        }

        val steps = multiTaskPlanner.split(input)
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
            is TaskExecutionResult.Completed -> {
                executePlannedIntents(plannedIntents, input)
            }
        }
    }

    private fun executePlannedIntents(intents: List<AssistantIntent>, sourceText: String): String {
        val taskPlan = taskPlanner.planTask(intents)
        val completedMessages = mutableListOf<String>()

        for ((index, plannedAction) in taskPlan.actions.withIndex()) {
            val execution = router.routeResult(plannedAction.intent)
            val verification = verificationEvaluator.evaluate(plannedAction.intent, execution)

            if (execution.status != ExecutionStatus.SUCCESS) {
                val previous = if (completedMessages.isEmpty()) {
                    ""
                } else {
                    "Sakamakon matakan da suka gabata: " + completedMessages.joinToString(" ") + " "
                }
                val stopped = when (execution.status) {
                    ExecutionStatus.PARTIAL ->
                        "Mataki na ${index + 1} ya tsaya a matakin shiri kawai: ${execution.message}"
                    ExecutionStatus.BLOCKED ->
                        "An toshe mataki na ${index + 1}; ba a aiwatar da shi ba: ${execution.message}"
                    ExecutionStatus.UNKNOWN ->
                        "Ban iya tabbatar da sakamakon mataki na ${index + 1} ba: ${execution.message}"
                    ExecutionStatus.NEEDS_CONFIRMATION ->
                        "Mataki na ${index + 1} yana jiran tabbatarwa: ${execution.message}"
                    ExecutionStatus.FAILED ->
                        "Mataki na ${index + 1} ya gaza: ${execution.message}"
                    ExecutionStatus.SUCCESS -> execution.message
                }
                return previous + stopped
            }

            completedMessages += formatExecutionMessage(execution, verification)
            rememberIntent(plannedAction.intent)
        }

        conversationContext.rememberCommand(sourceText)
        return if (completedMessages.size == 1) {
            completedMessages.single()
        } else {
            "An aiwatar da matakan da aka shirya. " + completedMessages.mapIndexed { index, message ->
                "${index + 1}) $message"
            }.joinToString(" ")
        }
    }

    private fun formatExecutionMessage(
        execution: ExecutionResult,
        verification: VerificationResult
    ): String = when (verification.status) {
        VerificationStatus.VERIFIED,
        VerificationStatus.SURFACE_OPENED -> execution.message

        VerificationStatus.PARTIAL ->
            "Aikin bai kammala ba tukuna: ${execution.message}"

        VerificationStatus.NOT_CHECKED ->
            "${execution.message} Ba a tabbatar da sakamakon ƙarshe ba."

        VerificationStatus.FAILED ->
            "Ba a tabbatar da nasarar aikin ba: ${execution.message}"
    }

    private fun executeMessageCopilotAction(action: MessageAction): String = when (action) {
        MessageAction.Suggest -> {
            val context = messageCopilot.latest()
                ?: return missingWhatsAppContext("shawarar reply")
            val suggestions = MessageCopilotParser.suggestionsFor(context.message)
            if (suggestions.isEmpty()) {
                "Na karɓi sabon saƙo, amma ban sami shawarar amsa da ta dace ba. Ka faɗi amsar da kake son tsara."
            } else {
                "Ga amsoshin da za ka iya amfani da su: " +
                    suggestions.mapIndexed { index, value -> "${index + 1}) $value" }.joinToString("  ")
            }
        }

        MessageAction.Ignore -> "To, ba zan tura reply ba."

        MessageAction.InvalidDirectMessage ->
            "Don aika sabon saƙon WhatsApp, yi amfani da lambar ƙasa da tsarin ƙasa kamar +2348012345678. Ban buɗe chat ba saboda lambar ba ta da ingantaccen tsarin ƙasa."

        is MessageAction.DirectMessage -> openWhatsAppDraft(action.phoneNumber, action.text)

        is MessageAction.TextReply -> {
            val context = messageCopilot.latest()
                ?: return missingWhatsAppContext("reply")
            val gateResult = confirmationManager.prepare(
                listOf(AssistantIntent.Reply(action.text)),
                "reply: " + context.sender
            )
            when (gateResult) {
                is TaskExecutionResult.ConfirmationRequired -> gateResult.message
                is TaskExecutionResult.Failed -> gateResult.message
                is TaskExecutionResult.Completed -> {
                    router.routeResult(AssistantIntent.Reply(action.text)).message
                }
            }
        }

        is MessageAction.RememberCaptionStyle -> {
            val store = com.cesi.assistant.features.messaging.WhatsAppCaptionLearningStore(context)
            if (store.remember(action.style)) {
                "Na adana salon caption na ${store.currentStyle().name.lowercase(java.util.Locale.ROOT)} a wannan wayar kawai. Ba na adana saƙonninka ko hotunanka domin wannan."
            } else {
                "Ban gane wannan salon caption ba. Ka zaɓi natural, funny, professional, Islamic, romantic, ko short."
            }
        }

        MessageAction.ClearCaptionStyle -> {
            com.cesi.assistant.features.messaging.WhatsAppCaptionLearningStore(context).clear()
            "Na goge salon caption da aka adana. Zan koma salon natural."
        }

        is MessageAction.QuotedReply -> prepareQuotedReply(action)

        is MessageAction.React -> executeWhatsAppReaction(action.emoji)

        MessageAction.Sticker ->
            "Na gane kana son sticker reply, amma Android notification reply ba ya ba CESI damar aika sticker kai tsaye. Wannan zai bukaci wani execution layer na WhatsApp."

        MessageAction.Gif ->
            "Na gane kana son GIF reply, amma GIF ba a aika shi ta wannan notification-reply channel ba. Ba zan yi fake success ba."

        is MessageAction.Image ->
            "Na gane kana son hoton “${action.query}”, amma nemo hoto da aika shi a WhatsApp mataki biyu ne daban. CESI bai haɗa su kai tsaye ba tukuna."

        is MessageAction.ShareMedia -> openWhatsAppMediaPicker(action)
    }


    private fun openWhatsAppMediaPicker(action: MessageAction.ShareMedia): String {
        return try {
            context.startActivity(
                Intent(context, com.cesi.assistant.features.messaging.WhatsAppMediaShareActivity::class.java).apply {
                    val caption = if (action.captionProvided) {
                        action.caption
                    } else {
                        com.cesi.assistant.features.messaging.WhatsAppCaptionLearningStore(context)
                            .captionFor(action.mediaKind, action.asStatus)
                    }
                    putExtra(com.cesi.assistant.features.messaging.WhatsAppMediaShareActivity.EXTRA_CAPTION, caption)
                    putExtra(com.cesi.assistant.features.messaging.WhatsAppMediaShareActivity.EXTRA_MEDIA_KIND, action.mediaKind)
                    putExtra(com.cesi.assistant.features.messaging.WhatsAppMediaShareActivity.EXTRA_AS_STATUS, action.asStatus)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            if (action.asStatus) {
                "Na buɗe zaɓin media. Zaɓi hoton ko bidiyon, sannan a WhatsApp ka zaɓi My Status ka duba caption kafin ka danna Post."
            } else {
                "Na buɗe zaɓin media. Zaɓi hoton ko bidiyon; WhatsApp zai buɗe da caption. Duba wanda za ka tura wa sannan ka danna Send da kanka."
            }
        } catch (_: Exception) {
            "Ban iya buɗe zaɓin media ba. Babu media da aka tura."
        }
    }

    /** Opens a WhatsApp chat with a prefilled draft; sending always remains a user action. */
    private fun openWhatsAppDraft(phoneNumber: String, message: String): String {
        val uri = Uri.Builder()
            .scheme("https")
            .authority("wa.me")
            .appendPath(phoneNumber)
            .appendQueryParameter("text", message)
            .build()
        val intent = Intent(Intent.ACTION_VIEW, uri)
            .setPackage("com.whatsapp")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) {
            return "Ban sami WhatsApp a wayar ba. Ka girka ko kunna WhatsApp kafin amfani da wannan fasalin."
        }
        return try {
            context.startActivity(intent)
            "Na buɗe chat ɗin WhatsApp da saƙonka a rubuce. Duba lambar da saƙon, sannan ka danna Send da kanka; CESI bai aika saƙon kai tsaye ba."
        } catch (_: Exception) {
            "Na kasa buɗe WhatsApp chat ɗin. Babu saƙon da CESI ya aika."
        }
    }


    private fun prepareQuotedReply(action: MessageAction.QuotedReply): String {
        if (!WhatsAppAutomationDisclosureActivity.hasConsent(context) ||
            !CesiWhatsAppAccessibilityService.isEnabled(context)
        ) {
            return try {
                context.startActivity(
                    Intent(context, WhatsAppAutomationDisclosureActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                "Ka duba bayanin izinin CESI. Idan ka amince, kunna CESI a Accessibility settings, ka dawo ka sake umarnin quoted reply. Ba a aika komai ba tukuna."
            } catch (_: Exception) {
                "CESI na bukatar izinin Accessibility da amincewarka kafin ya iya nemo tsohon saƙo. Babu abin da aka aika."
            }
        }

        if (WhatsAppReactionCoordinator.current() != null) {
            return "Akwai WhatsApp reaction da ke gudana. Ka kammala ko ka soke shi kafin sabon quoted reply."
        }
        if (!WhatsAppQuotedReplyCoordinator.begin(action.targetText, action.replyText)) {
            return "Ba zan iya shirya quoted reply ba saboda saƙon ko amsar babu komai."
        }

        val source = QUOTED_REPLY_SOURCE_PREFIX + action.targetText
        return when (val gateResult = confirmationManager.prepare(
            listOf(AssistantIntent.Reply(action.replyText)),
            source
        )) {
            is TaskExecutionResult.ConfirmationRequired ->
                gateResult.message + " Zan amsa saƙon “${action.targetText}” da “${action.replyText}”. Ka tabbatar ne kawai idan waɗannan kalmomi da chat ɗin da kake nufi sun yi daidai. Bayan ka tabbatar, buɗe chat ɗin kuma ka nuna ainihin saƙon a screen. CESI zai tsaya idan bai sami saƙo guda ɗaya da ya dace ba."

            is TaskExecutionResult.Failed -> {
                WhatsAppQuotedReplyCoordinator.clear()
                gateResult.message
            }

            is TaskExecutionResult.Completed -> startConfirmedQuotedReply(action.targetText)
        }
    }

    private fun startConfirmedQuotedReply(targetText: String): String {
        val pending = WhatsAppQuotedReplyCoordinator.current()
            ?: return "Lokacin quoted reply ya wuce. Ka sake ba da umarnin."
        if (pending.targetText != targetText) {
            WhatsAppQuotedReplyCoordinator.clear()
            return "Saƙon da aka tabbatar bai dace da saƙon da ake jira ba. An soke aikin."
        }
        if (!WhatsAppAutomationDisclosureActivity.hasConsent(context) ||
            !CesiWhatsAppAccessibilityService.isEnabled(context)
        ) {
            WhatsAppQuotedReplyCoordinator.clear()
            return "An buƙaci sake kunna izinin CESI Accessibility. Ba a aika reply ba."
        }
        if (!WhatsAppQuotedReplyCoordinator.authorize()) {
            WhatsAppQuotedReplyCoordinator.clear()
            return "Lokacin quoted reply ya wuce. Ka sake ba da umarnin."
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
            ?: run {
                WhatsAppQuotedReplyCoordinator.clear()
                return "Ban sami WhatsApp a wayar ba. Ba a aika reply ba."
            }
        return try {
            context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "An tabbatar da reply. A WhatsApp, buɗe chat ɗin da ke ɗauke da tsohon saƙon, nemo shi ta ainihin rubutunsa, sannan ka nuna shi a screen cikin minti uku. CESI zai yi reply ne kawai idan ya sami saƙon guda ɗaya daidai."
        } catch (_: Exception) {
            WhatsAppQuotedReplyCoordinator.clear()
            "Ban iya buɗe WhatsApp ba. Ba a aika reply ba."
        }
    }

    private fun executeWhatsAppReaction(emoji: String): String {
        val current = messageCopilot.latest()
            ?: return missingWhatsAppContext("reaction")

        // Register the one-shot request before asking for accessibility access so
        // the user's explicit command can resume after they return from Settings.
        if (!WhatsAppReactionCoordinator.begin(current.message, emoji)) {
            return "Ban iya shirya reaction ɗin ba saboda bayanin saƙon bai cika ba."
        }

        if (!WhatsAppAutomationDisclosureActivity.hasConsent(context) ||
            !CesiWhatsAppAccessibilityService.isEnabled(context)
        ) {
            return try {
                context.startActivity(
                    Intent(context, WhatsAppAutomationDisclosureActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                "Na buɗe bayanin izinin WhatsApp reaction. Idan ka amince, kunna CESI a Accessibility settings sannan ka dawo; zan yi ƙoƙarin ci gaba da wannan reaction ɗin."
            } catch (_: Exception) {
                WhatsAppReactionCoordinator.clear()
                "Ba zan iya buɗe saitin izinin reaction ba."
            }
        }

        if (!CesiNotificationListenerService.openLatestWhatsAppConversationForReaction()) {
            WhatsAppReactionCoordinator.clear()
            return "Ban sami hanyar buɗe chat ɗin daga sabon WhatsApp notification ba. Ka sake karɓar notification ɗin, sannan ka sake gwadawa."
        }

        return "Na buɗe WhatsApp don ƙoƙarin saka reaction $emoji a saƙon da ya dace. Duba chat ɗin don tabbatar; ban tabbatar da an aika shi ba."
    }

    private fun missingWhatsAppContext(action: String): String {
        if (!isNotificationAccessEnabled()) {
            return try {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                "Na buɗe Notification Access. Ka kunna CESI a jerin apps ɗin, sannan ka dawo ka sake umarnin $action."
            } catch (_: Exception) {
                "CESI na bukatar Notification Access domin karɓar saƙon WhatsApp. Ka buɗe Settings > Notification Access ka kunna CESI."
            }
        }
        return "Ban da sabon saƙon WhatsApp a context yanzu. Ka karɓi sabon saƙo ta notification, sannan ka sake umarnin $action."
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        val expected = ComponentName(context, CesiNotificationListenerService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    private fun isNewCommand(input: String): Boolean {
        if (messageCopilot.parseCommand(input) != null) return true
        val steps = multiTaskPlanner.split(input)
        return steps.isNotEmpty() && steps.all { step ->
            intentEngine.understand(resolveFollowUp(step)) != AssistantIntent.Unknown
        }
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

}
