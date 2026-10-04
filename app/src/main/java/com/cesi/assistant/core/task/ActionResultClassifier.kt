package com.cesi.assistant.core.task

/**
 * Converts the current Android router's user-facing outcome into a structured
 * execution state. This is a migration boundary while individual executors
 * are being upgraded to return ExecutionResult directly.
 */
object ActionResultClassifier {
    fun classify(message: String): ExecutionResult {
        val normalized = message.trim()

        if (normalized.isBlank()) {
            return ExecutionResult(
                status = ExecutionStatus.FAILED,
                message = "Ba a samu sakamakon execution ba.",
                retryable = true
            )
        }

        if (normalized.contains("permission", ignoreCase = true) ||
            normalized.contains("danna Allow", ignoreCase = true)
        ) {
            return ExecutionResult(
                status = ExecutionStatus.BLOCKED,
                message = normalized,
                retryable = true
            )
        }

        if (normalized.startsWith("Ban iya") ||
            normalized.startsWith("Ban sami") ||
            normalized.startsWith("Ban gane") ||
            normalized.startsWith("Ban samu") ||
            normalized.startsWith("Ina bukatar")
        ) {
            return ExecutionResult(
                status = ExecutionStatus.FAILED,
                message = normalized,
                retryable = true
            )
        }

        return ExecutionResult(
            status = ExecutionStatus.SUCCESS,
            message = normalized
        )
    }
}
