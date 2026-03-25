package com.nutriscan.app.domain.parser

import com.nutriscan.app.data.local.entity.IngredientEntity
import com.nutriscan.app.domain.analyzer.Allergen
import com.nutriscan.app.domain.analyzer.HalalStatus
import com.nutriscan.app.model.IngredientType

/**
 * Parsed ingredient with analysis results
 */
data class ParsedIngredient(
    val entity: IngredientEntity,
    val detectedAllergens: List<Allergen>,
    val halalStatus: HalalStatus,
    val isVegan: Boolean,
    val isVegetarian: Boolean
)

object IngredientParser {

    private val E_CODE_PATTERN = Regex("E\\d{3,4}[a-z]?", RegexOption.IGNORE_CASE)
    private val PERCENTAGE_PATTERN = Regex("\\d+\\s*%")
    private val PARENTHESIS_PATTERN = Regex("\\(.*?\\)")

    // E-Codes database with descriptions and types
    private val E_CODE_DATABASE = mapOf(
        // Colors (E100-E199)
        "E100" to Pair("Curcumin", IngredientType.NEUTRAL),
        "E101" to Pair("Riboflavin", IngredientType.NEUTRAL),
        "E102" to Pair("Tartrazine", IngredientType.BAD),
        "E104" to Pair("Quinoline Yellow", IngredientType.BAD),
        "E110" to Pair("Sunset Yellow", IngredientType.BAD),
        "E120" to Pair("Cochineal", IngredientType.BAD), // Haram
        "E122" to Pair("Carmoisine", IngredientType.BAD),
        "E123" to Pair("Amaranth", IngredientType.BAD),
        "E124" to Pair("Ponceau 4R", IngredientType.BAD),
        "E129" to Pair("Allura Red", IngredientType.BAD),
        "E131" to Pair("Patent Blue", IngredientType.NEUTRAL),
        "E132" to Pair("Indigo Carmine", IngredientType.NEUTRAL),
        "E133" to Pair("Brilliant Blue", IngredientType.BAD),
        "E150" to Pair("Caramel", IngredientType.NEUTRAL),
        "E160" to Pair("Carotenoids", IngredientType.GOOD),
        "E170" to Pair("Calcium Carbonate", IngredientType.NEUTRAL),

        // Preservatives (E200-E299)
        "E200" to Pair("Sorbic Acid", IngredientType.NEUTRAL),
        "E210" to Pair("Benzoic Acid", IngredientType.BAD),
        "E211" to Pair("Sodium Benzoate", IngredientType.BAD),
        "E220" to Pair("Sulfur Dioxide", IngredientType.BAD),
        "E223" to Pair("Sodium Metabisulfite", IngredientType.BAD),
        "E250" to Pair("Sodium Nitrite", IngredientType.BAD),
        "E251" to Pair("Sodium Nitrate", IngredientType.BAD),

        // Antioxidants (E300-E399)
        "E300" to Pair("Vitamin C", IngredientType.GOOD),
        "E301" to Pair("Sodium Ascorbate", IngredientType.GOOD),
        "E306" to Pair("Vitamin E", IngredientType.GOOD),
        "E320" to Pair("BHA", IngredientType.BAD),
        "E321" to Pair("BHT", IngredientType.BAD),

        // Acidity regulators (E300-E399)
        "E330" to Pair("Citric Acid", IngredientType.NEUTRAL),
        "E331" to Pair("Sodium Citrate", IngredientType.NEUTRAL),
        "E334" to Pair("Tartaric Acid", IngredientType.NEUTRAL),
        "E339" to Pair("Sodium Phosphate", IngredientType.NEUTRAL),

        // Thickeners/Stabilizers (E400-E499)
        "E400" to Pair("Alginic Acid", IngredientType.NEUTRAL),
        "E401" to Pair("Sodium Alginate", IngredientType.NEUTRAL),
        "E406" to Pair("Agar", IngredientType.GOOD),
        "E407" to Pair("Carrageenan", IngredientType.NEUTRAL),
        "E410" to Pair("Locust Bean Gum", IngredientType.GOOD),
        "E412" to Pair("Guar Gum", IngredientType.GOOD),
        "E414" to Pair("Arabic Gum", IngredientType.GOOD),
        "E415" to Pair("Xanthan Gum", IngredientType.NEUTRAL),
        "E440" to Pair("Pectin", IngredientType.GOOD),

        // Emulsifiers (E400-E499)
        "E422" to Pair("Glycerol", IngredientType.NEUTRAL),
        "E432" to Pair("Polysorbate 20", IngredientType.NEUTRAL),
        "E433" to Pair("Polysorbate 80", IngredientType.NEUTRAL),
        "E471" to Pair("Mono/Diglycerides", IngredientType.NEUTRAL),
        "E472" to Pair("Fatty Acid Esters", IngredientType.NEUTRAL),
        "E475" to Pair("Polyglycerol Esters", IngredientType.NEUTRAL),

        // Flavor enhancers (E600-E699)
        "E621" to Pair("Monosodium Glutamate", IngredientType.BAD),
        "E627" to Pair("Disodium Guanylate", IngredientType.NEUTRAL),
        "E631" to Pair("Disodium Inosinate", IngredientType.NEUTRAL),
        "E635" to Pair("Ribonucleotides", IngredientType.NEUTRAL),

        // Glazing agents (E900-E999)
        "E901" to Pair("Beeswax", IngredientType.NEUTRAL),
        "E904" to Pair("Shellac", IngredientType.BAD),
        "E920" to Pair("L-Cysteine", IngredientType.BAD),
        "E951" to Pair("Aspartame", IngredientType.BAD),
        "E955" to Pair("Sucralose", IngredientType.BAD),
        "E965" to Pair("Maltitol", IngredientType.NEUTRAL),
        "E966" to Pair("Lactitol", IngredientType.NEUTRAL),
        "E968" to Pair("Erythritol", IngredientType.GOOD)
    )

    // Haram E-codes
    private val HARAM_E_CODES = setOf("E120", "E441", "E542", "E901", "E904", "E920")

    // Mushbooh E-codes (depends on source)
    private val MUSHBOOH_E_CODES = setOf(
        "E471", "E472", "E473", "E474", "E475", "E476", "E477",
        "E481", "E482", "E483", "E491", "E492", "E493", "E494", "E495",
        "E570", "E572", "E573", "E574", "E575", "E576", "E577", "E578",
        "E579", "E585", "E621", "E631", "E635"
    )

    // Allergen keywords mapping
    private val ALLERGEN_KEYWORDS = mapOf(
        Allergen.GLUTEN to setOf("wheat", "barley", "rye", "oats", "gluten", "flour", "bread", "pasta", "un"),
        Allergen.MILK to setOf("milk", "dairy", "lactose", "whey", "casein", "cream", "butter", "cheese", "yogurt", "sut", "sutli"),
        Allergen.EGGS to setOf("egg", "eggs", "albumin", "ovalbumin", "tuxum"),
        Allergen.PEANUTS to setOf("peanut", "peanuts", "arachis", "groundnut", "yeryongoq"),
        Allergen.TREE_NUTS to setOf("almond", "walnut", "cashew", "pecan", "pistachio", "hazelnut", "macadamia", "bodom", "yongoq"),
        Allergen.SOY to setOf("soy", "soya", "soybean", "tofu", "edamame", "miso"),
        Allergen.FISH to setOf("fish", "salmon", "tuna", "cod", "anchovy", "baliq"),
        Allergen.SHELLFISH to setOf("shrimp", "crab", "lobster", "prawn", "crayfish", "shellfish", "krevetka"),
        Allergen.SESAME to setOf("sesame", "tahini", "kunjut"),
        Allergen.SULFITES to setOf("sulfite", "sulfur dioxide", "sulfit")
    )

    // Haram ingredient keywords
    private val HARAM_KEYWORDS = setOf(
        "pork", "bacon", "ham", "lard", "gelatin", "pepperoni", "salami",
        "chochqa", "shorva", "alcohol", "ethanol", "wine", "beer", "rum", "vodka",
        "spirt", "vino", "pivo", "araq", "rennet", "pepsin", "lipase"
    )

    // Vegan/Vegetarian indicators
    private val ANIMAL_KEYWORDS = setOf(
        "meat", "chicken", "beef", "pork", "lamb", "fish", "seafood",
        "egg", "milk", "dairy", "honey", "whey", "casein", "lactose",
        "gosht", "tovuq", "mol", "baliq", "tuxum", "sut", "asal"
    )

    /**
     * Main parse function - extracts ingredients from OCR text
     */
    fun parse(text: String): List<IngredientEntity> {
        // Extract ingredients section
        val ingredientsSection = extractIngredientsSection(text)

        // Clean up the text
        val cleanedText = ingredientsSection
            .replace(PERCENTAGE_PATTERN, "") // Remove percentages
            .replace(PARENTHESIS_PATTERN, " ") // Replace parentheses with space
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()

        // Split by common delimiters
        val rawIngredients = cleanedText.split(",", ";", "\n", "•", "·", "/")
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length > 2 }

        return rawIngredients.mapNotNull { rawIngredient ->
            parseSingleIngredient(rawIngredient)
        }.distinctBy { it.name.lowercase() }
    }

    private fun parseSingleIngredient(raw: String): IngredientEntity? {
        val eCode = extractECode(raw)
        val name = cleanIngredientName(raw)

        if (name.isBlank()) return null

        // Determine ingredient type based on E-code or name
        val type = when {
            eCode != null -> E_CODE_DATABASE[eCode]?.second ?: IngredientType.NEUTRAL
            isSugar(name) -> IngredientType.BAD
            isSalt(name) -> IngredientType.NEUTRAL
            isOilOrFat(name) -> IngredientType.BAD
            isArtificial(name) -> IngredientType.BAD
            isNaturalGood(name) -> IngredientType.GOOD
            else -> IngredientType.NEUTRAL
        }

        val description = eCode?.let { E_CODE_DATABASE[it]?.first }

        return IngredientEntity(
            name = name,
            type = type,
            description = description,
            eCode = eCode
        )
    }

    /**
     * Parse with full analysis including allergens and halal status
     */
    fun parseWithAnalysis(text: String): List<ParsedIngredient> {
        val ingredients = parse(text)
        return ingredients.map { ingredient ->
            ParsedIngredient(
                entity = ingredient,
                detectedAllergens = detectAllergens(ingredient.name),
                halalStatus = analyzeHalalStatus(ingredient),
                isVegan = isVegan(ingredient.name),
                isVegetarian = isVegetarian(ingredient.name)
            )
        }
    }

    private fun extractECode(text: String): String? {
        return E_CODE_PATTERN.find(text)?.value?.uppercase()
    }

    private fun cleanIngredientName(text: String): String {
        return text
            .replace(E_CODE_PATTERN, "") // Remove E-codes from name
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
            .capitalizeWords()
    }

    private fun detectAllergens(ingredientName: String): List<Allergen> {
        val lowerName = ingredientName.lowercase()
        return ALLERGEN_KEYWORDS.filter { (_, keywords) ->
            keywords.any { lowerName.contains(it) }
        }.keys.toList()
    }

    private fun analyzeHalalStatus(ingredient: IngredientEntity): HalalStatus {
        val eCode = ingredient.eCode
        val name = ingredient.name.lowercase()

        // Check E-code
        if (eCode != null) {
            when {
                HARAM_E_CODES.contains(eCode) -> return HalalStatus.HARAM
                MUSHBOOH_E_CODES.contains(eCode) -> return HalalStatus.MUSHBOOH
            }
        }

        // Check name keywords
        if (HARAM_KEYWORDS.any { name.contains(it) }) {
            return HalalStatus.HARAM
        }

        return HalalStatus.HALAL
    }

    private fun isVegan(ingredientName: String): Boolean {
        val lowerName = ingredientName.lowercase()
        return !ANIMAL_KEYWORDS.any { lowerName.contains(it) }
    }

    private fun isVegetarian(ingredientName: String): Boolean {
        val lowerName = ingredientName.lowercase()
        val nonVegMeat = setOf("meat", "chicken", "beef", "pork", "lamb", "fish", "gosht", "tovuq", "baliq")
        return !nonVegMeat.any { lowerName.contains(it) }
    }

    private fun isSugar(name: String): Boolean {
        val sugarKeywords = setOf("sugar", "glucose", "fructose", "syrup", "sucrose", "qand")
        return sugarKeywords.any { name.lowercase().contains(it) }
    }

    private fun isSalt(name: String): Boolean {
        return name.lowercase().contains("salt") || name.lowercase().contains("tuz")
    }

    private fun isOilOrFat(name: String): Boolean {
        val fatKeywords = setOf("oil", "fat", "butter", "margarine", "yog", "saryog", "maye")
        return fatKeywords.any { name.lowercase().contains(it) }
    }

    private fun isArtificial(name: String): Boolean {
        val artificialKeywords = setOf("artificial", "synthetic", "flavor", "color", "preservative")
        return artificialKeywords.any { name.lowercase().contains(it) }
    }

    private fun isNaturalGood(name: String): Boolean {
        val goodKeywords = setOf("vitamin", "mineral", "fiber", "protein", "fruit", "vegetable")
        return goodKeywords.any { name.lowercase().contains(it) }
    }

    fun extractIngredientsSection(text: String): String {
        val lines = text.lines()
        val ingredientsLines = mutableListOf<String>()
        var inIngredientsSection = false

        for (line in lines) {
            val trimmed = line.trim()

            // Detect ingredients section (multilingual)
            if (trimmed.contains("ingredients", ignoreCase = true) ||
                trimmed.contains("composition", ignoreCase = true) ||
                trimmed.contains("tarkib", ignoreCase = true) ||
                trimmed.contains("состав", ignoreCase = true) ||
                trimmed.contains("ingredientes", ignoreCase = true)) {
                inIngredientsSection = true
                // Remove the header itself if it's on same line
                val withoutHeader = trimmed.replace(Regex("ingredients?\\s*:?", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("состав\\s*:?", RegexOption.IGNORE_CASE), "")
                    .trim()
                if (withoutHeader.isNotEmpty()) {
                    ingredientsLines.add(withoutHeader)
                }
                continue
            }

            // Detect end of ingredients section
            if (inIngredientsSection && (
                trimmed.contains("nutrition", ignoreCase = true) ||
                trimmed.contains("allergens", ignoreCase = true) ||
                trimmed.contains("storage", ignoreCase = true) ||
                trimmed.contains("manufacturer", ignoreCase = true) ||
                trimmed.contains("address", ignoreCase = true) ||
                trimmed.contains("best before", ignoreCase = true)
            )) {
                break
            }

            if (inIngredientsSection && trimmed.isNotEmpty()) {
                ingredientsLines.add(trimmed)
            }
        }

        // If no ingredients section found, try to find comma-separated list
        return if (ingredientsLines.isEmpty()) {
            // Look for patterns like "Water, Sugar, Salt..."
            val commaLine = lines.find { it.contains(",") && it.length > 20 }
            commaLine ?: text
        } else {
            ingredientsLines.joinToString(" ")
        }
    }

    private fun String.capitalizeWords(): String {
        return this.split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
}
