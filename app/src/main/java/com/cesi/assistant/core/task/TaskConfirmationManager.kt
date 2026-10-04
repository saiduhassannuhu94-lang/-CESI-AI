package com.cesi.assistant.core.task

import com.cesi.assistant.core.intent.AssistantIntent

enum class ConfirmationState {
    PENDING,
    CONFIRMED,
    DECLINED,
    EXPIRED,
    CANCELLED
}

sealed class ConfirmationResolution {
    data object NoPending : ConfirmationResolution()
    data class Confirmed(
        val intents: List<AssistantIntent>,
        val sourceText: String
    ) : ConfirmationResolution()
    data object Declined : ConfirmationResolution()
    data object Expired : ConfirmationResolution()
    data object Cancelled : ConfirmationResolution()
    data class StillPending(val message: String) : ConfirmationResolution()
}

/**
 * Owns the lifecycle of a pending safety confirmation.
 *
 * Confirmation is intentionally separate from Android execution so the same
 * state machine can be tested without a device and cannot accidentally
 * execute an external-side-effect action twice.
 */
class TaskConfirmationManager(
    private val gate: TaskExecutionGate = TaskExecutionGate(),
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val ttlMs: Long = DEFAULT_TTL_MS
) {
    private data class Pending(
        val intents: List<AssistantIntent>,
        val message: String,
        val sourceText: String,
        val createdAtMs: Long
    )

    private var pending: Pending? = null

    fun prepare(intents: List<AssistantIntent>, sourceText: String = ""): TaskExecutionResult {
        val result = gate.inspect(intents)

        return when (result) {
            is TaskExecutionResult.ConfirmationRequired -> {
                pending = Pending(
                    intents = intents.toList(),
                    message = result.message,
                    sourceText = sourceText,
                    createdAtMs = nowMs()
                )
                result
            }

            else -> result
        }
    }

    fun resolve(input: String): ConfirmationResolution {
        val current = pending ?: return ConfirmationResolution.NoPending

        if (nowMs() - current.createdAtMs > ttlMs) {
            pending = null
            return ConfirmationResolution.Expired
        }

        return when (normalize(input)) {
            "yes", "y", "eh", "e", "confirm", "confirmed", "na tabbatar", "tabbatar" -> {
                pending = null
                ConfirmationResolution.Confirmed(current.intents, current.sourceText)
            }

            "no", "n", "a'a", "a’a", "cancel", "cancelled", "soke", "a soke" -> {
                pending = null
                ConfirmationResolution.Declined
            }

            else -> ConfirmationResolution.StillPending(current.message)
        }
    }

    fun cancel(): Boolean {
        if (pending == null) return false
        pending = null
        return true
    }

    fun hasPending(): Boolean = pending != null

    private fun normalize(input: String): String =
        input.trim().lowercase().replace(Regex("\\s+"), " ")

    companion object {
        const val DEFAULT_TTL_MS = 30_000L
    }
}
