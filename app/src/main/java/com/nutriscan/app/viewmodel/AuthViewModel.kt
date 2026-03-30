package com.nutriscan.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    }

    private val _uiState = MutableStateFlow(
        AuthUiState(isAuthenticated = auth?.currentUser != null)
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        val firebaseAuth = auth ?: run {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Firebase not configured"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        firebaseAuth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    isAuthenticated = true,
                    errorMessage = null
                )
            }
            .addOnFailureListener { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Authentication failed"
                )
            }
    }

    fun register(email: String, password: String) {
        val firebaseAuth = auth ?: run {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Firebase not configured"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        firebaseAuth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    isAuthenticated = true,
                    errorMessage = null
                )
            }
            .addOnFailureListener { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Registration failed"
                )
            }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetAuthState() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = null
            )
        }
    }
}
