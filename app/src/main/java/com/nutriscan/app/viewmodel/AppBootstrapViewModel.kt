package com.nutriscan.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nutriscan.app.R
import com.nutriscan.app.data.local.SettingsDataStore
import com.nutriscan.app.localization.AppLanguage
import com.nutriscan.app.localization.LocaleManager
import com.nutriscan.app.localization.UiText
import com.nutriscan.app.localization.uiText
import com.nutriscan.app.ui.theme.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AppBootstrapUiState(
    val bootstrapState: BootstrapState = BootstrapState.Loading,
    val theme: String = "system",
    val languageCode: String = AppLanguage.fallback.code,
    val onboardingCompleted: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: UiText? = null
)

class AppBootstrapViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    }

    private val _uiState = MutableStateFlow(AppBootstrapUiState())
    val uiState: StateFlow<AppBootstrapUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var isLoading = false

    fun loadIfNeeded() {
        if (hasLoaded || isLoading) return

        viewModelScope.launch {
            isLoading = true
            _uiState.value = _uiState.value.copy(
                bootstrapState = BootstrapState.Loading,
                errorMessage = null
            )

            try {
                val savedTheme = settingsDataStore.theme.first()
                ThemeManager.applyTheme(savedTheme)

                val savedLanguage = settingsDataStore.language.first()
                val hasManualLanguageSelection = settingsDataStore.isLanguageManuallySelected.first()
                val resolvedLanguage = LocaleManager.resolveStartupLanguage(
                    savedLanguageCode = savedLanguage,
                    hasManualSelection = hasManualLanguageSelection
                )

                if (!hasManualLanguageSelection && savedLanguage != resolvedLanguage.code) {
                    settingsDataStore.setResolvedLanguage(resolvedLanguage.code)
                }

                hasLoaded = true
                isLoading = false
                _uiState.value = AppBootstrapUiState(
                    bootstrapState = BootstrapState.Ready,
                    theme = savedTheme,
                    languageCode = resolvedLanguage.code,
                    onboardingCompleted = settingsDataStore.hasCompletedOnboarding.first(),
                    isAuthenticated = auth?.currentUser != null
                )
            } catch (throwable: Throwable) {
                _uiState.value = _uiState.value.copy(
                    bootstrapState = BootstrapState.Error,
                    languageCode = _uiState.value.languageCode,
                    errorMessage = throwable.message
                        ?.takeIf { it.isNotBlank() }
                        ?.let(UiText::DynamicString)
                        ?: uiText(R.string.error_generic)
                )
                isLoading = false
            }
        }
    }
}
