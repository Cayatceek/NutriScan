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
import com.nutriscan.app.localization.LocaleManager
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
        LocaleManager.applyLanguage(context, language)
    }

    fun getCurrentLanguage(context: Context): String {
        return LocaleManager.currentAppLanguage(context).code
    }
}
