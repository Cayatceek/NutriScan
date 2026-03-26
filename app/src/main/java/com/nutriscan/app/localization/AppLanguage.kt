package com.nutriscan.app.localization

import java.util.Locale

enum class AppLanguage(
    val code: String,
    val languageTag: String
) {
    UZ("uz", "uz-UZ"),
    RU("ru", "ru-RU"),
    EN("en", "en-US");

    companion object {
        val fallback: AppLanguage = UZ

        fun fromCode(code: String?): AppLanguage? {
            return entries.firstOrNull { language ->
                language.code.equals(code, ignoreCase = true)
            }
        }

        fun fromLocale(locale: Locale?): AppLanguage? {
            return when (locale?.language?.lowercase()) {
                UZ.code -> UZ
                RU.code -> RU
                EN.code -> EN
                else -> null
            }
        }
    }
}
