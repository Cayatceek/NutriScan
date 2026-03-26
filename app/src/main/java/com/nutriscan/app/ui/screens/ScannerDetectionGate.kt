package com.nutriscan.app.ui.screens

internal data class ScannerDetectionSnapshot(
    val isLocked: Boolean,
    val lastBarcode: String?,
    val lastDetectionTimestampMillis: Long
)

internal object ScannerDetectionGate {

    private const val DEFAULT_DUPLICATE_WINDOW_MILLIS = 2500L

    fun shouldAcceptDetectedBarcode(
        snapshot: ScannerDetectionSnapshot,
        barcode: String,
        nowMillis: Long,
        duplicateWindowMillis: Long = DEFAULT_DUPLICATE_WINDOW_MILLIS
    ): Boolean {
        if (snapshot.isLocked) return false
        if (barcode.isBlank()) return false

        val isRecentDuplicate = snapshot.lastBarcode == barcode &&
            nowMillis - snapshot.lastDetectionTimestampMillis < duplicateWindowMillis

        return !isRecentDuplicate
    }
}
