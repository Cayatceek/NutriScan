package com.nutriscan.app.domain.analysis

import com.nutriscan.app.model.SafetyStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisEngineTest {

    @Test
    fun analyze_usesConfiguredRules_whenCustomRuleSetIsProvided() {
        val customRuleSet = AnalysisRuleSet(
            allergenRules = listOf(
                AllergenRule(
                    keyword = "soy",
                    category = "soy",
                    severity = SafetyStatus.CAUTION,
                    explanation = "Soy is a configured allergen rule."
                )
            ),
            harmfulIngredientRules = emptyList(),
            dietaryRules = emptyList()
        )

        val analysis = AnalysisEngine.analyze(
            ingredients = listOf("Soy lecithin", "Salt"),
            settings = AnalysisSettings(selectedAllergens = setOf("soy")),
            ruleSet = customRuleSet
        )

        assertEquals(SafetyStatus.DANGER, analysis.safetyStatus)
        assertTrue(analysis.detectedAllergens.contains("Soy"))
    }

    @Test
    fun analyze_appliesMultipleConfiguredRules_andKeepsHighestSeverity() {
        val analysis = AnalysisEngine.analyze(
            ingredients = listOf("Milk powder", "Sugar", "Glucose syrup", "E621", "Gelatin"),
            settings = AnalysisSettings(
                selectedAllergens = setOf("dairy"),
                halalModeEnabled = true
            )
        )

        assertEquals(SafetyStatus.DANGER, analysis.safetyStatus)
        assertTrue(analysis.detectedAllergens.contains("Dairy"))
        assertTrue(analysis.harmfulIngredients.contains("E621"))
        assertTrue(analysis.harmfulIngredients.contains("High sugar"))
        assertTrue(analysis.warnings.any { it.title == "Not halal" })
    }

    @Test
    fun analyze_returnsSafe_whenNoRulesAreTriggered() {
        val analysis = AnalysisEngine.analyze(
            ingredients = listOf("Water", "Salt", "Rice flour"),
            settings = AnalysisSettings()
        )

        assertEquals(SafetyStatus.SAFE, analysis.safetyStatus)
        assertTrue(analysis.warnings.isEmpty())
    }
}
