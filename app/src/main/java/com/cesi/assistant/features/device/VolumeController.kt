package com.cesi.assistant.features.device

import android.content.Context
import android.media.AudioManager
import com.cesi.assistant.core.task.ExecutionResult
import com.cesi.assistant.core.task.ExecutionStatus

/**
 * Typed volume command executor.
 *
 * Android's adjustVolume() has no result/read-back value. A SUCCESS here means
 * the platform call returned without throwing; independent volume verification
 * remains NOT_CHECKED by ActionExecutionProfileRegistry.
 */
class VolumeController(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun up(): ExecutionResult = adjust(
        AudioManager.ADJUST_RAISE,
        "Na ƙara sauti.",
        "Ban iya ƙara sauti ba."
    )

    fun down(): ExecutionResult = adjust(
        AudioManager.ADJUST_LOWER,
        "Na rage sauti.",
        "Ban iya rage sauti ba."
    )

    fun mute(): ExecutionResult = adjust(
        AudioManager.ADJUST_MUTE,
        "Na yi shiru.",
        "Ban iya kashe sauti ba."
    )

    private fun adjust(operation: Int, successMessage: String, failureMessage: String): ExecutionResult {
        val manager = audio ?: return ExecutionResult(
            status = ExecutionStatus.FAILED,
            message = failureMessage,
            retryable = false
        )

        return try {
            manager.adjustVolume(operation, AudioManager.FLAG_SHOW_UI)
            ExecutionResult(
                status = ExecutionStatus.SUCCESS,
                message = successMessage
            )
        } catch (_: SecurityException) {
            ExecutionResult(
                status = ExecutionStatus.BLOCKED,
                message = failureMessage,
                retryable = true
            )
        } catch (_: RuntimeException) {
            ExecutionResult(
                status = ExecutionStatus.FAILED,
                message = failureMessage,
                retryable = true
            )
        }
    }
}
