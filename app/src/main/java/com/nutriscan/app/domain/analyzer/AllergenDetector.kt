package com.nutriscan.app.domain.analyzer

import com.nutriscan.app.data.local.entity.IngredientEntity

enum class Allergen {
    GLUTEN, MILK, EGGS, PEANUTS, TREE_NUTS, SOY, FISH, SHELLFISH, SESAME, SULFITES
}

data class AllergenResult(
    val detectedAllergens: List<Allergen>,
    val allergenIngredients: Map<Allergen, List<String>>
)

object AllergenDetector {

    private val ALLERGEN_KEYWORDS = mapOf(
        Allergen.GLUTEN to setOf(
            "wheat", "barley", "rye", "oats", "gluten", "flour", "bread", "pasta",
            "пшеница", "ячмень", "рожь", "овес", "глютен", "мука", "хлеб",
            "bug'doy", "arpa", "javdar", "yulaf", "un", "non"
        ),
        Allergen.MILK to setOf(
            "milk", "dairy", "lactose", "whey", "casein", "cream", "butter", "cheese", "yogurt",
            "молоко", "молочный", "лактоза", "сыворотка", "казеин", "сливки", "масло", "сыр", "йогурт",
            "sut", "sutli", "laktaza", "qaymoq", "saryog'", "pishloq", "qatiq"
        ),
        Allergen.EGGS to setOf(
            "egg", "eggs", "albumin", "ovalbumin", "lysozyme",
            "яйцо", "яйца", "альбумин", "овальбумин", "лизоцим",
            "tuxum", "tuxumlar", "albumin"
        ),
        Allergen.PEANUTS to setOf(
            "peanut", "peanuts", "arachis", "groundnut",
            "арахис", "арахисовый", "земляной орех",
            "yeryong'oq", "yer yong'oq"
        ),
        Allergen.TREE_NUTS to setOf(
            "almond", "walnut", "cashew", "pecan", "pistachio", "hazelnut", "macadamia", "brazil nut",
            "миндаль", "грецкий орех", "кешью", "пекан", "фисташка", "фундук", "макадамия",
            "bodom", "yong'oq", "keshyu", "pistazor", "funduk"
        ),
        Allergen.SOY to setOf(
            "soy", "soya", "soybean", "tofu", "edamame", "miso",
            "соя", "соевый", "тофу", "эдамаме", "мисо",
            "soya", "soya loviya", "tofu"
        ),
        Allergen.FISH to setOf(
            "fish", "salmon", "tuna", "cod", "anchovy", "fish sauce",
            "рыба", "лосось", "тунец", "треска", "анчоус", "рыбный соус",
            "baliq", "somon", "tuna", "treska", "anchous"
        ),
        Allergen.SHELLFISH to setOf(
            "shrimp", "crab", "lobster", "prawn", "crayfish", "shellfish",
            "креветка", "краб", "омар", "рак", "моллюск",
            "krevetka", "qisqichbaqa", "omar", "rak"
        ),
        Allergen.SESAME to setOf(
            "sesame", "tahini", "sesame oil", "sesame seed",
            "кунжут", "тахини", "кунжутное масло", "кунжутный",
            "kunjut", "tahini", "kunjut yog'i"
        ),
        Allergen.SULFITES to setOf(
            "sulfite", "sulfur dioxide", "sodium bisulfite", "potassium metabisulfite",
            "сульфит", "диоксид серы", "бисульфит натрия",
            "sulfit", "kukun dioksid", "natriy bisulfit"
        )
    )

    fun detect(ingredients: List<IngredientEntity>): AllergenResult {
        val detected = mutableSetOf<Allergen>()
        val ingredientMap = mutableMapOf<Allergen, MutableList<String>>()

        for (ingredient in ingredients) {
            val name = ingredient.name.lowercase()

            ALLERGEN_KEYWORDS.forEach { (allergen, keywords) ->
                keywords.forEach { keyword ->
                    if (name.contains(keyword.lowercase())) {
                        detected.add(allergen)
                        ingredientMap.getOrPut(allergen) { mutableListOf() }
                            .add(ingredient.name)
                    }
                }
            }
        }

        return AllergenResult(
            detectedAllergens = detected.toList(),
            allergenIngredients = ingredientMap.mapValues { it.value.distinct() }
        )
    }

    fun hasAllergen(ingredients: List<IngredientEntity>, allergen: Allergen): Boolean {
        return detect(ingredients).detectedAllergens.contains(allergen)
    }

    fun getAllergenWarning(result: AllergenResult): String? {
        if (result.detectedAllergens.isEmpty()) return null
        
        val allergenNames = result.detectedAllergens.joinToString(", ") { 
            it.name.replace("_", " ").lowercase().capitalize() 
        }
        return "Contains: $allergenNames"
    }

    private fun String.capitalize(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
