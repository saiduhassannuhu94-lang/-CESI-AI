package com.cesi.assistant.core.ai

import com.cesi.assistant.core.task.ActionType
import com.cesi.assistant.core.task.AssistantRequest
import org.junit.Assert.*
import org.junit.Test

class AstraCoordinatorTest {
    private val request = AssistantRequest(
        rawText = "turn on flashlight",
        normalizedText = "turn on flashlight"
    )

    private class FakeBrain(private val response: AstraResult) : CesiBrain {
        var seenRequest: AssistantRequest? = null
        var seenContext: List<AstraTurn>? = null
        override fun analyze(request: AssistantRequest, context: List<AstraTurn>): AstraResult {
            seenRequest = request
            seenContext = context
            return response
        }
    }

    @Test fun conversationalResponseReturnsReply() {
        val brain = FakeBrain(AstraResult.Conversation("Hello there."))
        assertEquals(AstraCoordinationResult.Reply("Hello there."), AstraCoordinator(brain).handle(request))
    }

    @Test fun clarificationIsPreserved() {
        val brain = FakeBrain(AstraResult.Clarification("Which contact?", "Missing recipient"))
        assertEquals(
            AstraCoordinationResult.NeedsClarification("Which contact?", "Missing recipient"),
            AstraCoordinator(brain).handle(request)
        )
    }

    @Test fun unavailableReasonIsPreserved() {
        val brain = FakeBrain(AstraResult.Unavailable(AstraUnavailableReason.OFFLINE))
        assertEquals(
            AstraCoordinationResult.Unavailable(AstraUnavailableReason.OFFLINE),
            AstraCoordinator(brain).handle(request)
        )
    }

    @Test fun acceptedActionProposalProducesPlanButDoesNotExecute() {
        val brain = FakeBrain(
            AstraResult.ActionProposal(
                "Turning on flashlight.",
                listOf(AstraActionProposal(ActionType.TURN_FLASHLIGHT_ON)),
                confidence = 0.95f
            )
        )
        val result = AstraCoordinator(brain).handle(request)
        assertTrue(result is AstraCoordinationResult.PlanReady)
        assertEquals(1, (result as AstraCoordinationResult.PlanReady).plan.actions.size)
    }

    @Test fun rejectedActionProposalReturnsReason() {
        val brain = FakeBrain(
            AstraResult.ActionProposal(
                "Calling someone.",
                listOf(AstraActionProposal(ActionType.CALL_CONTACT, mapOf("target" to ""))),
                confidence = 0.99f
            )
        )
        val result = AstraCoordinator(brain).handle(request)
        assertTrue(result is AstraCoordinationResult.PlanRejected)
    }

    @Test fun brainReceivesRequestAndConversationContext() {
        val context = listOf(AstraTurn(AstraRole.USER, "hello"), AstraTurn(AstraRole.ASSISTANT, "Hi"))
        val brain = FakeBrain(AstraResult.Conversation("context used"))
        AstraCoordinator(brain).handle(request, context)
        assertEquals(request, brain.seenRequest)
        assertEquals(context, brain.seenContext)
    }

    @Test fun brainFailureIsConvertedToUnavailable() {
        val brain = object : CesiBrain {
            override fun analyze(request: AssistantRequest, context: List<AstraTurn>): AstraResult {
                error("provider failure")
            }
        }
        assertEquals(
            AstraCoordinationResult.Unavailable(AstraUnavailableReason.PROVIDER_ERROR),
            AstraCoordinator(brain).handle(request)
        )
    }
}
