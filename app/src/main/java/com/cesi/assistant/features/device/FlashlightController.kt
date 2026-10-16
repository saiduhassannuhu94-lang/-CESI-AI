package com.cesi.assistant.features.device

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.cesi.assistant.core.task.ExecutionResult
import com.cesi.assistant.core.task.ExecutionStatus

/**
 * Typed flashlight executor. SUCCESS means CameraManager accepted the torch
 * request; it is not an independent read-back of the physical torch state.
 */
class FlashlightController(context: Context) {
    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private fun findFlashCamera(manager: CameraManager): String? = try {
        manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (_: Exception) {
        null
    }

    fun setEnabled(enabled: Boolean): ExecutionResult {
        val successMessage = if (enabled) "Na kunna haske." else "Na kashe haske."
        val failureMessage = if (enabled) "Ban iya kunna haske ba." else "Ban iya kashe haske ba."

        val manager = cameraManager
            ?: return ExecutionResult(
                status = ExecutionStatus.FAILED,
                message = failureMessage,
                retryable = false
            )

        val cameraId = findFlashCamera(manager)
            ?: return ExecutionResult(
                status = ExecutionStatus.FAILED,
                message = failureMessage,
                retryable = false
            )

        return try {
            manager.setTorchMode(cameraId, enabled)
            ExecutionResult(
                status = ExecutionStatus.SUCCESS,
                message = successMessage
            )
        } catch (_: Exception) {
            ExecutionResult(
                status = ExecutionStatus.FAILED,
                message = failureMessage,
                retryable = true
            )
        }
    }
}
