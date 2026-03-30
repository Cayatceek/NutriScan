package com.nutriscan.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.R
import com.nutriscan.app.data.local.SettingsDataStore
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.data.local.entity.ScanHistoryWithProduct
import com.nutriscan.app.data.repository.ScanHistoryRepository
import com.nutriscan.app.domain.analysis.AnalysisEngine
import com.nutriscan.app.domain.analysis.AnalysisSettings
import com.nutriscan.app.domain.analyzer.Allergen
import com.nutriscan.app.localization.UiText
import com.nutriscan.app.localization.uiText
import com.nutriscan.app.model.SafetyStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HistoryRecordUiState(
    val id: String,
    val productId: String,
    val productName: String,
    val brand: String,
    val isFavorite: Boolean,
    val scannedAtLabel: String,
    val status: SafetyStatus
)

data class HistoryUiState(
    val items: List<HistoryRecordUiState> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val searchQuery: String = "",
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    val loadState: ScreenLoadState = ScreenLoadState.Idle
)

enum class HistoryFilter {
    ALL,
    FAVORITES
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val scanHistoryRepository = ScanHistoryRepository(application)
    private val settingsDataStore = SettingsDataStore(application)

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var historyJob: Job? = null
    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(HistoryFilter.ALL)
    private val analysisStatusCache = object : LinkedHashMap<AnalysisCacheKey, SafetyStatus>(
        ANALYSIS_CACHE_MAX_SIZE,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<AnalysisCacheKey, SafetyStatus>?
        ): Boolean = size > ANALYSIS_CACHE_MAX_SIZE
    }

    fun loadIfNeeded() {
        if (historyJob != null || _uiState.value.loadState == ScreenLoadState.Success) return

        historyJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadState = ScreenLoadState.Loading,
                error = null
            )

            val baseRecordsFlow = combine(
                scanHistoryRepository.getScanHistoryWithProducts(),
                settingsDataStore.selectedAllergens,
                settingsDataStore.halalMode,
                settingsDataStore.language
            ) { history, selectedAllergens, halalModeEnabled, languageCode ->
                HistoryBaseSnapshot(
                    history = history,
                    selectedAllergens = selectedAllergens,
                    halalModeEnabled = halalModeEnabled,
                    languageCode = languageCode
                )
            }
                .mapLatest { snapshot ->
                    withContext(Dispatchers.Default) {
                        snapshot.toBaseRecords()
                    }
                }

            combine(
                baseRecordsFlow,
                searchQuery,
                selectedFilter
            ) { baseRecords, query, filter ->
                HistoryProjectionSnapshot(
                    baseRecords = baseRecords,
                    query = query,
                    filter = filter
                )
            }
                .catch { e ->
                    historyJob = null
                    _uiState.value = _uiState.value.copy(
                        error = e.message
                            ?.takeIf { it.isNotBlank() }
                            ?.let(UiText::DynamicString)
                            ?: uiText(R.string.history_error_message),
                        isLoading = false,
                        loadState = ScreenLoadState.Error
                    )
                }
                .collect { snapshot ->
                    _uiState.value = _uiState.value.copy(
                        items = snapshot.projectItems(),
                        isLoading = false,
                        loadState = ScreenLoadState.Success,
                        error = null,
                        searchQuery = snapshot.query,
                        selectedFilter = snapshot.filter
                    )
                }
        }
    }

    fun retryLoad() {
        historyJob?.cancel()
        historyJob = null
        loadIfNeeded()
    }

    fun deleteScan(scanId: String) {
        viewModelScope.launch {
            scanHistoryRepository.deleteScanHistory(scanId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            scanHistoryRepository.deleteAllHistory()
        }
    }

    fun searchHistory(query: String) {
        searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setFilter(filter: HistoryFilter) {
        selectedFilter.value = filter
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    private data class HistoryBaseSnapshot(
        val history: List<ScanHistoryWithProduct>,
        val selectedAllergens: Set<Allergen>,
        val halalModeEnabled: Boolean,
        val languageCode: String
    )

    private data class HistoryBaseRecord(
        val uiModel: HistoryRecordUiState,
        val normalizedProductName: String,
        val normalizedBrand: String
    )

    private data class HistoryProjectionSnapshot(
        val baseRecords: List<HistoryBaseRecord>,
        val query: String,
        val filter: HistoryFilter
    )

    private data class HistoryAnalysisContext(
        val selectedAllergens: Set<String>,
        val allergensSignature: String,
        val halalModeEnabled: Boolean
    )

    private data class AnalysisCacheKey(
        val productId: String,
        val ingredientsSignature: String,
        val allergensSignature: String,
        val halalModeEnabled: Boolean
    )

    private fun HistoryBaseSnapshot.toBaseRecords(): List<HistoryBaseRecord> {
        val formatter = SimpleDateFormat("MMM dd, HH:mm", localeForLanguage(languageCode))
        val analysisContext = createAnalysisContext(
            selectedAllergens = selectedAllergens,
            halalModeEnabled = halalModeEnabled
        )

        return history
            .map { item ->
                HistoryBaseRecord(
                    uiModel = HistoryRecordUiState(
                        id = item.scanHistory.id,
                        productId = item.product.id,
                        productName = item.product.name,
                        brand = item.product.brand,
                        isFavorite = item.product.isFavorite,
                        scannedAtLabel = formatter.format(Date(item.scanHistory.scannedAt)),
                        status = historyStatus(
                            product = item.product,
                            analysisContext = analysisContext
                        )
                    )
                    ,
                    normalizedProductName = normalizeForSearch(item.product.name),
                    normalizedBrand = normalizeForSearch(item.product.brand)
                )
            }
    }

    private fun HistoryProjectionSnapshot.projectItems(): List<HistoryRecordUiState> {
        val normalizedQuery = normalizeForSearch(query)

        return baseRecords.asSequence()
            .filter { record ->
                when (filter) {
                    HistoryFilter.ALL -> true
                    HistoryFilter.FAVORITES -> record.uiModel.isFavorite
                }
            }
            .filter { record ->
                normalizedQuery.isBlank() ||
                    record.normalizedProductName.contains(normalizedQuery) ||
                    record.normalizedBrand.contains(normalizedQuery)
            }
            .map { it.uiModel }
            .toList()
    }

    private fun localeForLanguage(code: String): Locale {
        return when (code) {
            "ru" -> Locale.forLanguageTag("ru")
            "en" -> Locale.ENGLISH
            else -> Locale.forLanguageTag("uz-UZ")
        }
    }

    private fun historyStatus(
        product: ProductEntity,
        analysisContext: HistoryAnalysisContext
    ): SafetyStatus {
        val cacheKey = AnalysisCacheKey(
            productId = product.id,
            ingredientsSignature = product.ingredients.joinToString("|") { ingredient ->
                normalizeForSearch(ingredient.name)
            },
            allergensSignature = analysisContext.allergensSignature,
            halalModeEnabled = analysisContext.halalModeEnabled
        )

        synchronized(analysisStatusCache) {
            analysisStatusCache[cacheKey]
        }?.let { cachedStatus ->
            return cachedStatus
        }

        val status = AnalysisEngine.analyze(
            ingredients = product.ingredients.map { it.name },
            settings = AnalysisSettings(
                selectedAllergens = analysisContext.selectedAllergens,
                halalModeEnabled = analysisContext.halalModeEnabled
            )
        ).safetyStatus

        synchronized(analysisStatusCache) {
            analysisStatusCache[cacheKey] = status
        }

        return status
    }

    private fun normalizeForSearch(value: String): String {
        return value.trim().lowercase(Locale.ROOT)
    }

    private companion object {
        private const val ANALYSIS_CACHE_MAX_SIZE = 256
    }

    private fun createAnalysisContext(
        selectedAllergens: Set<Allergen>,
        halalModeEnabled: Boolean
    ): HistoryAnalysisContext {
        val mappedAllergens = selectedAllergens.mapNotNull { allergen ->
            when (allergen) {
                Allergen.MILK -> "dairy"
                Allergen.GLUTEN -> "gluten"
                Allergen.PEANUTS, Allergen.TREE_NUTS -> "nuts"
                else -> null
            }
        }.toSortedSet()

        return HistoryAnalysisContext(
            selectedAllergens = mappedAllergens,
            allergensSignature = mappedAllergens.joinToString("|"),
            halalModeEnabled = halalModeEnabled
        )
    }
}
