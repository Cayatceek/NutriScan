package com.nutriscan.app.localization

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleManager {

    fun resolveStartupLanguage(
        savedLanguageCode: String?,
        hasManualSelection: Boolean
    ): AppLanguage {
        return if (hasManualSelection) {
            AppLanguage.fromCode(savedLanguageCode) ?: AppLanguage.fallback
        } else {
            AppLanguage.fromLocale(systemLocale()) ?: AppLanguage.fallback
        }
    }

    fun currentAppLanguage(context: Context): AppLanguage {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val explicitLocale = appLocales[0]
        return AppLanguage.fromLocale(explicitLocale)
            ?: AppLanguage.fromLocale(context.resources.configuration.locales[0])
            ?: AppLanguage.fallback
    }

    fun applyLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.languageTag)
        )
    }

    fun applyLanguage(languageCode: String?) {
        applyLanguage(AppLanguage.fromCode(languageCode) ?: AppLanguage.fallback)
    }

    fun applyLanguage(context: Context, languageCode: String?) {
        applyLanguage(languageCode)
    }

    private fun systemLocale(): Locale? {
        return LocaleListCompat.getAdjustedDefault()[0]
    }
}
