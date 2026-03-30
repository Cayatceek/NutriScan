package com.nutriscan.app.ui.components

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.nutriscan.app.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class ScannerRuntimeState {
    Cold,
    WarmDetached,
    BoundActive
}

class ScannerRuntime(context: Context) {

    private val appContext = context.applicationContext
    private val runtimeMutex = Mutex()
    private var cameraExecutor: ExecutorService = createCameraExecutor()

    @Volatile
    private var scanningEnabled: Boolean = false

    @Volatile
    private var onBarcodeDetected: (String) -> Unit = {}

    @Volatile
    private var onScannerError: (Throwable) -> Unit = {}

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var barcodeScanner: BarcodeScanner? = null
    private var boundLifecycleOwner: LifecycleOwner? = null

    @Volatile
    private var runtimeState: ScannerRuntimeState = ScannerRuntimeState.Cold

    @Volatile
    private var lastAttachStartedAtMillis: Long = 0L

    @Volatile
    private var lastDetachRequestedAtMillis: Long = 0L

    @Volatile
    private var nextAttachCycleId: Long = 0L

    @Volatile
    private var activeAttachCycleId: Long = 0L

    @Volatile
    private var streamingReportedAttachCycleId: Long = -1L

    suspend fun warmUp() {
        runtimeMutex.withLock {
            ensureCameraProvider()
            ensureBarcodeScanner()
            ensureUseCases()
            moveToWarmDetached("Runtime warmed")
            debugLog("Runtime warmed")
        }
    }

    fun state(): ScannerRuntimeState {
        return runtimeState
    }

    fun isWarmedUp(): Boolean = runtimeState != ScannerRuntimeState.Cold

    fun isBoundActive(): Boolean = runtimeState == ScannerRuntimeState.BoundActive

    fun canRebindQuickly(): Boolean = runtimeState == ScannerRuntimeState.WarmDetached

    private fun moveToWarmDetached(reason: String) {
        if (runtimeState != ScannerRuntimeState.WarmDetached) {
            runtimeState = ScannerRuntimeState.WarmDetached
            debugLog("State -> WarmDetached ($reason)")
        } else {
            debugLog("State kept WarmDetached ($reason)")
        }
    }

    fun updateCallbacks(
        onBarcodeDetected: (String) -> Unit,
        onScannerError: (Throwable) -> Unit
    ) {
        this.onBarcodeDetected = onBarcodeDetected
        this.onScannerError = onScannerError
    }

    fun setScanningEnabled(enabled: Boolean) {
        scanningEnabled = enabled
    }

    suspend fun attach(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider
    ) {
        runtimeMutex.withLock {
            activeAttachCycleId = ++nextAttachCycleId
            streamingReportedAttachCycleId = -1L
            lastAttachStartedAtMillis = SystemClock.elapsedRealtime()
            debugLog("Attach requested (state=$runtimeState, cycle=$activeAttachCycleId)")
            val provider = ensureCameraProvider()
            val preview = ensurePreview()
            val imageAnalysis = ensureImageAnalysis()

            preview.surfaceProvider = surfaceProvider

            if (boundLifecycleOwner === lifecycleOwner && runtimeState == ScannerRuntimeState.BoundActive) {
                debugLog("Attach skipped, already bound to current lifecycle")
                return
            }

            if (boundLifecycleOwner != null) {
                runCatching {
                    provider.unbind(preview, imageAnalysis)
                }.onFailure { throwable ->
                    debugLog("Previous targeted unbind failed: ${throwable.message}")
                }
            }

            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
            boundLifecycleOwner = lifecycleOwner
            runtimeState = ScannerRuntimeState.BoundActive
            debugLog(
                "Camera bound in ${SystemClock.elapsedRealtime() - lastAttachStartedAtMillis}ms " +
                    "(cycle=$activeAttachCycleId)"
            )
        }
    }

    fun detach(lifecycleOwner: LifecycleOwner) {
        lastDetachRequestedAtMillis = SystemClock.elapsedRealtime()
        debugLog("Detach requested (state=$runtimeState)")
        val provider = cameraProvider
        if (provider == null) {
            clearPreviewSurfaceProvider()
            resetAttachCycleTracking()
            if (runtimeState != ScannerRuntimeState.Cold) {
                moveToWarmDetached("Detached without active provider")
            }
            return
        }
        if (boundLifecycleOwner !== lifecycleOwner) return

        val preview = preview
        val imageAnalysis = imageAnalysis
        if (preview == null || imageAnalysis == null) {
            boundLifecycleOwner = null
            clearPreviewSurfaceProvider()
            resetAttachCycleTracking()
            moveToWarmDetached("Detached without complete use cases")
            return
        }

        runCatching {
            provider.unbind(preview, imageAnalysis)
            boundLifecycleOwner = null
            clearPreviewSurfaceProvider()
            resetAttachCycleTracking()
            moveToWarmDetached("Detached from lifecycle")
            debugLog("Camera detached in ${SystemClock.elapsedRealtime() - lastDetachRequestedAtMillis}ms")
        }.onFailure { throwable ->
            boundLifecycleOwner = null
            clearPreviewSurfaceProvider()
            resetAttachCycleTracking()
            moveToWarmDetached("Detach fallback after failure")
            debugLog("Camera detach failed: ${throwable.message}")
        }
    }

    fun release() {
        preview?.let { preview ->
            imageAnalysis?.let { analysis ->
                runCatching {
                    cameraProvider?.unbind(preview, analysis)
                }
            }
        }
        clearPreviewSurfaceProvider()
        imageAnalysis?.clearAnalyzer()
        barcodeScanner?.close()
        cameraExecutor.shutdown()
        cameraExecutor = createCameraExecutor()
        cameraProvider = null
        preview = null
        imageAnalysis = null
        barcodeScanner = null
        boundLifecycleOwner = null
        resetAttachCycleTracking()
        runtimeState = ScannerRuntimeState.Cold
        debugLog("Runtime fully released (State -> Cold)")
    }

    fun onPreviewStreaming() {
        val attachCycleId = activeAttachCycleId
        if (attachCycleId == 0L) {
            return
        }
        if (streamingReportedAttachCycleId == attachCycleId) {
            return
        }
        streamingReportedAttachCycleId = attachCycleId
        val attachStartedAtMillis = lastAttachStartedAtMillis
        if (attachStartedAtMillis > 0L) {
            debugLog(
                "Preview streaming after ${SystemClock.elapsedRealtime() - attachStartedAtMillis}ms " +
                    "from attach (cycle=$attachCycleId)"
            )
        } else {
            debugLog("Preview streaming (cycle=$attachCycleId)")
        }
    }

    private suspend fun ensureCameraProvider(): ProcessCameraProvider {
        cameraProvider?.let { return it }
        val provider = awaitCameraProvider(appContext)
        cameraProvider = provider
        return provider
    }

    private fun ensureUseCases() {
        ensurePreview()
        ensureImageAnalysis()
    }

    private fun ensurePreview(): Preview {
        preview?.let { return it }
        return Preview.Builder()
            .build()
            .also { preview = it }
    }

    private fun ensureImageAnalysis(): ImageAnalysis {
        imageAnalysis?.let { return it }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(
            ensureCameraExecutor(),
            RuntimeBarcodeAnalyzer(
                barcodeScannerProvider = { ensureBarcodeScanner() },
                isScanningEnabled = { scanningEnabled },
                onBarcodeDetected = { barcode ->
                    onBarcodeDetected(barcode)
                },
                onScannerError = { throwable ->
                    onScannerError(throwable)
                }
            )
        )

        imageAnalysis = analysis
        debugLog("Analyzer attached")
        return analysis
    }

    private fun ensureBarcodeScanner(): BarcodeScanner {
        barcodeScanner?.let { return it }
        val options = BarcodeScannerOptions.Builder()
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
        return BarcodeScanning.getClient(options).also {
            barcodeScanner = it
        }
    }

    private fun ensureCameraExecutor(): ExecutorService {
        if (cameraExecutor.isShutdown || cameraExecutor.isTerminated) {
            cameraExecutor = createCameraExecutor()
        }
        return cameraExecutor
    }

    private fun clearPreviewSurfaceProvider() {
        preview?.surfaceProvider = null
    }

    private fun resetAttachCycleTracking() {
        activeAttachCycleId = 0L
        streamingReportedAttachCycleId = -1L
        lastAttachStartedAtMillis = 0L
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    private class RuntimeBarcodeAnalyzer(
        private val barcodeScannerProvider: () -> BarcodeScanner,
        private val isScanningEnabled: () -> Boolean,
        private val onBarcodeDetected: (String) -> Unit,
        private val onScannerError: (Throwable) -> Unit
    ) : ImageAnalysis.Analyzer {

        override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
            if (!isScanningEnabled()) {
                imageProxy.close()
                return
            }

            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return
            }

            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScannerProvider()
                .process(image)
                .addOnSuccessListener { barcodes ->
                    val detectedBarcode = barcodes.firstNotNullOfOrNull { barcode ->
                        barcode.rawValue?.takeIf { it.isNotBlank() }
                    }
                    if (detectedBarcode != null && isScanningEnabled()) {
                        onBarcodeDetected(detectedBarcode)
                    }
                }
                .addOnFailureListener { throwable ->
                    onScannerError(throwable)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    companion object {
        private const val TAG = "ScannerRuntime"

        private fun createCameraExecutor(): ExecutorService {
            return Executors.newSingleThreadExecutor()
        }
    }
}

private suspend fun awaitCameraProvider(context: Context): ProcessCameraProvider {
    val future = ProcessCameraProvider.getInstance(context)
    return suspendCancellableCoroutine { continuation ->
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { provider -> continuation.resume(provider) }
                    .onFailure { throwable -> continuation.resumeWithException(throwable) }
            },
            ContextCompat.getMainExecutor(context)
        )
    }
}
