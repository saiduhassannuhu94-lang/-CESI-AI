package com.cesi.assistant

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

class SelfieActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private var imageCapture: ImageCapture? = null

    private val cameraPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        previewView = PreviewView(this)
        setContentView(previewView)

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermission.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    private fun startCamera() {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider =
                cameraProviderFuture.get()

            val preview =
                Preview.Builder().build()

            imageCapture =
                ImageCapture.Builder()
                    .setCaptureMode(
                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                    )
                    .build()

            try {

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageCapture
                )

                preview.setSurfaceProvider(
                    previewView.surfaceProvider
                )

                previewView.postDelayed(
                    {
                        takeSelfie()
                    },
                    1500
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Unable to start front camera.",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takeSelfie() {

        val capture =
            imageCapture ?: return

        val name =
            "CESI_Selfie_${System.currentTimeMillis()}.jpg"

        val contentValues =
            ContentValues().apply {

                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    name
                )

                put(
                    MediaStore.Images.Media.MIME_TYPE,
                    "image/jpeg"
                )

                if (
                    android.os.Build.VERSION.SDK_INT >=
                    android.os.Build.VERSION_CODES.Q
                ) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "Pictures/CESI"
                    )
                }
            }

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object :
                ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    outputFileResults:
                    ImageCapture.OutputFileResults
                ) {

                    Toast.makeText(
                        this@SelfieActivity,
                        "Selfie saved to Pictures/CESI",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {

                    Toast.makeText(
                        this@SelfieActivity,
                        "Selfie failed: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }
            }
        )
    }
}
