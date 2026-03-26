package com.nutriscan.app.data.mapper

import com.nutriscan.app.data.local.entity.IngredientEntity
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.data.remote.dto.OpenFoodFactsProduct
import com.nutriscan.app.model.IngredientType
import com.nutriscan.app.model.NutriScore

object OpenFoodFactsProductMapper {

    fun toEntity(
        apiProduct: OpenFoodFactsProduct,
        source: String = "api"
    ): ProductEntity {
        val ingredients = apiProduct.ingredients?.map { ingredient ->
            IngredientEntity(
                name = ingredient.text ?: "Unknown",
                type = IngredientType.NEUTRAL,
                description = null,
                eCode = extractECode(ingredient.id)
            )
        } ?: parseIngredientsText(apiProduct.ingredientsText)

        return ProductEntity(
            id = apiProduct.code ?: "off_${System.currentTimeMillis()}",
            barcode = apiProduct.code,
            name = apiProduct.productName ?: "Unknown Product",
            brand = apiProduct.brands ?: "Unknown Brand",
            nutriscore = parseNutriScore(apiProduct.nutriscoreGrade),
            ingredients = ingredients,
            imageUrl = apiProduct.imageFrontUrl ?: apiProduct.imageUrl,
            source = source
        )
    }

    private fun parseIngredientsText(text: String?): List<IngredientEntity> {
        if (text.isNullOrBlank()) return emptyList()

        return text.split(",", ";", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { ingredientName ->
                IngredientEntity(
                    name = ingredientName,
                    type = IngredientType.NEUTRAL,
                    description = null,
                    eCode = extractECode(ingredientName)
                )
            }
    }

    private fun extractECode(text: String?): String? {
        text ?: return null
        val regex = Regex("E\\d{3,4}[a-z]?", RegexOption.IGNORE_CASE)
        return regex.find(text)?.value?.uppercase()
    }

    private fun parseNutriScore(grade: String?): NutriScore {
        return when (grade?.uppercase()) {
            "A" -> NutriScore.A
            "B" -> NutriScore.B
            "C" -> NutriScore.C
            "D" -> NutriScore.D
            "E" -> NutriScore.E
            else -> NutriScore.C
        }
    }
}
