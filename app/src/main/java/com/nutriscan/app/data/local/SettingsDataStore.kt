package com.nutriscan.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nutriscan.app.domain.analyzer.Allergen
import com.nutriscan.app.localization.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val MANUAL_LANGUAGE_KEY = booleanPreferencesKey("manual_language")
        val THEME_KEY = stringPreferencesKey("theme")
        val HALAL_MODE_KEY = booleanPreferencesKey("halal_mode")
        val HEALTHY_MODE_KEY = booleanPreferencesKey("healthy_mode")
        val VEGAN_MODE_KEY = booleanPreferencesKey("vegan_mode")
        val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        val ONBOARDING_SHOWN_KEY = booleanPreferencesKey("onboarding_shown")
        
        fun getAllergenKey(allergen: Allergen) = booleanPreferencesKey("allergen_${allergen.name}")
    }

    // Language
    val language: Flow<String> = context.dataStore.data.map { preferences ->
        AppLanguage.fromCode(preferences[LANGUAGE_KEY])?.code ?: AppLanguage.fallback.code
    }

    val isLanguageManuallySelected: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MANUAL_LANGUAGE_KEY] ?: false
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
            preferences[MANUAL_LANGUAGE_KEY] = true
        }
    }

    suspend fun setResolvedLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
            preferences[MANUAL_LANGUAGE_KEY] = false
        }
    }

    // Theme
    val theme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "system"
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    // Halal Mode
    val halalMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HALAL_MODE_KEY] ?: false
    }

    suspend fun setHalalMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HALAL_MODE_KEY] = enabled
        }
    }

    val healthyMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HEALTHY_MODE_KEY] ?: false
    }

    suspend fun setHealthyMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HEALTHY_MODE_KEY] = enabled
        }
    }

    // Vegan Mode
    val veganMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VEGAN_MODE_KEY] ?: false
    }

    suspend fun setVeganMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VEGAN_MODE_KEY] = enabled
        }
    }

    // Notifications
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_KEY] ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
    }

    // Allergens
    fun isAllergenSelected(allergen: Allergen): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[getAllergenKey(allergen)] ?: false
    }

    suspend fun setAllergenSelected(allergen: Allergen, selected: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[getAllergenKey(allergen)] = selected
        }
    }

    // Get all selected allergens
    val selectedAllergens: Flow<Set<Allergen>> = context.dataStore.data.map { preferences ->
        Allergen.values().filter { allergen ->
            preferences[getAllergenKey(allergen)] == true
        }.toSet()
    }

    // Onboarding
    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED_KEY] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    // Onboarding Tutorial Shown
    val hasShownOnboarding: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_SHOWN_KEY] ?: false
    }

    suspend fun setOnboardingShown(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_SHOWN_KEY] = shown
        }
    }
}
