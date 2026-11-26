package com.cesi.assistant.core.ai

import com.cesi.assistant.core.task.ActionType
import com.cesi.assistant.core.task.AssistantRequest
import com.cesi.assistant.core.task.TaskRisk

/**
 * Provider-neutral contract for CESI's AI reasoning layer.
 *
 * Astra may interpret and propose; it never executes Android actions.
 * All proposals must be revalidated by CESI's local capability registry,
 * risk policy, confirmation gate, action router, and verification layer.
 */
interface CesiBrain {
    fun analyze(request: AssistantRequest, context: List<AstraTurn> = emptyList()): AstraResult
}

data class AstraTurn(
    val role: AstraRole,
    val text: String
)

enum class AstraRole { USER, ASSISTANT }

sealed interface AstraResult {
    data class Conversation(
        val reply: String,
        val confidence: Float = 1f
    ) : AstraResult

    data class ActionProposal(
        val reply: String,
        val proposals: List<AstraActionProposal>,
        val confidence: Float
    ) : AstraResult

    data class Clarification(
        val question: String,
        val reason: String? = null
    ) : AstraResult

    data class Unavailable(
        val reason: AstraUnavailableReason
    ) : AstraResult
}

data class AstraActionProposal(
    val action: ActionType,
    val parameters: Map<String, String> = emptyMap(),
    val risk: TaskRisk = TaskRisk.NONE,
    val explanation: String? = null
)

enum class AstraUnavailableReason {
    OFFLINE,
    NOT_CONFIGURED,
    AUTHENTICATION_REQUIRED,
    RATE_LIMITED,
    PROVIDER_ERROR,
    INVALID_RESPONSE
}
