package com.nutriscan.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun BarcodeCameraPreview(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onBarcodeDetected: (String) -> Unit,
    onScannerError: (Throwable) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(enabled, lifecycleOwner) {
        if (enabled) {
            bindCameraUseCases(
                context = context,
                previewView = previewView,
                cameraExecutor = cameraExecutor,
                lifecycleOwner = lifecycleOwner,
                onBarcodeDetected = onBarcodeDetected,
                onScannerError = onScannerError
            )
        } else {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { previewView }
    )
}

@SuppressLint("UnsafeOptInUsageError")
private fun bindCameraUseCases(
    context: Context,
    previewView: PreviewView,
    cameraExecutor: ExecutorService,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onBarcodeDetected: (String) -> Unit,
    onScannerError: (Throwable) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(
                        cameraExecutor,
                        RetailBarcodeAnalyzer(
                            onBarcodeDetected = onBarcodeDetected,
                            onScannerError = onScannerError
                        )
                    )
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analyzer
            )
        } catch (throwable: Throwable) {
            onScannerError(throwable)
        }
    }, ContextCompat.getMainExecutor(context))
}

private class RetailBarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit,
    private val onScannerError: (Throwable) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_ITF
            )
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val barcode = barcodes
                    .firstOrNull { candidate -> !candidate.rawValue.isNullOrBlank() }
                    ?.rawValue
                    ?.trim()
                if (!barcode.isNullOrBlank()) {
                    onBarcodeDetected(barcode)
                }
            }
            .addOnFailureListener(onScannerError)
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
