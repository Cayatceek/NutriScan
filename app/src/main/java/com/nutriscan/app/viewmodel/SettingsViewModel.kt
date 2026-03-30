package com.nutriscan.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.R
import com.nutriscan.app.localization.AppLanguage
import com.nutriscan.app.localization.UiText
import com.nutriscan.app.localization.uiText
import com.nutriscan.app.data.local.SettingsDataStore
import com.nutriscan.app.data.repository.ScanHistoryRepository
import com.nutriscan.app.domain.analyzer.Allergen
import com.nutriscan.app.ui.theme.ThemeManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
    val historyCount: Int = 0,
    val showClearHistoryDialog: Boolean = false,
    val errorMessage: UiText? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val scanHistoryRepository = ScanHistoryRepository(application)
    private val settingsDataStore = SettingsDataStore(application)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            selectedLanguage = AppLanguage.fallback.code
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var settingsJob: Job? = null
    private var historyCountJob: Job? = null

    init {
        startSettingsCollection()
        refreshHistoryCount()
    }

    fun retryLoad() {
        settingsJob?.cancel()
        settingsJob = null
        _uiState.update { it.copy(errorMessage = null) }
        startSettingsCollection()
        refreshHistoryCount()
    }

    fun setLanguage(language: String) {
        if (language == _uiState.value.selectedLanguage) return

        viewModelScope.launch {
            settingsDataStore.setLanguage(language)
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
            _uiState.update {
                it.copy(
                    historyCount = 0,
                    showClearHistoryDialog = false
                )
            }
        }
    }

    private fun startSettingsCollection() {
        if (settingsJob != null) return

        settingsJob = viewModelScope.launch {
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
                SettingsSnapshot(
                    selectedLanguage = values[0] as String,
                    selectedTheme = values[1] as String,
                    halalModeEnabled = values[2] as Boolean,
                    healthyModeEnabled = values[3] as Boolean,
                    veganModeEnabled = values[4] as Boolean,
                    notificationsEnabled = values[5] as Boolean,
                    selectedAllergens = values[6] as Set<Allergen>
                )
            }
                .catch { throwable ->
                    settingsJob = null
                    _uiState.update {
                        it.copy(
                            errorMessage = throwable.message
                                ?.takeIf { message -> message.isNotBlank() }
                                ?.let(UiText::DynamicString)
                                ?: uiText(R.string.error_generic)
                        )
                    }
                }
                .collect { snapshot ->
                    _uiState.update {
                        it.copy(
                            selectedLanguage = snapshot.selectedLanguage,
                            selectedTheme = snapshot.selectedTheme,
                            selectedAllergens = snapshot.selectedAllergens,
                            halalModeEnabled = snapshot.halalModeEnabled,
                            healthyModeEnabled = snapshot.healthyModeEnabled,
                            veganModeEnabled = snapshot.veganModeEnabled,
                            notificationsEnabled = snapshot.notificationsEnabled,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    private fun refreshHistoryCount() {
        historyCountJob?.cancel()
        historyCountJob = viewModelScope.launch {
            runCatching { scanHistoryRepository.getHistoryCount() }
                .onSuccess { count ->
                    _uiState.update { it.copy(historyCount = count) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            errorMessage = throwable.message
                                ?.takeIf { message -> message.isNotBlank() }
                                ?.let(UiText::DynamicString)
                                ?: uiText(R.string.error_generic)
                        )
                    }
                }
        }
    }

    fun getAppVersion(): String {
        return "1.0.0"
    }

    private data class SettingsSnapshot(
        val selectedLanguage: String,
        val selectedTheme: String,
        val selectedAllergens: Set<Allergen>,
        val halalModeEnabled: Boolean,
        val healthyModeEnabled: Boolean,
        val veganModeEnabled: Boolean,
        val notificationsEnabled: Boolean
    )
}
