package com.nutriscan.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.localization.LocaleManager
import com.nutriscan.app.data.local.SettingsDataStore
import com.nutriscan.app.data.repository.ScanHistoryRepository
import com.nutriscan.app.domain.analyzer.Allergen
import com.nutriscan.app.ui.theme.LanguageManager
import com.nutriscan.app.ui.theme.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SettingsUiState(
    val selectedLanguage: String = "uz",
    val selectedTheme: String = "system",
    val selectedAllergens: Set<Allergen> = emptySet(),
    val halalModeEnabled: Boolean = false,
    val healthyModeEnabled: Boolean = false,
    val veganModeEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val historyCount: Int = 0,
    val showClearHistoryDialog: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val scanHistoryRepository = ScanHistoryRepository(application)
    private val settingsDataStore = SettingsDataStore(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadHistoryCount()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                settingsDataStore.language,
                settingsDataStore.theme,
                settingsDataStore.halalMode,
                settingsDataStore.healthyMode,
                settingsDataStore.veganMode,
                settingsDataStore.notificationsEnabled,
                settingsDataStore.selectedAllergens
            ) { values: Array<Any> ->
                @Suppress("UNCHECKED_CAST")
                SettingsUiState(
                    selectedLanguage = values[0] as String,
                    selectedTheme = values[1] as String,
                    halalModeEnabled = values[2] as Boolean,
                    healthyModeEnabled = values[3] as Boolean,
                    veganModeEnabled = values[4] as Boolean,
                    notificationsEnabled = values[5] as Boolean,
                    selectedAllergens = values[6] as Set<Allergen>
                )
            }.collect { state ->
                _uiState.value = _uiState.value.copy(
                    selectedLanguage = state.selectedLanguage,
                    selectedTheme = state.selectedTheme,
                    selectedAllergens = state.selectedAllergens,
                    halalModeEnabled = state.halalModeEnabled,
                    healthyModeEnabled = state.healthyModeEnabled,
                    veganModeEnabled = state.veganModeEnabled,
                    notificationsEnabled = state.notificationsEnabled
                )
            }
        }
    }

    private fun loadHistoryCount() {
        viewModelScope.launch {
            val count = scanHistoryRepository.getHistoryCount()
            _uiState.value = _uiState.value.copy(historyCount = count)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            settingsDataStore.setLanguage(language)
            LocaleManager.applyLanguage(getApplication(), language)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsDataStore.setTheme(theme)
            ThemeManager.applyTheme(theme)
        }
    }

    fun toggleAllergen(allergen: Allergen) {
        val current = _uiState.value.selectedAllergens
        val newSet = if (current.contains(allergen)) {
            current - allergen
        } else {
            current + allergen
        }
        viewModelScope.launch {
            settingsDataStore.setAllergenSelected(allergen, !current.contains(allergen))
        }
    }

    fun setHalalMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setHalalMode(enabled)
        }
    }

    fun setHealthyMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setHealthyMode(enabled)
        }
    }

    fun setVeganMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setVeganMode(enabled)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setNotificationsEnabled(enabled)
        }
    }

    fun showClearHistoryDialog() {
        _uiState.value = _uiState.value.copy(showClearHistoryDialog = true)
    }

    fun dismissClearHistoryDialog() {
        _uiState.value = _uiState.value.copy(showClearHistoryDialog = false)
    }

    fun clearHistory() {
        viewModelScope.launch {
            scanHistoryRepository.deleteAllHistory()
            _uiState.value = _uiState.value.copy(
                historyCount = 0,
                showClearHistoryDialog = false
            )
        }
    }

    fun getAppVersion(): String {
        return "1.0.0"
    }
}
