package com.nutriscan.app.domain.analysis

import com.nutriscan.app.model.SafetyStatus

object AnalysisRuleConfig {

    val defaultRuleSet = AnalysisRuleSet(
        allergenRules = listOf(
            AllergenRule(
                keyword = "milk",
                category = "dairy",
                severity = SafetyStatus.CAUTION,
                explanation = "Milk indicates a dairy allergen that may trigger sensitive users."
            ),
            AllergenRule(
                keyword = "nuts",
                category = "nuts",
                severity = SafetyStatus.CAUTION,
                explanation = "Nuts are a common allergen and should be checked carefully."
            ),
            AllergenRule(
                keyword = "gluten",
                category = "gluten",
                severity = SafetyStatus.CAUTION,
                explanation = "Gluten may be unsafe for users with gluten sensitivity."
            )
        ),
        harmfulIngredientRules = listOf(
            HarmfulIngredientRule(
                keyword = "e102",
                category = "additives",
                severity = SafetyStatus.CAUTION,
                explanation = "E102 is an additive that some users prefer to avoid.",
                displayName = "E102"
            ),
            HarmfulIngredientRule(
                keyword = "e621",
                category = "additives",
                severity = SafetyStatus.CAUTION,
                explanation = "E621 is a flavor enhancer that some users prefer to avoid.",
                displayName = "E621"
            ),
            HarmfulIngredientRule(
                keyword = "sugar",
                category = "unhealthy",
                severity = SafetyStatus.CAUTION,
                explanation = "Multiple sugar sources or sugar near the top of the list suggests high sugar content.",
                tags = listOf("high_sugar"),
                displayName = "High sugar",
                aliases = setOf("glucose", "fructose", "syrup"),
                matchThreshold = 2,
                topIngredientWindow = 3
            )
        ),
        dietaryRules = listOf(
            DietaryRule(
                keyword = "gelatin",
                category = "halal",
                severity = SafetyStatus.CAUTION,
                explanation = "Gelatin may conflict with halal dietary preferences.",
                tags = listOf("halal"),
                displayName = "Gelatin"
            )
        )
    )
}
