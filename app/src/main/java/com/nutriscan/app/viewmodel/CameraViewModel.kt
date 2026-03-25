package com.nutriscan.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.data.local.SettingsDataStore
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.data.repository.FakeProductRepository
import com.nutriscan.app.data.repository.ProductRepositoryImpl
import com.nutriscan.app.data.repository.ScanHistoryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class ScannerPanel {
    NONE,
    BARCODE,
    MANUAL,
    PHOTO
}

data class CameraUiState(
    val scanState: ScanState = ScanState.Idle,
    val lastBarcode: String? = null,
    val errorMessage: String? = null,
    val halalModeEnabled: Boolean = false,
    val healthyModeEnabled: Boolean = false,
    val selectedAllergensCount: Int = 0,
    val activePanel: ScannerPanel = ScannerPanel.NONE,
    val barcodeInput: String = "",
    val manualSearchQuery: String = "",
    val manualSearchResults: List<ProductEntity> = emptyList(),
    val isManualSearchLoading: Boolean = false,
    val alternatives: List<ProductEntity> = emptyList()
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val fakeProductRepository = FakeProductRepository()
    private val productRepository = ProductRepositoryImpl(application)
    private val scanHistoryRepository = ScanHistoryRepository(application)
    private val settingsDataStore = SettingsDataStore(application)

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                settingsDataStore.selectedAllergens,
                settingsDataStore.halalMode,
                settingsDataStore.healthyMode
            ) { selectedAllergens, halalModeEnabled, healthyModeEnabled ->
                Triple(selectedAllergens.size, halalModeEnabled, healthyModeEnabled)
            }.collect { (allergensCount, halalModeEnabled, healthyModeEnabled) ->
                _uiState.value = _uiState.value.copy(
                    selectedAllergensCount = allergensCount,
                    halalModeEnabled = halalModeEnabled,
                    healthyModeEnabled = healthyModeEnabled
                )
            }
        }
    }

    fun openPanel(panel: ScannerPanel) {
        _uiState.value = _uiState.value.copy(
            activePanel = panel,
            errorMessage = null
        )
    }

    fun closePanel() {
        _uiState.value = _uiState.value.copy(activePanel = ScannerPanel.NONE)
    }

    fun onBarcodeInputChange(value: String) {
        _uiState.value = _uiState.value.copy(barcodeInput = value)
    }

    fun onManualSearchQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(
            manualSearchQuery = value,
            manualSearchResults = if (value.isBlank()) emptyList() else _uiState.value.manualSearchResults
        )
    }

    fun submitBarcodeInput() {
        val barcode = _uiState.value.barcodeInput.trim()
        if (barcode.isBlank()) {
            _uiState.value = _uiState.value.copy(
                scanState = ScanState.Error("Enter a barcode to continue"),
                errorMessage = "Enter a barcode to continue"
            )
            return
        }

        viewModelScope.launch {
            performBarcodeLookup(barcode)
        }
    }

    fun onBarcodeDetected(barcode: String) {
        viewModelScope.launch {
            performBarcodeLookup(barcode)
        }
    }

    fun searchManualProducts() {
        val query = _uiState.value.manualSearchQuery.trim()
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(
                isManualSearchLoading = false,
                manualSearchResults = emptyList(),
                errorMessage = "Enter at least 2 characters to search"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isManualSearchLoading = true,
                errorMessage = null
            )

            try {
                val results = fakeProductRepository.searchByQuery(query)
                _uiState.value = _uiState.value.copy(
                    isManualSearchLoading = false,
                    manualSearchResults = results
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isManualSearchLoading = false,
                    errorMessage = e.message ?: "Unable to search products"
                )
            }
        }
    }

    fun selectManualSearchResult(product: ProductEntity) {
        viewModelScope.launch {
            persistProduct(product)
            _uiState.value = _uiState.value.copy(
                scanState = ScanState.Success(product.id),
                lastBarcode = product.barcode ?: product.id,
                activePanel = ScannerPanel.NONE,
                manualSearchResults = emptyList(),
                manualSearchQuery = "",
                barcodeInput = product.barcode ?: product.id,
                alternatives = emptyList(),
                errorMessage = null
            )
        }
    }

    fun resetToIdle() {
        _uiState.value = _uiState.value.copy(
            scanState = ScanState.Idle,
            errorMessage = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            scanState = if (_uiState.value.scanState is ScanState.Error) ScanState.Idle else _uiState.value.scanState
        )
    }

    private suspend fun performBarcodeLookup(barcode: String) {
        if (barcode.isBlank()) {
            _uiState.value = _uiState.value.copy(
                scanState = ScanState.Error("Invalid barcode"),
                errorMessage = "Invalid barcode"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            scanState = ScanState.Scanning,
            lastBarcode = barcode,
            errorMessage = null,
            activePanel = ScannerPanel.NONE,
            alternatives = emptyList()
        )

        delay(250)

        _uiState.value = _uiState.value.copy(
            scanState = ScanState.Loading,
            barcodeInput = barcode
        )

        try {
            val product = fakeProductRepository.searchByBarcode(barcode)
            if (product != null) {
                persistProduct(product)
                _uiState.value = _uiState.value.copy(
                    scanState = ScanState.Success(product.id),
                    lastBarcode = product.barcode ?: product.id,
                    alternatives = emptyList()
                )
            } else {
                val alternatives = fakeProductRepository.getSuggestedAlternatives(limit = 3)
                _uiState.value = _uiState.value.copy(
                    scanState = ScanState.NotFound,
                    lastBarcode = barcode,
                    alternatives = alternatives
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                scanState = ScanState.Error("Unable to find product right now"),
                errorMessage = e.message ?: "Unable to find product right now"
            )
        }
    }

    private suspend fun persistProduct(product: ProductEntity) {
        productRepository.saveProduct(product)
        scanHistoryRepository.addToHistory(product.id, product.imageUrl)
    }
}
