package com.nutriscan.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import com.nutriscan.app.data.local.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object ThemeManager {

    fun applyTheme(theme: String) {
        val nightMode = when (theme) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    @Composable
    fun isDarkTheme(settingsDataStore: SettingsDataStore): Boolean {
        val theme by settingsDataStore.theme.collectAsState(initial = "system")
        return when (theme) {
            "light" -> false
            "dark" -> true
            else -> isSystemInDarkTheme()
        }
    }
}

object LanguageManager {

    fun applyLanguage(context: Context, language: String) {
        val localeList = when (language) {
            "en" -> LocaleListCompat.forLanguageTags("en-US")
            "ru" -> LocaleListCompat.forLanguageTags("ru-RU")
            "uz" -> LocaleListCompat.forLanguageTags("uz-UZ")
            else -> LocaleListCompat.getEmptyLocaleList() // System default
        }
        
        // Apply locale using AppCompatDelegate
        AppCompatDelegate.setApplicationLocales(localeList)
        
        // For older devices or if the above doesn't work immediately,
        // we also update the configuration directly
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            updateResourcesLegacy(context, language)
        }
    }
    
    private fun updateResourcesLegacy(context: Context, language: String) {
        if (language == "system") return
        
        val locale = java.util.Locale(language)
        java.util.Locale.setDefault(locale)
        
        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    fun getCurrentLanguage(context: Context): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return when {
            locales.isEmpty -> "system"
            locales[0]?.language == "uz" -> "uz"
            locales[0]?.language == "ru" -> "ru"
            locales[0]?.language?.startsWith("en") == true -> "en"
            else -> "system"
        }
    }
}
