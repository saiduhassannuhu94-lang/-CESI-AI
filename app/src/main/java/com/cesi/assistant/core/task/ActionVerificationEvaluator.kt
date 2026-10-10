package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent

/**
 * Converts an execution outcome plus the capability's declared verification
 * limits into an explicit verification status.
 *
 * This class never upgrades UNKNOWN, BLOCKED, NEEDS_CONFIRMATION, or a UI-only
 * handoff to VERIFIED. Verification limits are part of the result contract,
 * not hidden in user-facing wording.
 */
class ActionVerificationEvaluator(
    private val profiles: ActionExecutionProfileRegistry = ActionExecutionProfileRegistry()
) {
    fun evaluate(intent: AssistantIntent, execution: ExecutionResult): VerificationResult {
        return when (execution.status) {
            ExecutionStatus.SUCCESS -> when (profiles.profileFor(intent).verification) {
                VerificationMode.ACTION_RESULT -> VerificationResult(
                    status = VerificationStatus.VERIFIED,
                    message = "Executor returned a recognized successful result; this is not independent device-state readback."
                )

                VerificationMode.UI_OPEN_ONLY -> VerificationResult(
                    status = VerificationStatus.SURFACE_OPENED,
                    message = "The requested UI surface was opened. Content loading or any further effect was not independently checked."
                )

                VerificationMode.EXTERNAL_SIDE_EFFECT -> VerificationResult(
                    status = VerificationStatus.NOT_CHECKED,
                    message = "The external action was initiated, but its final outcome is not observable here."
                )

                VerificationMode.NOT_AVAILABLE -> VerificationResult(
                    status = VerificationStatus.NOT_CHECKED,
                    message = "No reliable post-action verification signal is available."
                )
            }

            ExecutionStatus.PARTIAL -> VerificationResult(
                status = VerificationStatus.PARTIAL,
                message = "The executor prepared or opened the next step; the requested task is not complete yet."
            )

            ExecutionStatus.FAILED -> VerificationResult(
                status = VerificationStatus.FAILED,
                message = "The executor reported that the action failed."
            )

            ExecutionStatus.BLOCKED -> VerificationResult(
                status = VerificationStatus.NOT_CHECKED,
                message = "Execution was blocked before the requested action completed."
            )

            ExecutionStatus.NEEDS_CONFIRMATION -> VerificationResult(
                status = VerificationStatus.NOT_CHECKED,
                message = "The action is awaiting explicit confirmation."
            )

            ExecutionStatus.UNKNOWN -> VerificationResult(
                status = VerificationStatus.NOT_CHECKED,
                message = "The result is ambiguous; no success claim is justified."
            )
        }
    }
}
