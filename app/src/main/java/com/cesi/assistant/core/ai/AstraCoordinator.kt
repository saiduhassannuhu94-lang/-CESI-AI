package com.cesi.assistant.core.ai

import com.cesi.assistant.core.task.AssistantRequest
import com.cesi.assistant.core.task.TaskPlan

/**
 * Coordinates AI reasoning with the locally validated plan contract.
 *
 * It never executes a plan. Android-side execution remains in the existing
 * execution pipeline and must re-check permissions, confirmation and state.
 */
sealed interface AstraCoordinationResult {
    data class Reply(val text: String) : AstraCoordinationResult
    data class NeedsClarification(val question: String, val reason: String?) : AstraCoordinationResult
    data class PlanReady(val reply: String, val plan: TaskPlan) : AstraCoordinationResult
    data class PlanRejected(val reply: String, val reason: String) : AstraCoordinationResult
    data class Unavailable(val reason: AstraUnavailableReason) : AstraCoordinationResult
}

class AstraCoordinator(
    private val brain: CesiBrain,
    private val validator: AstraProposalValidator = AstraProposalValidator()
) {
    fun handle(request: AssistantRequest, context: List<AstraTurn> = emptyList()): AstraCoordinationResult {
        val result = try {
            brain.analyze(request, context)
        } catch (_: Exception) {
            return AstraCoordinationResult.Unavailable(AstraUnavailableReason.PROVIDER_ERROR)
        }

        return when (result) {
            is AstraResult.Conversation -> AstraCoordinationResult.Reply(result.reply)
            is AstraResult.Clarification -> AstraCoordinationResult.NeedsClarification(
                result.question,
                result.reason
            )
            is AstraResult.Unavailable -> AstraCoordinationResult.Unavailable(result.reason)
            is AstraResult.ActionProposal -> when (val validated = validator.validate(result)) {
                is AstraProposalValidation.Accepted -> AstraCoordinationResult.PlanReady(
                    reply = result.reply,
                    plan = validated.plan
                )
                is AstraProposalValidation.Rejected -> AstraCoordinationResult.PlanRejected(
                    reply = result.reply,
                    reason = validated.reason
                )
            }
        }
    }
}
