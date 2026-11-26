package com.cesi.assistant.core.ai

import com.cesi.assistant.core.task.ActionType
import com.cesi.assistant.core.task.TaskRisk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CesiBrainContractTest {
    @Test
    fun actionProposalIsOnlyTypedIntentAndDoesNotRepresentExecution() {
        val proposal = AstraActionProposal(
            action = ActionType.OPEN_APP,
            parameters = mapOf("app" to "WhatsApp"),
            risk = TaskRisk.NONE,
            explanation = "Open the messaging app"
        )
        val result = AstraResult.ActionProposal(
            reply = "I'll open WhatsApp.",
            proposals = listOf(proposal),
            confidence = 0.91f
        )

        assertTrue(result is AstraResult.ActionProposal)
        assertEquals(ActionType.OPEN_APP, proposal.action)
        assertEquals("WhatsApp", proposal.parameters["app"])
        assertEquals(0.91f, (result as AstraResult.ActionProposal).confidence)
    }

    @Test
    fun unavailableReasonsAreExplicitAndCanDriveFallback() {
        val result = AstraResult.Unavailable(AstraUnavailableReason.OFFLINE)
        assertEquals(AstraUnavailableReason.OFFLINE, (result as AstraResult.Unavailable).reason)
    }

    @Test
    fun clarificationDoesNotCarryAnActionProposal() {
        val result = AstraResult.Clarification("Which contact do you mean?")
        assertEquals("Which contact do you mean?", (result as AstraResult.Clarification).question)
    }
}
