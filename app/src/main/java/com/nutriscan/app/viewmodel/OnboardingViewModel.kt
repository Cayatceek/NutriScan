package com.nutriscan.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.nutriscan.app.data.local.SettingsDataStore
import com.nutriscan.app.ui.theme.LanguageManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class OnboardingNavigationEvent {
    data object NavigateToAuth : OnboardingNavigationEvent()
    data object NavigateToHome : OnboardingNavigationEvent()
}

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class OnboardingUiState(
    val selectedLanguage: String = "system",
    val isLanguageSelected: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val isLoggedIn: Boolean = false,
    val authState: AuthState = AuthState.Idle
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null // Firebase not configured — auth features disabled
    }

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<OnboardingNavigationEvent>()
    val navigationEvent: SharedFlow<OnboardingNavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            checkOnboardingStatus()
        }
    }

    private suspend fun checkOnboardingStatus() {
        val hasCompleted = settingsDataStore.hasCompletedOnboarding.first()
        val savedLanguage = settingsDataStore.language.first()
        val currentUser = auth?.currentUser

        _uiState.value = _uiState.value.copy(
            selectedLanguage = savedLanguage,
            isLanguageSelected = savedLanguage != "system",
            hasCompletedOnboarding = hasCompleted,
            isLoggedIn = currentUser != null
        )

        when {
            !hasCompleted -> {
                // Stay on splash, user needs to complete onboarding
            }
            auth == null || currentUser == null -> {
                _navigationEvent.emit(OnboardingNavigationEvent.NavigateToAuth)
            }
            else -> {
                _navigationEvent.emit(OnboardingNavigationEvent.NavigateToHome)
            }
        }
    }

    fun selectLanguage(language: String) {
        viewModelScope.launch {
            settingsDataStore.setLanguage(language)
            LanguageManager.applyLanguage(getApplication(), language)

            _uiState.value = _uiState.value.copy(
                selectedLanguage = language,
                isLanguageSelected = true
            )
        }
    }

    fun continueToAuth() {
        viewModelScope.launch {
            settingsDataStore.setOnboardingCompleted(true)
            _navigationEvent.emit(OnboardingNavigationEvent.NavigateToAuth)
        }
    }

    fun skipAuth() {
        viewModelScope.launch {
            settingsDataStore.setOnboardingCompleted(true)
            _navigationEvent.emit(OnboardingNavigationEvent.NavigateToHome)
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(authState = AuthState.Loading)

            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                (auth ?: throw IllegalStateException("Firebase not configured"))
                    .signInWithCredential(credential)
                    .addOnSuccessListener { result ->
                        result.user?.let { user ->
                            _uiState.value = _uiState.value.copy(
                                authState = AuthState.Success(user),
                                isLoggedIn = true
                            )
                            viewModelScope.launch {
                                _navigationEvent.emit(OnboardingNavigationEvent.NavigateToHome)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        _uiState.value = _uiState.value.copy(
                            authState = AuthState.Error(e.message ?: "Authentication failed")
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    authState = AuthState.Error(e.message ?: "Authentication failed")
                )
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(authState = AuthState.Loading)

            (auth ?: run {
                _uiState.value = _uiState.value.copy(authState = AuthState.Error("Firebase not configured"))
                return@launch
            }).signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    result.user?.let { user ->
                        _uiState.value = _uiState.value.copy(
                            authState = AuthState.Success(user),
                            isLoggedIn = true
                        )
                        viewModelScope.launch {
                            _navigationEvent.emit(OnboardingNavigationEvent.NavigateToHome)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    _uiState.value = _uiState.value.copy(
                        authState = AuthState.Error(e.message ?: "Authentication failed")
                    )
                }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(authState = AuthState.Loading)

            (auth ?: run {
                _uiState.value = _uiState.value.copy(authState = AuthState.Error("Firebase not configured"))
                return@launch
            }).createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    result.user?.let { user ->
                        _uiState.value = _uiState.value.copy(
                            authState = AuthState.Success(user),
                            isLoggedIn = true
                        )
                        viewModelScope.launch {
                            _navigationEvent.emit(OnboardingNavigationEvent.NavigateToHome)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    _uiState.value = _uiState.value.copy(
                        authState = AuthState.Error(e.message ?: "Registration failed")
                    )
                }
        }
    }

    fun clearAuthError() {
        _uiState.value = _uiState.value.copy(authState = AuthState.Idle)
    }

    fun isUserLoggedIn(): Boolean {
        return auth?.currentUser != null
    }
}
