package com.nutriscan.app.domain.analyzer

import com.nutriscan.app.data.local.entity.IngredientEntity
import com.nutriscan.app.model.NutriScore

object NutriScoreCalculator {

    data class NutritionalValues(
        val energyKcal: Double = 0.0,      // per 100g
        val sugars: Double = 0.0,           // per 100g
        val saturatedFat: Double = 0.0,     // per 100g
        val sodium: Double = 0.0,           // per 100g (mg)
        val proteins: Double = 0.0,         // per 100g
        val fiber: Double = 0.0,            // per 100g
        val fruitsVegetables: Double = 0.0  // percentage
    )

    fun calculateFromIngredients(ingredients: List<IngredientEntity>): NutriScore {
        // Estimate nutritional values based on ingredients
        val values = estimateNutritionalValues(ingredients)
        return calculate(values)
    }

    fun calculate(values: NutritionalValues): NutriScore {
        // Negative points (0-40)
        val energyPoints = calculateEnergyPoints(values.energyKcal)
        val sugarPoints = calculateSugarPoints(values.sugars)
        val satFatPoints = calculateSaturatedFatPoints(values.saturatedFat)
        val sodiumPoints = calculateSodiumPoints(values.sodium)
        
        val negativePoints = energyPoints + sugarPoints + satFatPoints + sodiumPoints
        
        // Positive points (0-15)
        val proteinPoints = calculateProteinPoints(values.proteins)
        val fiberPoints = calculateFiberPoints(values.fiber)
        val fvPoints = calculateFruitsVegetablesPoints(values.fruitsVegetables)
        
        val positivePoints = proteinPoints + fiberPoints + fvPoints
        
        // Final score
        val totalScore = negativePoints - positivePoints
        
        return when {
            totalScore <= -1 -> NutriScore.A
            totalScore <= 2 -> NutriScore.B
            totalScore <= 10 -> NutriScore.C
            totalScore <= 18 -> NutriScore.D
            else -> NutriScore.E
        }
    }

    private fun estimateNutritionalValues(ingredients: List<IngredientEntity>): NutritionalValues {
        // Heuristic estimation based on ingredient types
        var estimatedSugar = 0.0
        var estimatedFat = 0.0
        var estimatedProtein = 0.0
        var estimatedFiber = 0.0
        
        val ingredientNames = ingredients.map { it.name.lowercase() }
        
        // Sugar estimation
        if (ingredientNames.any { it.contains("sugar") || it.contains("qand") || it.contains("сахар") }) {
            estimatedSugar += 15.0
        }
        if (ingredientNames.any { it.contains("glucose") || it.contains("fructose") || it.contains("syrup") }) {
            estimatedSugar += 10.0
        }
        if (ingredientNames.any { it.contains("honey") || it.contains("asal") }) {
            estimatedSugar += 8.0
        }
        
        // Fat estimation
        if (ingredientNames.any { it.contains("oil") || it.contains("yog'") || it.contains("масло") }) {
            estimatedFat += 10.0
        }
        if (ingredientNames.any { it.contains("butter") || it.contains("saryog'") }) {
            estimatedFat += 15.0
        }
        
        // Protein estimation
        if (ingredientNames.any { it.contains("meat") || it.contains("go'sht") || it.contains("мясо") }) {
            estimatedProtein += 15.0
        }
        if (ingredientNames.any { it.contains("milk") || it.contains("sut") || it.contains("молоко") }) {
            estimatedProtein += 5.0
        }
        if (ingredientNames.any { it.contains("egg") || it.contains("tuxum") }) {
            estimatedProtein += 8.0
        }
        
        // Fiber estimation
        if (ingredientNames.any { it.contains("whole") || it.contains("grain") || it.contains("bran") }) {
            estimatedFiber += 8.0
        }
        if (ingredientNames.any { it.contains("oats") || it.contains("yulaf") }) {
            estimatedFiber += 5.0
        }
        
        return NutritionalValues(
            energyKcal = (estimatedSugar * 4) + (estimatedFat * 9) + (estimatedProtein * 4) + 50,
            sugars = estimatedSugar,
            saturatedFat = estimatedFat * 0.4,
            sodium = 200.0, // default assumption
            proteins = estimatedProtein,
            fiber = estimatedFiber,
            fruitsVegetables = if (ingredientNames.any { 
                it.contains("fruit") || it.contains("vegetable") || it.contains("meva") 
            }) 20.0 else 0.0
        )
    }

    private fun calculateEnergyPoints(kcal: Double): Int {
        return when {
            kcal <= 335 -> 0
            kcal <= 670 -> 1
            kcal <= 1005 -> 2
            kcal <= 1340 -> 3
            kcal <= 1675 -> 4
            kcal <= 2010 -> 5
            kcal <= 2345 -> 6
            kcal <= 2680 -> 7
            kcal <= 3015 -> 8
            kcal <= 3350 -> 9
            else -> 10
        }
    }

    private fun calculateSugarPoints(sugar: Double): Int {
        return when {
            sugar <= 4.5 -> 0
            sugar <= 9.0 -> 1
            sugar <= 13.5 -> 2
            sugar <= 18.0 -> 3
            sugar <= 22.5 -> 4
            sugar <= 27.0 -> 5
            sugar <= 31.0 -> 6
            sugar <= 36.0 -> 7
            sugar <= 40.0 -> 8
            sugar <= 45.0 -> 9
            else -> 10
        }
    }

    private fun calculateSaturatedFatPoints(satFat: Double): Int {
        return when {
            satFat <= 1.0 -> 0
            satFat <= 2.0 -> 1
            satFat <= 3.0 -> 2
            satFat <= 4.0 -> 3
            satFat <= 5.0 -> 4
            satFat <= 6.0 -> 5
            satFat <= 7.0 -> 6
            satFat <= 8.0 -> 7
            satFat <= 9.0 -> 8
            satFat <= 10.0 -> 9
            else -> 10
        }
    }

    private fun calculateSodiumPoints(sodiumMg: Double): Int {
        val sodiumG = sodiumMg / 1000
        return when {
            sodiumG <= 0.09 -> 0
            sodiumG <= 0.18 -> 1
            sodiumG <= 0.27 -> 2
            sodiumG <= 0.36 -> 3
            sodiumG <= 0.45 -> 4
            sodiumG <= 0.54 -> 5
            sodiumG <= 0.63 -> 6
            sodiumG <= 0.72 -> 7
            sodiumG <= 0.81 -> 8
            sodiumG <= 0.90 -> 9
            else -> 10
        }
    }

    private fun calculateProteinPoints(protein: Double): Int {
        return when {
            protein <= 1.6 -> 0
            protein <= 3.2 -> 1
            protein <= 4.8 -> 2
            protein <= 6.4 -> 3
            protein <= 8.0 -> 4
            else -> 5
        }
    }

    private fun calculateFiberPoints(fiber: Double): Int {
        return when {
            fiber <= 0.9 -> 0
            fiber <= 1.9 -> 1
            fiber <= 2.8 -> 2
            fiber <= 3.7 -> 3
            fiber <= 4.7 -> 4
            else -> 5
        }
    }

    private fun calculateFruitsVegetablesPoints(percentage: Double): Int {
        return when {
            percentage <= 40 -> 0
            percentage <= 60 -> 1
            percentage <= 80 -> 2
            else -> 5
        }
    }
}
