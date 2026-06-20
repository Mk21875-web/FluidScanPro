package com.fluidscan.pro.ui.screens.scanner.components

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.fluidscan.pro.service.scan.EdgeDetectionAnalyzer
import com.fluidscan.pro.service.scan.EdgeDetector
import com.fluidscan.pro.service.scan.EdgeResult
import java.io.File
import java.util.concurrent.Executors

/**
 * Imperative handle the screen uses to drive the (otherwise declarative) camera:
 * trigger a full-res capture and toggle the torch.
 */
class CameraController {
    internal var imageCapture: ImageCapture? = null
    internal var enableTorch: ((Boolean) -> Unit)? = null

    fun setTorch(enabled: Boolean) { enableTorch?.invoke(enabled) }

    /** Takes a picture to [target]; reports the saved path + sensor rotation, or an error. */
    fun capture(
        context: Context,
        target: File,
        onSaved: (path: String, rotationDegrees: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: run { onError("Camera not ready"); return }
        val rotation = capture.targetRotation
        val options = ImageCapture.OutputFileOptions.Builder(target).build()
        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSaved(target.absolutePath, rotation.toExifDegrees())
                }
                override fun onError(exc: ImageCaptureException) {
                    onError(exc.message ?: "Capture failed")
                }
            }
        )
    }

    private fun Int.toExifDegrees(): Int = when (this) {
        android.view.Surface.ROTATION_90 -> 90
        android.view.Surface.ROTATION_180 -> 180
        android.view.Surface.ROTATION_270 -> 270
        else -> 0
    }
}

@Composable
fun rememberCameraController(): CameraController = remember { CameraController() }

/**
 * Live CameraX preview wired to real-time [EdgeDetector]. Uses:
 *  - `Preview` for the viewfinder,
 *  - `ImageAnalysis` (STRATEGY_KEEP_ONLY_LATEST) running [EdgeDetectionAnalyzer] on a
 *    dedicated single-thread executor (never the UI thread),
 *  - `ImageCapture` (MINIMIZE_LATENCY) for the shutter.
 */
@Composable
fun CameraPreview(
    controller: CameraController,
    detector: EdgeDetector,
    flashEnabled: Boolean,
    onEdge: (EdgeResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, EdgeDetectionAnalyzer(detector, onEdge)) }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                controller.imageCapture = imageCapture

                provider.unbindAll()
                val camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                    imageCapture
                )
                controller.enableTorch = { on ->
                    if (camera.cameraInfo.hasFlashUnit()) camera.cameraControl.enableTorch(on)
                }
                controller.setTorch(flashEnabled)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        update = { controller.setTorch(flashEnabled) }
    )
}
