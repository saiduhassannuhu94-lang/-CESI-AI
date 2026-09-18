package com.cesi.assistant.features.device

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class FlashlightController(
    context: Context
) {

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private fun findFlashCamera(): String? {

        return cameraManager.cameraIdList.firstOrNull { id ->

            val characteristics =
                cameraManager.getCameraCharacteristics(id)

            characteristics.get(
                CameraCharacteristics.FLASH_INFO_AVAILABLE
            ) == true
        }
    }

    fun setEnabled(enabled: Boolean): Boolean {

        val cameraId = findFlashCamera()
            ?: return false

        return try {

            cameraManager.setTorchMode(
                cameraId,
                enabled
            )

            true

        } catch (_: Exception) {

            false
        }
    }
}
