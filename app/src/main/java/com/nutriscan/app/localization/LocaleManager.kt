package com.nutriscan.app.localization

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
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
        return AppLanguage.fromLocale(currentLocale(context)) ?: AppLanguage.fallback
    }

    fun createLocalizedContext(
        baseContext: Context,
        languageCode: String?
    ): Context {
        return createLocalizedContext(
            baseContext = baseContext,
            baseConfiguration = baseContext.resources.configuration,
            languageCode = languageCode
        )
    }

    fun createLocalizedContext(
        baseContext: Context,
        baseConfiguration: Configuration,
        languageCode: String?
    ): Context {
        val localizedConfiguration = createLocalizedConfiguration(baseConfiguration, languageCode)
        val localizedResourcesContext = baseContext.createConfigurationContext(localizedConfiguration)
        return LocalizedContextWrapper(
            baseContext = baseContext,
            localizedResourcesContext = localizedResourcesContext
        )
    }

    fun createLocalizedConfiguration(
        baseConfiguration: Configuration,
        languageCode: String?
    ): Configuration {
        val locale = localeForLanguage(languageCode)
        return Configuration(baseConfiguration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setLocales(LocaleList(locale))
            }
        }
    }

    fun localeForLanguage(languageCode: String?): Locale {
        val language = AppLanguage.fromCode(languageCode) ?: AppLanguage.fallback
        return Locale.forLanguageTag(language.languageTag)
    }

    private fun currentLocale(context: Context): Locale? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }

    private fun systemLocale(): Locale? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources.getSystem().configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            Resources.getSystem().configuration.locale
        }
    }
}

private class LocalizedContextWrapper(
    baseContext: Context,
    localizedResourcesContext: Context
) : ContextWrapper(baseContext) {

    private val localizedResources = localizedResourcesContext.resources
    private val localizedTheme = localizedResourcesContext.theme

    override fun getResources(): Resources = localizedResources

    override fun getAssets(): AssetManager = localizedResources.assets

    override fun getTheme() = localizedTheme
}
