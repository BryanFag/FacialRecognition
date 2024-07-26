package br.com.bryan.facial.recognition

import android.content.ContentValues.TAG
import android.graphics.Color
import android.util.Log
import android.widget.FrameLayout
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import processImageProxy
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CamActivity() {
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    AndroidView(factory = { ctx ->
        val previewView = PreviewView(ctx)
        val faceOverlay = FaceOverlayView(ctx)
        val similarityTextView = TextView(ctx).apply {
            textSize = 24f
            setTextColor(Color.RED)
        }

        val container = FrameLayout(ctx).apply {
            addView(previewView)
            addView(faceOverlay, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            addView(similarityTextView, FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
            val imageAnalyzer = ImageAnalysis.Builder().build().apply { setAnalyzer(cameraExecutor) {
                processImageProxy(
                    it,
                    faceOverlay,
                    previewView,
                    similarityTextView
                )
            } }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(ctx as LifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e(TAG, "Failed to bind use cases", exc)
            }
        }, ContextCompat.getMainExecutor(ctx))

        container
    })
}