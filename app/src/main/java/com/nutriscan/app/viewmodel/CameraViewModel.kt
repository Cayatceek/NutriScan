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
import com.nutriscan.app.localization.UiText
import com.nutriscan.app.localization.uiText
import com.nutriscan.app.ui.components.ScannerRuntime
import com.nutriscan.app.util.BrandedLoadingOutcome
import com.nutriscan.app.util.runBrandedLoadingGate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.util.Log

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
    val errorMessage: UiText? = null,
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
    val scannerRuntime: ScannerRuntime by lazy(LazyThreadSafetyMode.NONE) {
        ScannerRuntime(application)
    }

    private val _uiState = MutableStateFlow(
        CameraUiState(
            demoScenarios = defaultDemoScenarios()
        )
    )
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()
    private var scannerPrewarmJob: Job? = null
    private var activeLookupJob: Job? = null
    private var activeLookupToken: Long = 0L

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

    fun prepareScannerRuntime() {
        if (scannerRuntime.isWarmedUp()) return
        if (scannerPrewarmJob?.isActive == true) return

        scannerPrewarmJob = viewModelScope.launch {
            runCatching {
                scannerRuntime.warmUp()
            }
        }
    }

    fun onScannerVisibilityChanged(isVisible: Boolean) {
        if (isVisible) return

        val hadInFlightLookup = activeLookupJob != null || _uiState.value.scanState == ScanState.Loading
        invalidateActiveLookup()
        if (hadInFlightLookup) {
            _uiState.value = _uiState.value.copy(
                scanState = if (_uiState.value.scanState == ScanState.Loading) {
                    ScanState.Idle
                } else {
                    _uiState.value.scanState
                },
                activeDemoScenarioId = null
            )
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
            val error = uiText(R.string.scanner_error_enter_barcode)
            _uiState.value = _uiState.value.copy(
                scanState = ScanState.Error(error),
                errorMessage = error
            )
            return
        }

        launchLookup { lookupToken ->
            performBarcodeLookup(barcode, lookupToken)
        }
    }

    fun onBarcodeDetected(barcode: String) {
        launchLookup { lookupToken ->
            performBarcodeLookup(barcode, lookupToken)
        }
    }

    fun runDemoScenario(scenario: DemoScenario) {
        if (!BuildConfig.DEBUG) return
        launchLookup { lookupToken ->
            _uiState.value = _uiState.value.copy(activeDemoScenarioId = scenario.id)
            performDemoLookup(scenario.barcode, lookupToken)
        }
    }

    fun searchManualProducts() {
        val query = _uiState.value.manualSearchQuery.trim()
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(
                isManualSearchLoading = false,
                manualSearchResults = emptyList(),
                errorMessage = uiText(R.string.scanner_error_short_search)
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
                    errorMessage = e.message
                        ?.takeIf { it.isNotBlank() }
                        ?.let(UiText::DynamicString)
                        ?: uiText(R.string.scanner_error_search_products)
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
        launchLookup { lookupToken ->
            performBarcodeLookup(barcode, lookupToken)
        }
    }

    private suspend fun performBarcodeLookup(barcode: String, lookupToken: Long) {
        if (barcode.isBlank()) {
            val error = uiText(R.string.scanner_error_invalid_barcode)
            deliverLookupState(lookupToken) {
                it.copy(
                    scanState = ScanState.Error(error),
                    errorMessage = error,
                    activeDemoScenarioId = null
                )
            }
            return
        }

        try {
            deliverLookupState(lookupToken) {
                it.copy(
                    scanState = ScanState.Loading,
                    lastBarcode = barcode,
                    errorMessage = null,
                    activePanel = ScannerPanel.NONE,
                    alternatives = emptyList(),
                    barcodeInput = barcode
                )
            } ?: return

            when (val gatedLookup = runBrandedLoadingGate { productRepository.lookupBarcode(barcode) }) {
                is BrandedLoadingOutcome.Success -> {
                    when (val result = gatedLookup.value) {
                        is ProductLookupResult.Success -> {
                            if (!isLookupActive(lookupToken)) {
                                debugLog("Ignoring stale barcode lookup success for $barcode")
                                return
                            }
                            persistProduct(result.product)
                            deliverLookupState(lookupToken) {
                                it.copy(
                                    scanState = ScanState.Success(result.product.id),
                                    lastBarcode = result.product.barcode ?: result.product.id,
                                    alternatives = emptyList(),
                                    errorMessage = null
                                )
                            }
                        }

                        ProductLookupResult.NotFound -> {
                            deliverLookupState(lookupToken) {
                                it.copy(
                                    scanState = ScanState.NotFound,
                                    lastBarcode = barcode,
                                    alternatives = emptyList(),
                                    errorMessage = null
                                )
                            }
                        }

                        is ProductLookupResult.NetworkError -> {
                            val error = uiText(R.string.scanner_error_network)
                            deliverLookupState(lookupToken) {
                                it.copy(
                                    scanState = ScanState.Error(error),
                                    errorMessage = error,
                                    activeDemoScenarioId = null
                                )
                            }
                        }

                        is ProductLookupResult.ServerUnavailable -> {
                            val error = uiText(R.string.scanner_error_server)
                            deliverLookupState(lookupToken) {
                                it.copy(
                                    scanState = ScanState.Error(error),
                                    errorMessage = error,
                                    activeDemoScenarioId = null
                                )
                            }
                        }

                        is ProductLookupResult.UnknownError -> {
                            val error = result.throwable?.message
                                ?.takeIf { it.isNotBlank() }
                                ?.let(UiText::DynamicString)
                                ?: uiText(R.string.scanner_error_lookup_failed)
                            deliverLookupState(lookupToken) {
                                it.copy(
                                    scanState = ScanState.Error(error),
                                    errorMessage = error,
                                    activeDemoScenarioId = null
                                )
                            }
                        }
                    }
                }

                BrandedLoadingOutcome.Timeout -> {
                    val error = uiText(R.string.scanner_error_lookup_failed)
                    deliverLookupState(lookupToken) {
                        it.copy(
                            scanState = ScanState.Error(error),
                            errorMessage = error,
                            activeDemoScenarioId = null
                        )
                    }
                }

                is BrandedLoadingOutcome.Error -> {
                    val error = gatedLookup.throwable.message
                        ?.takeIf { it.isNotBlank() }
                        ?.let(UiText::DynamicString)
                        ?: uiText(R.string.scanner_error_lookup_failed)
                    deliverLookupState(lookupToken) {
                        it.copy(
                            scanState = ScanState.Error(error),
                            errorMessage = error,
                            activeDemoScenarioId = null
                        )
                    }
                }
            }
        } finally {
            clearLookupJobIfActive(lookupToken)
        }
    }

    private suspend fun performDemoLookup(barcode: String, lookupToken: Long) {
        try {
            deliverLookupState(lookupToken) {
                it.copy(
                    scanState = ScanState.Loading,
                    lastBarcode = barcode,
                    errorMessage = null,
                    activePanel = ScannerPanel.NONE,
                    alternatives = emptyList(),
                    barcodeInput = barcode
                )
            } ?: return

            when (val gatedLookup = runBrandedLoadingGate { debugProductRepository.searchByBarcode(barcode) }) {
                is BrandedLoadingOutcome.Success -> {
                    val product = gatedLookup.value
                    if (product != null) {
                        if (!isLookupActive(lookupToken)) {
                            debugLog("Ignoring stale demo lookup success for $barcode")
                            return
                        }
                        persistProduct(product.copy(source = "debug"))
                        deliverLookupState(lookupToken) {
                            it.copy(
                                scanState = ScanState.Success(product.id),
                                lastBarcode = product.barcode ?: product.id,
                                alternatives = emptyList()
                            )
                        }
                    } else {
                        deliverLookupState(lookupToken) {
                            it.copy(
                                scanState = ScanState.NotFound,
                                lastBarcode = barcode,
                                alternatives = emptyList()
                            )
                        }
                    }
                }

                BrandedLoadingOutcome.Timeout -> {
                    val error = uiText(R.string.scanner_error_lookup_failed)
                    deliverLookupState(lookupToken) {
                        it.copy(
                            scanState = ScanState.Error(error),
                            errorMessage = error
                        )
                    }
                }

                is BrandedLoadingOutcome.Error -> {
                    val error = gatedLookup.throwable.message
                        ?.takeIf { it.isNotBlank() }
                        ?.let(UiText::DynamicString)
                        ?: uiText(R.string.scanner_error_lookup_failed)
                    deliverLookupState(lookupToken) {
                        it.copy(
                            scanState = ScanState.Error(error),
                            errorMessage = error
                        )
                    }
                }
            }
        } finally {
            clearLookupJobIfActive(lookupToken)
        }
    }

    private fun launchLookup(block: suspend (Long) -> Unit) {
        val lookupToken = beginLookup()
        activeLookupJob = viewModelScope.launch {
            block(lookupToken)
        }
    }

    private fun beginLookup(): Long {
        activeLookupJob?.cancel()
        activeLookupToken += 1
        debugLog("Starting lookup token=$activeLookupToken")
        return activeLookupToken
    }

    private fun invalidateActiveLookup() {
        if (activeLookupJob == null && _uiState.value.scanState != ScanState.Loading) return

        activeLookupToken += 1
        debugLog("Invalidating active lookup token=$activeLookupToken")
        activeLookupJob?.cancel()
        activeLookupJob = null
    }

    private fun isLookupActive(lookupToken: Long): Boolean {
        return lookupToken == activeLookupToken
    }

    private fun deliverLookupState(
        lookupToken: Long,
        transform: (CameraUiState) -> CameraUiState
    ): CameraUiState? {
        if (!isLookupActive(lookupToken)) {
            debugLog("Dropping stale lookup state for token=$lookupToken")
            return null
        }
        val updatedState = transform(_uiState.value)
        if (!isLookupActive(lookupToken)) {
            debugLog("Dropping stale lookup state after transform for token=$lookupToken")
            return null
        }
        _uiState.value = updatedState
        return updatedState
    }

    private fun clearLookupJobIfActive(lookupToken: Long) {
        if (isLookupActive(lookupToken)) {
            activeLookupJob = null
        }
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
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

    override fun onCleared() {
        activeLookupJob?.cancel()
        scannerRuntime.release()
        super.onCleared()
    }

    companion object {
        private const val TAG = "CameraViewModel"
    }
}
