package com.cesi.assistant.core.task

/**
 * Structured outcome of an attempted action.
 *
 * This contract deliberately contains no Android types so the same model can
 * be consumed by the APK and the browser test lab.
 */
data class ExecutionResult(
    val status: ExecutionStatus,
    val message: String,
    val retryable: Boolean = false
)

enum class ExecutionStatus {
    /** The executor accepted/completed the requested operation. Verification is separate. */
    SUCCESS,
    /** The executor prepared or opened the next step, but the user must finish it. */
    PARTIAL,
    FAILED,
    NEEDS_CONFIRMATION,
    BLOCKED,
    /** The legacy adapter could not safely establish whether the operation executed. */
    UNKNOWN
}

/**
 * Observable verification outcome after an action has been attempted.
 *
 * NOT_CHECKED means no reliable post-action signal is available. It must not
 * be treated as proof that the action succeeded.
 */
data class VerificationResult(
    val status: VerificationStatus,
    val message: String
)

enum class VerificationStatus {
    VERIFIED,
    FAILED,
    NOT_CHECKED
}
