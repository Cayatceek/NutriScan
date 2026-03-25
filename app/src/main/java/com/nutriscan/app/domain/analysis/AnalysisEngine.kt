package com.nutriscan.app.domain.analysis

import com.nutriscan.app.model.Ingredient
import com.nutriscan.app.model.IngredientType
import com.nutriscan.app.model.Product
import com.nutriscan.app.model.ProductAnalysis
import com.nutriscan.app.model.Recommendation
import com.nutriscan.app.model.SafetyStatus
import com.nutriscan.app.model.Warning

data class AnalysisSettings(
    val selectedAllergens: Set<String> = emptySet(),
    val halalModeEnabled: Boolean = false
)

object AnalysisEngine {

    private const val HALAL_TAG = "halal"

    fun analyze(
        product: Product,
        settings: AnalysisSettings,
        ruleSet: AnalysisRuleSet = AnalysisRuleConfig.defaultRuleSet
    ): ProductAnalysis {
        return analyze(product.ingredients.map { it.name }, settings, ruleSet)
    }

    fun analyze(
        ingredients: List<String>,
        settings: AnalysisSettings,
        ruleSet: AnalysisRuleSet = AnalysisRuleConfig.defaultRuleSet
    ): ProductAnalysis {
        val normalizedIngredients = ingredients
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val loweredIngredients = normalizedIngredients.map { it.lowercase() }
        val selectedAllergens = settings.selectedAllergens.map { it.lowercase() }.toSet()
        val allergenMatches = matchAllergenRules(loweredIngredients, ruleSet.allergenRules)
        val harmfulMatches = matchHarmfulRules(loweredIngredients, ruleSet.harmfulIngredientRules)
        val dietaryMatches = matchDietaryRules(loweredIngredients, ruleSet.dietaryRules)
        val matchedSelectedAllergens = allergenMatches
            .filter { match -> match.rule.category.lowercase() in selectedAllergens }
            .map { match -> match.rule.displayName }
        val detectedAllergens = allergenMatches.map { match -> match.rule.displayName }
        val harmfulIngredients = harmfulMatches.map { match -> match.rule.displayName }
        val dietaryWarnings = dietaryMatches.map { match -> match.rule.displayName }

        val warnings = buildList {
            if (matchedSelectedAllergens.isNotEmpty()) {
                add(
                    Warning(
                        title = "Selected allergen detected",
                        message = "Matches your settings: ${matchedSelectedAllergens.joinToString(", ")}."
                    )
                )
            } else if (detectedAllergens.isNotEmpty()) {
                add(
                    Warning(
                        title = "Potential allergen detected",
                        message = detectedAllergens.joinToString(", ")
                    )
                )
            }

            if (harmfulIngredients.isNotEmpty()) {
                add(
                    Warning(
                        title = "Harmful ingredients detected",
                        message = harmfulIngredients.joinToString(", ")
                    )
                )
            }

            if (dietaryWarnings.isNotEmpty()) {
                add(
                    Warning(
                        title = "Not halal",
                        message = "Contains: ${dietaryWarnings.joinToString(", ")}."
                    )
                )
            }
        }

        val safetyStatus = resolveSafetyStatus(
            allergenMatches = allergenMatches,
            harmfulMatches = harmfulMatches,
            dietaryMatches = dietaryMatches,
            selectedAllergens = selectedAllergens,
            halalModeEnabled = settings.halalModeEnabled
        )

        val explanation = buildExplanation(
            matchedSelectedAllergens = matchedSelectedAllergens,
            allergenMatches = allergenMatches,
            harmfulMatches = harmfulMatches,
            dietaryMatches = dietaryMatches,
            halalModeEnabled = settings.halalModeEnabled
        )

        val recommendation = buildRecommendation(safetyStatus)

        return ProductAnalysis(
            safetyStatus = safetyStatus,
            detectedAllergens = detectedAllergens,
            harmfulIngredients = harmfulIngredients,
            warnings = warnings,
            explanation = explanation,
            recommendation = recommendation
        )
    }

    fun createProductFromIngredients(ingredients: List<String>): Product {
        return Product(
            id = "analysis-preview",
            name = "Analyzed Product",
            brand = "NutriScan",
            nutriscore = com.nutriscan.app.model.NutriScore.C,
            ingredients = ingredients.map { Ingredient(name = it, type = IngredientType.NEUTRAL) }
        )
    }

    private fun matchAllergenRules(
        ingredients: List<String>,
        rules: List<AllergenRule>
    ): List<AllergenRuleMatch> {
        return rules.mapNotNull { rule ->
            val matchedIngredients = ingredients.filter { ingredient ->
                ingredient.contains(rule.keyword.lowercase())
            }
            if (matchedIngredients.isEmpty()) {
                null
            } else {
                AllergenRuleMatch(rule = rule, matchedIngredients = matchedIngredients)
            }
        }
    }

    private fun matchHarmfulRules(
        ingredients: List<String>,
        rules: List<HarmfulIngredientRule>
    ): List<HarmfulRuleMatch> {
        return rules.mapNotNull { rule ->
            val keywords = setOf(rule.keyword.lowercase()) + rule.aliases.map { alias -> alias.lowercase() }
            val matchedIngredients = ingredients.filter { ingredient ->
                keywords.any { keyword -> ingredient.contains(keyword) }
            }
            val topWindowMatched = rule.topIngredientWindow?.let { window ->
                ingredients.take(window).any { ingredient ->
                    keywords.any { keyword -> ingredient.contains(keyword) }
                }
            } ?: false
            val thresholdReached = matchedIngredients.size >= rule.matchThreshold

            if (thresholdReached || topWindowMatched) {
                HarmfulRuleMatch(rule = rule, matchedIngredients = matchedIngredients)
            } else {
                null
            }
        }
    }

    private fun matchDietaryRules(
        ingredients: List<String>,
        rules: List<DietaryRule>
    ): List<DietaryRuleMatch> {
        return rules.mapNotNull { rule ->
            val matchedIngredients = ingredients.filter { ingredient ->
                ingredient.contains(rule.keyword.lowercase())
            }
            if (matchedIngredients.isEmpty()) {
                null
            } else {
                DietaryRuleMatch(rule = rule, matchedIngredients = matchedIngredients)
            }
        }
    }

    private fun resolveSafetyStatus(
        allergenMatches: List<AllergenRuleMatch>,
        harmfulMatches: List<HarmfulRuleMatch>,
        dietaryMatches: List<DietaryRuleMatch>,
        selectedAllergens: Set<String>,
        halalModeEnabled: Boolean
    ): SafetyStatus {
        val severities = buildList {
            allergenMatches.forEach { match ->
                val severity = if (match.rule.category.lowercase() in selectedAllergens) {
                    SafetyStatus.DANGER
                } else {
                    match.rule.severity
                }
                add(severity)
            }

            harmfulMatches.forEach { match ->
                add(match.rule.severity)
            }

            dietaryMatches.forEach { match ->
                val severity = if (halalModeEnabled && match.rule.tags.any { tag -> tag.equals(HALAL_TAG, ignoreCase = true) }) {
                    SafetyStatus.DANGER
                } else {
                    match.rule.severity
                }
                add(severity)
            }
        }

        return severities.maxByOrNull(::severityPriority) ?: SafetyStatus.SAFE
    }

    private fun buildExplanation(
        matchedSelectedAllergens: List<String>,
        allergenMatches: List<AllergenRuleMatch>,
        harmfulMatches: List<HarmfulRuleMatch>,
        dietaryMatches: List<DietaryRuleMatch>,
        halalModeEnabled: Boolean
    ): String {
        val parts = mutableListOf<String>()
        val detectedAllergens = allergenMatches.map { match -> match.rule.displayName }
        val harmfulIngredients = harmfulMatches.map { match -> match.rule.displayName }
        val dietaryWarnings = dietaryMatches.map { match -> match.rule.displayName }

        if (matchedSelectedAllergens.isNotEmpty()) {
            parts += "This product matches your selected allergens: ${matchedSelectedAllergens.joinToString(", ")}."
        } else if (detectedAllergens.isNotEmpty()) {
            parts += "Potential allergens were detected: ${detectedAllergens.joinToString(", ")}."
        }

        if (harmfulIngredients.isNotEmpty()) {
            parts += "Potentially harmful ingredients were found: ${harmfulIngredients.joinToString(", ")}."
            parts += harmfulMatches.joinToString(" ") { match -> match.rule.explanation }
        }

        if (dietaryWarnings.isNotEmpty()) {
            val halalNote = if (halalModeEnabled) {
                " This conflicts with halal mode."
            } else {
                ""
            }
            parts += "The ingredient list suggests this product is not halal because it contains ${dietaryWarnings.joinToString(", ")}.$halalNote"
            parts += dietaryMatches.joinToString(" ") { match -> match.rule.explanation }
        }

        if (allergenMatches.isNotEmpty()) {
            parts += allergenMatches
                .filterNot { match -> match.rule.displayName in matchedSelectedAllergens }
                .joinToString(" ") { match -> match.rule.explanation }
        }

        if (parts.isEmpty()) {
            parts += "No major warnings were detected based on the current rules and selected settings."
        }

        return parts.joinToString(" ")
    }

    private fun buildRecommendation(safetyStatus: SafetyStatus): Recommendation {
        return when (safetyStatus) {
            SafetyStatus.DANGER -> Recommendation(
                title = "Avoid this product",
                message = "The detected ingredients conflict with your active safety preferences."
            )
            SafetyStatus.CAUTION -> Recommendation(
                title = "Review before consuming",
                message = "Check the warnings and decide if this product fits your needs."
            )
            SafetyStatus.SAFE -> Recommendation(
                title = "Looks acceptable",
                message = "No high-priority issues were detected by the current analysis rules."
            )
        }
    }

    private fun severityPriority(status: SafetyStatus): Int {
        return when (status) {
            SafetyStatus.SAFE -> 0
            SafetyStatus.CAUTION -> 1
            SafetyStatus.DANGER -> 2
        }
    }

    private data class AllergenRuleMatch(
        val rule: AllergenRule,
        val matchedIngredients: List<String>
    )

    private data class HarmfulRuleMatch(
        val rule: HarmfulIngredientRule,
        val matchedIngredients: List<String>
    )

    private data class DietaryRuleMatch(
        val rule: DietaryRule,
        val matchedIngredients: List<String>
    )
}
