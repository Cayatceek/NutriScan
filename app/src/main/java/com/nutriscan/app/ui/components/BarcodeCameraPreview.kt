package com.nutriscan.app.ui.components

import android.content.Context
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nutriscan.app.BuildConfig

@Composable
fun BarcodeCameraPreview(
    runtime: ScannerRuntime,
    modifier: Modifier = Modifier,
    cameraActive: Boolean,
    scanningEnabled: Boolean,
    onBarcodeDetected: (String) -> Unit,
    onScannerError: (Throwable) -> Unit
) {
    val viewContext = LocalView.current.context
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(viewContext) { createPreviewView(viewContext) }
    val currentOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)
    val currentOnScannerError by rememberUpdatedState(onScannerError)

    LaunchedEffect(runtime, currentOnBarcodeDetected, currentOnScannerError) {
        runtime.updateCallbacks(
            onBarcodeDetected = currentOnBarcodeDetected,
            onScannerError = currentOnScannerError
        )
    }

    LaunchedEffect(runtime, cameraActive, scanningEnabled) {
        runtime.setScanningEnabled(cameraActive && scanningEnabled)
    }

    LaunchedEffect(runtime, cameraActive, lifecycleOwner, previewView) {
        if (cameraActive) {
            runtime.attach(
                lifecycleOwner = lifecycleOwner,
                surfaceProvider = previewView.surfaceProvider
            )
        } else {
            runtime.detach(lifecycleOwner)
        }
    }

    DisposableEffect(runtime, previewView, lifecycleOwner) {
        val observer = Observer<PreviewView.StreamState> { state ->
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Preview stream state: $state")
            }
            if (state == PreviewView.StreamState.STREAMING) {
                runtime.onPreviewStreaming()
            }
        }
        previewView.previewStreamState.observe(lifecycleOwner, observer)

        onDispose {
            previewView.previewStreamState.removeObserver(observer)
        }
    }

    DisposableEffect(runtime, lifecycleOwner, previewView) {
        onDispose {
            runtime.detach(lifecycleOwner)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { previewView },
        update = { }
    )
}

private fun createPreviewView(context: Context): PreviewView {
    if (BuildConfig.DEBUG) {
        Log.d(TAG, "PreviewView created")
    }
    return PreviewView(context).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FILL_CENTER
        keepScreenOn = true
    }
}

private const val TAG = "BarcodeCameraPreview"
