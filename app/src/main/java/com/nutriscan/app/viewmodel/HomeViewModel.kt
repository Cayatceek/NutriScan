package com.nutriscan.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.data.local.SettingsDataStore
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.data.repository.ProductRepository
import com.nutriscan.app.data.repository.ProductRepositoryImpl
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class HomeUiState(
    val searchQuery: String = "",
    val searchResults: List<ProductEntity> = emptyList(),
    val recentScans: List<ProductEntity> = emptyList(),
    val selectedAllergensCount: Int = 0,
    val halalModeEnabled: Boolean = false,
    val healthyModeEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProductRepository = ProductRepositoryImpl(application)
    private val settingsDataStore = SettingsDataStore(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        loadRecentScans()
        observeModeSettings()
        
        // Debounced search
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .flatMapLatest { query ->
                    if (query.length >= 2) {
                        _uiState.value = _uiState.value.copy(isSearching = true)
                        repository.searchProducts(query)
                    } else {
                        flowOf(emptyList())
                    }
                }
                .collect { results ->
                    _uiState.value = _uiState.value.copy(
                        searchResults = results,
                        isSearching = false
                    )
                }
        }
    }

    private fun observeModeSettings() {
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

    private fun loadRecentScans() {
        viewModelScope.launch {
            repository.getRecentScans().collect { scans ->
                _uiState.value = _uiState.value.copy(
                    recentScans = scans
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _searchQuery.value = query
    }

    fun onSearchSubmit() {
        val query = _uiState.value.searchQuery
        if (query.length >= 2) {
            _searchQuery.value = query
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchResults = emptyList()
        )
        _searchQuery.value = ""
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
