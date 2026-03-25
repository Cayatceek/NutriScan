package com.nutriscan.app.domain.analysis

import com.nutriscan.app.model.SafetyStatus

data class AllergenRule(
    val keyword: String,
    val category: String,
    val severity: SafetyStatus,
    val explanation: String,
    val tags: List<String> = emptyList(),
    val displayName: String = category.replaceFirstChar { char -> char.uppercase() }
)

data class HarmfulIngredientRule(
    val keyword: String,
    val category: String,
    val severity: SafetyStatus,
    val explanation: String,
    val tags: List<String> = emptyList(),
    val displayName: String = keyword.uppercase(),
    val aliases: Set<String> = emptySet(),
    val matchThreshold: Int = 1,
    val topIngredientWindow: Int? = null
)

data class DietaryRule(
    val keyword: String,
    val category: String,
    val severity: SafetyStatus,
    val explanation: String,
    val tags: List<String> = emptyList(),
    val displayName: String = keyword.replaceFirstChar { char -> char.uppercase() }
)

data class AnalysisRuleSet(
    val allergenRules: List<AllergenRule>,
    val harmfulIngredientRules: List<HarmfulIngredientRule>,
    val dietaryRules: List<DietaryRule>
)
