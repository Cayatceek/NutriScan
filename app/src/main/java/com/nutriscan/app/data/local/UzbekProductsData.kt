package com.nutriscan.app.data.local

import com.nutriscan.app.data.local.entity.IngredientEntity
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.model.IngredientType
import com.nutriscan.app.model.NutriScore

object UzbekProductsData {

    val products: List<ProductEntity> = listOf(
        // Dairy
        ProductEntity(
            id = "uz_001",
            barcode = "478002200001",
            name = "Sut (Milk)",
            brand = "Selmash",
            nutriscore = NutriScore.B,
            ingredients = listOf(
                IngredientEntity("Sut", IngredientType.GOOD, "Fresh milk"),
                IngredientEntity("Vitamin D", IngredientType.GOOD, "Added vitamin")
            ),
            source = "uzbek"
        ),
        ProductEntity(
            id = "uz_002",
            barcode = "478002200002",
            name = "Qatiq (Yogurt)",
            brand = "Chortoq",
            nutriscore = NutriScore.A,
            ingredients = listOf(
                IngredientEntity("Sut", IngredientType.GOOD, "Fermented milk"),
                IngredientEntity("Live Cultures", IngredientType.GOOD, "Probiotics")
            ),
            source = "uzbek"
        ),
        ProductEntity(
            id = "uz_003",
            barcode = "478002200003",
            name = "Saryog' (Butter)",
            brand = "Yangi Hayot",
            nutriscore = NutriScore.C,
            ingredients = listOf(
                IngredientEntity("Sut yog'i", IngredientType.NEUTRAL, "Milk fat"),
                IngredientEntity("Suv", IngredientType.NEUTRAL, "Water")
            ),
            source = "uzbek"
        ),

        // Bread
        ProductEntity(
            id = "uz_004",
            barcode = "478002200004",
            name = "Patir Non",
            brand = "O'zbek Non",
            nutriscore = NutriScore.B,
            ingredients = listOf(
                IngredientEntity("Un", IngredientType.NEUTRAL, "Wheat flour"),
                IngredientEntity("Suv", IngredientType.NEUTRAL, "Water"),
                IngredientEntity("Tuz", IngredientType.NEUTRAL, "Salt"),
                IngredientEntity("Xamirturush", IngredientType.NEUTRAL, "Yeast")
            ),
            source = "uzbek"
        ),
        ProductEntity(
            id = "uz_005",
            barcode = "478002200005",
            name = "Lepyoshka",
            brand = "Toshkent Non",
            nutriscore = NutriScore.C,
            ingredients = listOf(
                IngredientEntity("Un", IngredientType.NEUTRAL, "Wheat flour"),
                IngredientEntity("Suv", IngredientType.NEUTRAL, "Water"),
                IngredientEntity("Tuz", IngredientType.NEUTRAL, "Salt"),
                IngredientEntity("Kungaboqar yog'i", IngredientType.NEUTRAL, "Sunflower oil")
            ),
            source = "uzbek"
        ),

        // Meat products
        ProductEntity(
            id = "uz_006",
            barcode = "478002200006",
            name = "Qo'ziqorin Kolbasa",
            brand = "Oqbilol",
            nutriscore = NutriScore.D,
            ingredients = listOf(
                IngredientEntity("Go'sht", IngredientType.GOOD, "Meat"),
                IngredientEntity("Tuz", IngredientType.NEUTRAL, "Salt"),
                IngredientEntity("E250", IngredientType.BAD, "Sodium nitrite - preservative"),
                IngredientEntity("E621", IngredientType.BAD, "MSG - flavor enhancer")
            ),
            source = "uzbek"
        ),
        ProductEntity(
            id = "uz_007",
            barcode = "478002200007",
            name = "Tovuq Nuggets",
            brand = "Meros",
            nutriscore = NutriScore.D,
            ingredients = listOf(
                IngredientEntity("Tovuq go'shti", IngredientType.GOOD, "Chicken meat"),
                IngredientEntity("Un", IngredientType.NEUTRAL, "Breading"),
                IngredientEntity("Kungaboqar yog'i", IngredientType.BAD, "Fried in oil"),
                IngredientEntity("E450", IngredientType.BAD, "Stabilizer")
            ),
            source = "uzbek"
        ),

        // Drinks
        ProductEntity(
            id = "uz_008",
            barcode = "478002200008",
            name = "Sharbat (Compote)",
            brand = "Navbahor",
            nutriscore = NutriScore.C,
            ingredients = listOf(
                IngredientEntity("Suv", IngredientType.NEUTRAL, "Water"),
                IngredientEntity("Mevalar", IngredientType.GOOD, "Fruits"),
                IngredientEntity("Qand", IngredientType.BAD, "Sugar")
            ),
            source = "uzbek"
        ),
        ProductEntity(
            id = "uz_009",
            barcode = "478002200009",
            name = "Choy (Tea)",
            brand = "Zarafshon",
            nutriscore = NutriScore.A,
            ingredients = listOf(
                IngredientEntity("Choy barglari", IngredientType.GOOD, "Tea leaves")
            ),
            source = "uzbek"
        ),

        // Snacks
        ProductEntity(
            id = "uz_010",
            barcode = "478002200010",
            name = "Yong'oq Halva",
            brand = "Sogdiana",
            nutriscore = NutriScore.D,
            ingredients = listOf(
                IngredientEntity("Yong'oq", IngredientType.GOOD, "Nuts"),
                IngredientEntity("Qand", IngredientType.BAD, "Sugar"),
                IngredientEntity("Yog'", IngredientType.BAD, "Fat")
            ),
            source = "uzbek"
        ),
        ProductEntity(
            id = "uz_011",
            barcode = "478002200011",
            name = "Qaymoq",
            brand = "Yangi Hayot",
            nutriscore = NutriScore.C,
            ingredients = listOf(
                IngredientEntity("Sut yog'i", IngredientType.NEUTRAL, "Cream"),
                IngredientEntity("Sut", IngredientType.GOOD, "Milk")
            ),
            source = "uzbek"
        ),
        ProductEntity(
            id = "uz_012",
            barcode = "478002200012",
            name = "Kurt (Qurut)",
            brand = "O'zbek",
            nutriscore = NutriScore.B,
            ingredients = listOf(
                IngredientEntity("Qatiq", IngredientType.GOOD, "Dried yogurt"),
                IngredientEntity("Tuz", IngredientType.NEUTRAL, "Salt")
            ),
            source = "uzbek"
        )
    )

    fun search(query: String): List<ProductEntity> {
        val lowerQuery = query.lowercase()
        return products.filter { product ->
            product.name.lowercase().contains(lowerQuery) ||
            product.brand.lowercase().contains(lowerQuery) ||
            product.ingredients.any { it.name.lowercase().contains(lowerQuery) }
        }
    }

    fun getByBarcode(barcode: String): ProductEntity? {
        return products.find { it.barcode == barcode }
    }
}
