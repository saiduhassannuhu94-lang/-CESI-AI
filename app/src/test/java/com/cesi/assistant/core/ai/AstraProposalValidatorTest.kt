package com.cesi.assistant.core.ai

import com.cesi.assistant.core.task.ActionType
import com.cesi.assistant.core.task.TaskRisk
import org.junit.Assert.*
import org.junit.Test

class AstraProposalValidatorTest {
    private val validator = AstraProposalValidator()

    @Test fun acceptsKnownLocalActionAndBuildsPlanWithoutExecuting() {
        val result = AstraResult.ActionProposal(
            reply = "Turning on flashlight.",
            proposals = listOf(AstraActionProposal(ActionType.TURN_FLASHLIGHT_ON)),
            confidence = 0.95f
        )
        val accepted = validator.validate(result)
        assertTrue(accepted is AstraProposalValidation.Accepted)
        val plan = (accepted as AstraProposalValidation.Accepted).plan
        assertEquals(1, plan.actions.size)
        assertEquals(ActionType.TURN_FLASHLIGHT_ON, plan.actions.single().action.type)
        assertFalse(plan.requiresConfirmation)
    }

    @Test fun rejectsUnknownAction() {
        val result = AstraResult.ActionProposal(
            reply = "Attempting unsupported action.",
            proposals = listOf(AstraActionProposal(ActionType.UNKNOWN)),
            confidence = 0.95f
        )
        assertTrue(validator.validate(result) is AstraProposalValidation.Rejected)
    }

    @Test fun rejectsUnexpectedParametersForParameterlessAction() {
        val result = AstraResult.ActionProposal(
            reply = "Turning on flashlight.",
            proposals = listOf(AstraActionProposal(ActionType.TURN_FLASHLIGHT_ON, mapOf("extra" to "value"))),
            confidence = 0.95f
        )
        assertTrue(validator.validate(result) is AstraProposalValidation.Rejected)
    }

    @Test fun rejectsMissingRequiredContactTarget() {
        val result = AstraResult.ActionProposal(
            reply = "Calling.",
            proposals = listOf(AstraActionProposal(ActionType.CALL_CONTACT, mapOf("target" to " "))),
            confidence = 0.95f
        )
        assertTrue(validator.validate(result) is AstraProposalValidation.Rejected)
    }

    @Test fun recalculatesRiskLocallyAndRequiresConfirmationForCall() {
        val result = AstraResult.ActionProposal(
            reply = "Calling Ahmed.",
            proposals = listOf(AstraActionProposal(ActionType.CALL_CONTACT, mapOf("target" to "Ahmed"), risk = TaskRisk.NONE)),
            confidence = 0.95f
        )
        val accepted = validator.validate(result) as AstraProposalValidation.Accepted
        assertEquals(TaskRisk.EXTERNAL_SIDE_EFFECT, accepted.plan.overallRisk)
        assertTrue(accepted.plan.requiresConfirmation)
    }

    @Test fun rejectsLowConfidence() {
        val result = AstraResult.ActionProposal(
            reply = "Maybe.",
            proposals = listOf(AstraActionProposal(ActionType.GET_TIME)),
            confidence = 0.25f
        )
        assertTrue(validator.validate(result) is AstraProposalValidation.Rejected)
    }

    @Test fun rejectsInvalidAlarmTime() {
        val result = AstraResult.ActionProposal(
            reply = "Setting alarm.",
            proposals = listOf(AstraActionProposal(ActionType.SET_ALARM, mapOf("hour" to "25", "minute" to "10"))),
            confidence = 0.99f
        )
        assertTrue(validator.validate(result) is AstraProposalValidation.Rejected)
    }
}
