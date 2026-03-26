package com.nutriscan.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.BuildConfig
import com.nutriscan.app.R
import com.nutriscan.app.data.local.SettingsDataStore
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.data.repository.FakeProductRepository
import com.nutriscan.app.data.repository.ProductLookupResult
import com.nutriscan.app.data.repository.ProductRepositoryImpl
import com.nutriscan.app.data.repository.ScanHistoryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ScannerPanel {
    NONE,
    BARCODE,
    MANUAL,
    PHOTO
}

enum class DemoScenarioFlow {
    RESULT,
    NOT_FOUND
}

data class DemoScenario(
    val id: String,
    val label: String,
    val statusLabel: String,
    val barcode: String,
    val expectedFlow: DemoScenarioFlow
)

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
    val alternatives: List<ProductEntity> = emptyList(),
    val demoScenarios: List<DemoScenario> = emptyList(),
    val activeDemoScenarioId: String? = null
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val debugProductRepository = FakeProductRepository()
    private val productRepository = ProductRepositoryImpl(application)
    private val scanHistoryRepository = ScanHistoryRepository(application)
    private val settingsDataStore = SettingsDataStore(application)

    private val _uiState = MutableStateFlow(
        CameraUiState(
            demoScenarios = defaultDemoScenarios()
        )
    )
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
                scanState = ScanState.Error(message(R.string.scanner_error_enter_barcode)),
                errorMessage = message(R.string.scanner_error_enter_barcode)
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

    fun runDemoScenario(scenario: DemoScenario) {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(activeDemoScenarioId = scenario.id)
            performDemoLookup(scenario.barcode)
        }
    }

    fun searchManualProducts() {
        val query = _uiState.value.manualSearchQuery.trim()
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(
                isManualSearchLoading = false,
                manualSearchResults = emptyList(),
                errorMessage = message(R.string.scanner_error_short_search)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isManualSearchLoading = true,
                errorMessage = null
            )

            try {
                val results = productRepository.searchProducts(query).first()
                _uiState.value = _uiState.value.copy(
                    isManualSearchLoading = false,
                    manualSearchResults = results
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isManualSearchLoading = false,
                    errorMessage = e.message ?: message(R.string.scanner_error_search_products)
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
                errorMessage = null,
                activeDemoScenarioId = null
            )
        }
    }

    fun resetToIdle() {
        _uiState.value = _uiState.value.copy(
            scanState = ScanState.Idle,
            errorMessage = null,
            activeDemoScenarioId = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            activeDemoScenarioId = null,
            scanState = if (_uiState.value.scanState is ScanState.Error) ScanState.Idle else _uiState.value.scanState
        )
    }

    fun retryLastLookup() {
        val barcode = _uiState.value.lastBarcode
        if (barcode.isNullOrBlank()) {
            clearError()
            return
        }
        viewModelScope.launch {
            performBarcodeLookup(barcode)
        }
    }

    private suspend fun performBarcodeLookup(barcode: String) {
        if (barcode.isBlank()) {
            _uiState.value = _uiState.value.copy(
                scanState = ScanState.Error(message(R.string.scanner_error_invalid_barcode)),
                errorMessage = message(R.string.scanner_error_invalid_barcode),
                activeDemoScenarioId = null
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
            when (val result = productRepository.lookupBarcode(barcode)) {
                is ProductLookupResult.Success -> {
                    persistProduct(result.product)
                    _uiState.value = _uiState.value.copy(
                        scanState = ScanState.Success(result.product.id),
                        lastBarcode = result.product.barcode ?: result.product.id,
                        alternatives = emptyList(),
                        errorMessage = null
                    )
                }

                ProductLookupResult.NotFound -> {
                    _uiState.value = _uiState.value.copy(
                        scanState = ScanState.NotFound,
                        lastBarcode = barcode,
                        alternatives = emptyList(),
                        errorMessage = null
                    )
                }

                is ProductLookupResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        scanState = ScanState.Error(message(R.string.scanner_error_network)),
                        errorMessage = message(R.string.scanner_error_network),
                        activeDemoScenarioId = null
                    )
                }

                is ProductLookupResult.ServerUnavailable -> {
                    _uiState.value = _uiState.value.copy(
                        scanState = ScanState.Error(message(R.string.scanner_error_server)),
                        errorMessage = message(R.string.scanner_error_server),
                        activeDemoScenarioId = null
                    )
                }

                is ProductLookupResult.UnknownError -> {
                    _uiState.value = _uiState.value.copy(
                        scanState = ScanState.Error(message(R.string.scanner_error_lookup_failed)),
                        errorMessage = result.throwable?.message ?: message(R.string.scanner_error_lookup_failed),
                        activeDemoScenarioId = null
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                scanState = ScanState.Error(message(R.string.scanner_error_lookup_failed)),
                errorMessage = e.message ?: message(R.string.scanner_error_lookup_failed),
                activeDemoScenarioId = null
            )
        }
    }

    private suspend fun performDemoLookup(barcode: String) {
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

        val product = debugProductRepository.searchByBarcode(barcode)
        if (product != null) {
            persistProduct(product.copy(source = "debug"))
            _uiState.value = _uiState.value.copy(
                scanState = ScanState.Success(product.id),
                lastBarcode = product.barcode ?: product.id,
                alternatives = emptyList()
            )
        } else {
            _uiState.value = _uiState.value.copy(
                scanState = ScanState.NotFound,
                lastBarcode = barcode,
                alternatives = emptyList()
            )
        }
    }

    private suspend fun persistProduct(product: ProductEntity) {
        productRepository.saveProduct(product)
        scanHistoryRepository.addToHistory(product.id, product.imageUrl)
    }

    private fun defaultDemoScenarios(): List<DemoScenario> {
        return listOf(
            DemoScenario(
                id = "safe",
                label = "SAFE",
                statusLabel = "Low risk",
                barcode = "478002200002",
                expectedFlow = DemoScenarioFlow.RESULT
            ),
            DemoScenario(
                id = "caution",
                label = "CAUTION",
                statusLabel = "Needs review",
                barcode = "478002200003",
                expectedFlow = DemoScenarioFlow.RESULT
            ),
            DemoScenario(
                id = "dietary",
                label = "DIETARY",
                statusLabel = "Mode-sensitive",
                barcode = "478002200006",
                expectedFlow = DemoScenarioFlow.RESULT
            ),
            DemoScenario(
                id = "not_found",
                label = "NOT FOUND",
                statusLabel = "Missing product",
                barcode = "000000000000",
                expectedFlow = DemoScenarioFlow.NOT_FOUND
            )
        )
    }

    private fun message(resId: Int): String {
        return getApplication<Application>().getString(resId)
    }
}
