package com.nutriscan.app.localization

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nutriscan.app.R
import com.nutriscan.app.domain.analyzer.Allergen
import com.nutriscan.app.model.IngredientType
import com.nutriscan.app.model.NutriScore
import com.nutriscan.app.model.SafetyStatus

sealed interface UiText {
    data class StringResource(
        @StringRes val resId: Int,
        val formatArgs: List<Any> = emptyList()
    ) : UiText

    data class DynamicString(
        val value: String
    ) : UiText
}

val LocalLocalizedAppContext = staticCompositionLocalOf<Context?> { null }

fun uiText(@StringRes resId: Int, vararg args: Any): UiText {
    return UiText.StringResource(resId = resId, formatArgs = args.toList())
}

fun UiText.resolve(context: Context): String {
    return when (this) {
        is UiText.DynamicString -> value
        is UiText.StringResource -> {
            if (formatArgs.isEmpty()) {
                context.getString(resId)
            } else {
                context.getString(resId, *formatArgs.toTypedArray())
            }
        }
    }
}

@Composable
fun UiText.asString(): String {
    LocalConfiguration.current
    return resolve(LocalLocalizedAppContext.current ?: LocalContext.current)
}

@Composable
fun UiText?.asStringOrNull(): String? {
    return this?.asString()
}

@StringRes
private fun languageLabelRes(code: String): Int {
    return when (code) {
        AppLanguage.RU.code -> R.string.language_russian
        AppLanguage.EN.code -> R.string.language_english
        else -> R.string.language_uzbek
    }
}

@Composable
fun localizedLanguageLabel(code: String): String = stringResource(languageLabelRes(code))

fun Context.localizedLanguageLabel(code: String): String = getString(languageLabelRes(code))

@StringRes
private fun themeLabelRes(code: String): Int {
    return when (code) {
        "light" -> R.string.settings_theme_light
        "dark" -> R.string.settings_theme_dark
        else -> R.string.settings_theme_system
    }
}

@Composable
fun localizedThemeLabel(code: String): String = stringResource(themeLabelRes(code))

@StringRes
private fun allergenRes(allergen: Allergen): Int {
    return when (allergen) {
        Allergen.GLUTEN -> R.string.allergen_gluten
        Allergen.MILK -> R.string.allergen_milk
        Allergen.EGGS -> R.string.allergen_eggs
        Allergen.PEANUTS -> R.string.allergen_peanuts
        Allergen.TREE_NUTS -> R.string.allergen_tree_nuts
        Allergen.SOY -> R.string.allergen_soy
        Allergen.FISH -> R.string.allergen_fish
        Allergen.SHELLFISH -> R.string.allergen_shellfish
        Allergen.SESAME -> R.string.allergen_sesame
        Allergen.SULFITES -> R.string.allergen_sulfites
    }
}

@Composable
fun localizedAllergenLabel(allergen: Allergen): String = stringResource(allergenRes(allergen))

@StringRes
private fun statusRes(status: SafetyStatus): Int {
    return when (status) {
        SafetyStatus.SAFE -> R.string.status_safe
        SafetyStatus.CAUTION -> R.string.status_caution
        SafetyStatus.DANGER -> R.string.status_danger
    }
}

@Composable
fun localizedStatusLabel(status: SafetyStatus): String = stringResource(statusRes(status))

fun Context.localizedStatusLabel(status: SafetyStatus): String = getString(statusRes(status))

@StringRes
private fun statusMessageRes(status: SafetyStatus): Int {
    return when (status) {
        SafetyStatus.SAFE -> R.string.result_status_message_safe
        SafetyStatus.CAUTION -> R.string.result_status_message_caution
        SafetyStatus.DANGER -> R.string.result_status_message_danger
    }
}

@Composable
fun localizedStatusMessage(status: SafetyStatus): String = stringResource(statusMessageRes(status))

@StringRes
private fun recommendationTitleRes(status: SafetyStatus): Int {
    return when (status) {
        SafetyStatus.SAFE -> R.string.recommendation_title_safe
        SafetyStatus.CAUTION -> R.string.recommendation_title_caution
        SafetyStatus.DANGER -> R.string.recommendation_title_danger
    }
}

@StringRes
private fun recommendationMessageRes(status: SafetyStatus): Int {
    return when (status) {
        SafetyStatus.SAFE -> R.string.recommendation_message_safe
        SafetyStatus.CAUTION -> R.string.recommendation_message_caution
        SafetyStatus.DANGER -> R.string.recommendation_message_danger
    }
}

@Composable
fun localizedRecommendation(status: SafetyStatus): Pair<String, String> {
    return stringResource(recommendationTitleRes(status)) to
        stringResource(recommendationMessageRes(status))
}

fun Context.localizedRecommendation(status: SafetyStatus): Pair<String, String> {
    return getString(recommendationTitleRes(status)) to getString(recommendationMessageRes(status))
}

@StringRes
private fun ingredientToneRes(type: IngredientType): Int {
    return when (type) {
        IngredientType.GOOD -> R.string.ingredient_tone_good
        IngredientType.BAD -> R.string.ingredient_tone_bad
        IngredientType.NEUTRAL -> R.string.ingredient_tone_neutral
    }
}

@Composable
fun localizedIngredientTone(type: IngredientType): String = stringResource(ingredientToneRes(type))

@StringRes
private fun nutriScoreNoteRes(score: NutriScore): Int {
    return when (score) {
        NutriScore.A -> R.string.nutriscore_a
        NutriScore.B -> R.string.nutriscore_b
        NutriScore.C -> R.string.nutriscore_c
        NutriScore.D -> R.string.nutriscore_d
        NutriScore.E -> R.string.nutriscore_e
    }
}

fun Context.localizedNutriScoreNote(score: NutriScore): String = getString(nutriScoreNoteRes(score))

@Composable
fun localizedDemoScenarioLabel(id: String): String {
    return stringResource(
        when (id) {
            "caution" -> R.string.demo_scenario_caution
            "dietary" -> R.string.demo_scenario_dietary
            "not_found" -> R.string.demo_scenario_not_found
            else -> R.string.demo_scenario_safe
        }
    )
}

fun Context.localizedAnalysisItem(raw: String): String {
    return when (raw.trim().lowercase()) {
        "dairy", "milk" -> getString(R.string.analysis_item_dairy)
        "nuts" -> getString(R.string.analysis_item_nuts)
        "gluten" -> getString(R.string.analysis_item_gluten)
        "high sugar" -> getString(R.string.analysis_item_high_sugar)
        "gelatin" -> getString(R.string.analysis_item_gelatin)
        "egg", "eggs" -> getString(R.string.allergen_eggs)
        "peanut", "peanuts" -> getString(R.string.allergen_peanuts)
        "tree nuts" -> getString(R.string.allergen_tree_nuts)
        "soy", "soya" -> getString(R.string.allergen_soy)
        "fish" -> getString(R.string.allergen_fish)
        "shellfish" -> getString(R.string.allergen_shellfish)
        "sesame" -> getString(R.string.allergen_sesame)
        "sulfites" -> getString(R.string.allergen_sulfites)
        "e102" -> "E102"
        "e621", "msg" -> "E621"
        "not halal" -> getString(R.string.result_warnings_dietary)
        else -> raw
    }
}
