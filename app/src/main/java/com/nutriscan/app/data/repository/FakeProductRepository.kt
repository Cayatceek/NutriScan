package com.nutriscan.app.data.repository

import com.nutriscan.app.data.local.entity.IngredientEntity
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.model.IngredientType
import com.nutriscan.app.model.NutriScore
import kotlinx.coroutines.delay

class FakeProductRepository {

    private val products = listOf(
        ProductEntity(
            id = "478002200001",
            barcode = "478002200001",
            name = "Bio Yogurt",
            brand = "Nutri Farm",
            nutriscore = NutriScore.B,
            ingredients = listOf(
                IngredientEntity("Milk", IngredientType.BAD, "Contains dairy"),
                IngredientEntity("Live cultures", IngredientType.GOOD, "Supports digestion")
            ),
            source = "fake"
        ),
        ProductEntity(
            id = "478002200002",
            barcode = "478002200002",
            name = "Oat Bar",
            brand = "Green Bite",
            nutriscore = NutriScore.A,
            ingredients = listOf(
                IngredientEntity("Oats", IngredientType.GOOD, "Whole grain ingredient"),
                IngredientEntity("Dates", IngredientType.GOOD, "Natural sweetness")
            ),
            source = "fake"
        ),
        ProductEntity(
            id = "478002200003",
            barcode = "478002200003",
            name = "Instant Noodles",
            brand = "Quick Bowl",
            nutriscore = NutriScore.D,
            ingredients = listOf(
                IngredientEntity("Wheat flour", IngredientType.BAD, "Contains gluten"),
                IngredientEntity("E621", IngredientType.BAD, "Flavor enhancer")
            ),
            source = "fake"
        ),
        ProductEntity(
            id = "478002200004",
            barcode = "478002200004",
            name = "Plain Chickpea Chips",
            brand = "Simply Crunch",
            nutriscore = NutriScore.B,
            ingredients = listOf(
                IngredientEntity("Chickpea flour", IngredientType.GOOD, "Plant-based protein source"),
                IngredientEntity("Sea salt", IngredientType.NEUTRAL, "Added for taste"),
                IngredientEntity("Sunflower oil", IngredientType.NEUTRAL, "Used for crisp texture")
            ),
            source = "fake"
        ),
        ProductEntity(
            id = "478002200005",
            barcode = "478002200005",
            name = "Berry Protein Yogurt",
            brand = "Active Dairy",
            nutriscore = NutriScore.C,
            ingredients = listOf(
                IngredientEntity("Milk", IngredientType.BAD, "Contains dairy"),
                IngredientEntity("Strawberry puree", IngredientType.GOOD, "Fruit ingredient"),
                IngredientEntity("Sugar", IngredientType.BAD, "Raises overall sugar load")
            ),
            source = "fake"
        ),
        ProductEntity(
            id = "478002200006",
            barcode = "478002200006",
            name = "Fruit Jelly Bites",
            brand = "Sweet Cloud",
            nutriscore = NutriScore.D,
            ingredients = listOf(
                IngredientEntity("Sugar", IngredientType.BAD, "Raises overall sugar load"),
                IngredientEntity("Gelatin", IngredientType.BAD, "Can conflict with halal dietary preferences"),
                IngredientEntity("E102", IngredientType.BAD, "Color additive some users prefer to avoid")
            ),
            source = "fake"
        )
    )

    suspend fun searchByBarcode(barcode: String): ProductEntity? {
        delay(750)
        return products.firstOrNull { it.barcode == barcode }
    }

    suspend fun searchByQuery(query: String): List<ProductEntity> {
        delay(450)
        if (query.isBlank()) return emptyList()

        val normalizedQuery = query.trim().lowercase()
        return products.filter { product ->
            product.name.lowercase().contains(normalizedQuery) ||
                product.brand.lowercase().contains(normalizedQuery) ||
                product.barcode?.contains(normalizedQuery) == true
        }
    }

    suspend fun getSuggestedAlternatives(
        excludeProductId: String? = null,
        limit: Int = 3
    ): List<ProductEntity> {
        delay(250)
        return products
            .filterNot { it.id == excludeProductId }
            .sortedBy { nutriScoreRank(it.nutriscore) }
            .take(limit)
    }

    private fun nutriScoreRank(score: NutriScore): Int {
        return when (score) {
            NutriScore.A -> 0
            NutriScore.B -> 1
            NutriScore.C -> 2
            NutriScore.D -> 3
            NutriScore.E -> 4
        }
    }
}
