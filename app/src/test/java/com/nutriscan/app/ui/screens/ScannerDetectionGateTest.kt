package com.nutriscan.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerDetectionGateTest {

    @Test
    fun acceptsFreshBarcodeWhenUnlocked() {
        val accepted = ScannerDetectionGate.shouldAcceptDetectedBarcode(
            snapshot = ScannerDetectionSnapshot(
                isLocked = false,
                lastBarcode = "123456",
                lastDetectionTimestampMillis = 1_000L
            ),
            barcode = "987654",
            nowMillis = 2_000L
        )

        assertTrue(accepted)
    }

    @Test
    fun rejectsDetectionWhenLocked() {
        val accepted = ScannerDetectionGate.shouldAcceptDetectedBarcode(
            snapshot = ScannerDetectionSnapshot(
                isLocked = true,
                lastBarcode = null,
                lastDetectionTimestampMillis = 0L
            ),
            barcode = "987654",
            nowMillis = 2_000L
        )

        assertFalse(accepted)
    }

    @Test
    fun rejectsRecentDuplicateBarcode() {
        val accepted = ScannerDetectionGate.shouldAcceptDetectedBarcode(
            snapshot = ScannerDetectionSnapshot(
                isLocked = false,
                lastBarcode = "987654",
                lastDetectionTimestampMillis = 5_000L
            ),
            barcode = "987654",
            nowMillis = 7_000L
        )

        assertFalse(accepted)
    }

    @Test
    fun acceptsSameBarcodeAfterDuplicateWindow() {
        val accepted = ScannerDetectionGate.shouldAcceptDetectedBarcode(
            snapshot = ScannerDetectionSnapshot(
                isLocked = false,
                lastBarcode = "987654",
                lastDetectionTimestampMillis = 5_000L
            ),
            barcode = "987654",
            nowMillis = 8_000L
        )

        assertTrue(accepted)
    }
}
