package com.nutriscan.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.data.local.SettingsDataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppShellUiState(
    val theme: String = "system"
)

class AppShellViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)

    private val _uiState = MutableStateFlow(AppShellUiState())
    val uiState: StateFlow<AppShellUiState> = _uiState.asStateFlow()

    private var themeJob: Job? = null

    fun start(initialTheme: String) {
        if (themeJob != null) return

        _uiState.value = AppShellUiState(theme = initialTheme)
        themeJob = viewModelScope.launch {
            settingsDataStore.theme.collect { theme ->
                _uiState.value = _uiState.value.copy(theme = theme)
            }
        }
    }
}
