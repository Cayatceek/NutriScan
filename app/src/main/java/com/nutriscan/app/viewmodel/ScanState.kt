package com.nutriscan.app.viewmodel

import com.nutriscan.app.localization.UiText

sealed class ScanState {
    data object Idle : ScanState()
    data object Scanning : ScanState()
    data object Loading : ScanState()
    data class Success(val barcode: String) : ScanState()
    data object NotFound : ScanState()
    data class Error(val message: UiText) : ScanState()
}
