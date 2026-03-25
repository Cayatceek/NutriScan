package com.nutriscan.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.data.local.entity.ScanHistoryWithProduct
import com.nutriscan.app.data.repository.ScanHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class HistoryUiState(
    val historyItems: List<ScanHistoryWithProduct> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedFilter: HistoryFilter = HistoryFilter.ALL
)

enum class HistoryFilter {
    ALL,
    FAVORITES
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val scanHistoryRepository = ScanHistoryRepository(application)

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            scanHistoryRepository.getScanHistoryWithProducts()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
                .collect { history ->
                    _uiState.value = _uiState.value.copy(
                        historyItems = history,
                        isLoading = false
                    )
                }
        }
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
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setFilter(filter: HistoryFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun getFilteredHistory(): List<ScanHistoryWithProduct> {
        val query = _uiState.value.searchQuery.lowercase()
        val filteredByFavorite = when (_uiState.value.selectedFilter) {
            HistoryFilter.ALL -> _uiState.value.historyItems
            HistoryFilter.FAVORITES -> _uiState.value.historyItems.filter { item -> item.product.isFavorite }
        }

        return if (query.isBlank()) {
            filteredByFavorite
        } else {
            filteredByFavorite.filter { item ->
                item.product.name.lowercase().contains(query) ||
                item.product.brand.lowercase().contains(query)
            }
        }
    }
}
