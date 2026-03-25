package com.nutriscan.app.model

data class Product(
    val id: String,
    val name: String,
    val brand: String,
    val nutriscore: NutriScore,
    val ingredients: List<Ingredient>,
    val imageUrl: String? = null,
    val scannedAt: Long = System.currentTimeMillis()
)

enum class NutriScore(val grade: String) {
    A("A"),
    B("B"),
    C("C"),
    D("D"),
    E("E")
}

data class Ingredient(
    val name: String,
    val type: IngredientType,
    val description: String? = null
)

enum class IngredientType {
    GOOD,
    BAD,
    NEUTRAL
}

data class ScanResult(
    val product: Product,
    val explanation: String,
    val alternatives: List<Product>
)

enum class SafetyStatus {
    SAFE,
    CAUTION,
    DANGER
}

data class Warning(
    val title: String,
    val message: String
)

data class Recommendation(
    val title: String,
    val message: String
)

data class ProductAnalysis(
    val safetyStatus: SafetyStatus,
    val detectedAllergens: List<String>,
    val harmfulIngredients: List<String>,
    val warnings: List<Warning>,
    val explanation: String,
    val recommendation: Recommendation
)
