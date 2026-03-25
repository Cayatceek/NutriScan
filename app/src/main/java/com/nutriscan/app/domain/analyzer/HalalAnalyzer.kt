package com.nutriscan.app.domain.analyzer

import com.nutriscan.app.data.local.entity.IngredientEntity

enum class HalalStatus {
    HALAL,      // Permitted
    HARAM,      // Forbidden
    MUSHBOOH,   // Doubtful/Questionable
    UNKNOWN     // Not in database
}

data class HalalAnalysisResult(
    val status: HalalStatus,
    val haramIngredients: List<String>,
    val mushboohIngredients: List<String>,
    val explanation: String
)

object HalalAnalyzer {

    // E-codes database (simplified - common ones)
    private val E_CODE_HALAL_STATUS = mapOf(
        // Halal E-codes
        "E100" to HalalStatus.HALAL,      // Curcumin
        "E101" to HalalStatus.HALAL,      // Riboflavin
        "E150" to HalalStatus.HALAL,      // Caramel
        "E160" to HalalStatus.HALAL,      // Carotenoids
        "E170" to HalalStatus.HALAL,      // Calcium carbonate
        "E260" to HalalStatus.HALAL,      // Acetic acid
        "E270" to HalalStatus.HALAL,      // Lactic acid
        "E300" to HalalStatus.HALAL,      // Vitamin C
        "E330" to HalalStatus.HALAL,      // Citric acid
        "E400" to HalalStatus.HALAL,      // Alginic acid
        "E440" to HalalStatus.HALAL,      // Pectins
        
        // Haram E-codes
        "E120" to HalalStatus.HARAM,      // Cochineal (insect)
        "E441" to HalalStatus.HARAM,      // Gelatin (pork source common)
        "E542" to HalalStatus.HARAM,      // Bone phosphate
        "E901" to HalalStatus.HARAM,      // Beeswax (may be haram for some)
        "E904" to HalalStatus.HARAM,      // Shellac (insect)
        "E920" to HalalStatus.HARAM,      // L-cysteine (often from human hair)
        
        // Mushbooh (depends on source)
        "E471" to HalalStatus.MUSHBOOH,   // Mono/diglycerides
        "E472" to HalalStatus.MUSHBOOH,   // Fatty acid esters
        "E473" to HalalStatus.MUSHBOOH,   // Sucrose esters
        "E474" to HalalStatus.MUSHBOOH,   // Sucroglycerides
        "E475" to HalalStatus.MUSHBOOH,   // Polyglycerol esters
        "E476" to HalalStatus.MUSHBOOH,   // Polyglycerol polyricinoleate
        "E477" to HalalStatus.MUSHBOOH,   // Propylene glycol esters
        "E481" to HalalStatus.MUSHBOOH,   // Sodium stearoyl lactylate
        "E482" to HalalStatus.MUSHBOOH,   // Calcium stearoyl lactylate
        "E483" to HalalStatus.MUSHBOOH,   // Stearyl tartrate
        "E491" to HalalStatus.MUSHBOOH,   // Sorbitan monostearate
        "E492" to HalalStatus.MUSHBOOH,   // Sorbitan tristearate
        "E493" to HalalStatus.MUSHBOOH,   // Sorbitan monolaurate
        "E494" to HalalStatus.MUSHBOOH,   // Sorbitan monooleate
        "E495" to HalalStatus.MUSHBOOH,   // Sorbitan monopalmitate
        "E570" to HalalStatus.MUSHBOOH,   // Stearic acid
        "E572" to HalalStatus.MUSHBOOH,   // Magnesium stearate
        "E573" to HalalStatus.MUSHBOOH,   // Aluminum stearate
        "E574" to HalalStatus.MUSHBOOH,   // Gluconic acid
        "E575" to HalalStatus.MUSHBOOH,   // Glucono delta-lactone
        "E576" to HalalStatus.MUSHBOOH,   // Sodium gluconate
        "E577" to HalalStatus.MUSHBOOH,   // Potassium gluconate
        "E578" to HalalStatus.MUSHBOOH,   // Calcium gluconate
        "E579" to HalalStatus.MUSHBOOH,   // Ferrous gluconate
        "E585" to HalalStatus.MUSHBOOH,   // Ferrous lactate
        "E621" to HalalStatus.MUSHBOOH,   // MSG (generally halal but some concerns)
        "E631" to HalalStatus.MUSHBOOH,   // Disodium inosinate
        "E635" to HalalStatus.MUSHBOOH    // Disodium 5'-ribonucleotides
    )

    // Haram ingredient keywords (English, Russian, Uzbek)
    private val HARAM_KEYWORDS = setOf(
        // Pork and derivatives
        "pork", "bacon", "ham", "lard", "gelatin", "pepperoni", "salami",
        "свинина", "бекон", "ветчина", "сало", "желатин",
        "cho'chqa", "sho'rva", "saryog' cho'chqa", 
        
        // Alcohol
        "alcohol", "ethanol", "wine", "beer", "rum", "vodka", "brandy",
        "алкоголь", "спирт", "вино", "пиво", "водка",
        "spirt", "vino", "pivo", "araq",
        
        // Other animal products (often non-halal)
        "rennet", "pepsin", "lipase", "animal fat",
        "реннет", "пепсин", "липаза",
        "maya", "hayvon yog'i"
    )

    // Mushbooh ingredients (need verification)
    private val MUSHBOOH_KEYWORDS = setOf(
        "enzyme", "emulsifier", "stabilizer", "preservative",
        "энзим", "эмульгатор", "стабилизатор", "консервант",
        "ferment", "emulsifikator", "stabilizator"
    )

    fun analyze(ingredients: List<IngredientEntity>): HalalAnalysisResult {
        val haramFound = mutableListOf<String>()
        val mushboohFound = mutableListOf<String>()

        for (ingredient in ingredients) {
            val name = ingredient.name.lowercase()
            val eCode = ingredient.eCode?.uppercase()

            // Check E-code
            eCode?.let { code ->
                when (E_CODE_HALAL_STATUS[code]) {
                    HalalStatus.HARAM -> haramFound.add("$code (${ingredient.name})")
                    HalalStatus.MUSHBOOH -> mushboohFound.add("$code (${ingredient.name})")
                    else -> {}
                }
            }

            // Check ingredient name
            HARAM_KEYWORDS.forEach { keyword ->
                if (name.contains(keyword.lowercase())) {
                    haramFound.add(ingredient.name)
                }
            }

            MUSHBOOH_KEYWORDS.forEach { keyword ->
                if (name.contains(keyword.lowercase())) {
                    mushboohFound.add(ingredient.name)
                }
            }
        }

        // Determine overall status
        val status = when {
            haramFound.isNotEmpty() -> HalalStatus.HARAM
            mushboohFound.isNotEmpty() -> HalalStatus.MUSHBOOH
            ingredients.isNotEmpty() -> HalalStatus.HALAL
            else -> HalalStatus.UNKNOWN
        }

        // Generate explanation
        val explanation = when (status) {
            HalalStatus.HALAL -> "This product appears to be halal. No haram ingredients detected."
            HalalStatus.HARAM -> "This product contains haram ingredients: ${haramFound.take(3).joinToString(", ")}"
            HalalStatus.MUSHBOOH -> "Some ingredients require verification: ${mushboohFound.take(3).joinToString(", ")}"
            HalalStatus.UNKNOWN -> "Unable to determine halal status. Ingredient information insufficient."
        }

        return HalalAnalysisResult(
            status = status,
            haramIngredients = haramFound.distinct(),
            mushboohIngredients = mushboohFound.distinct(),
            explanation = explanation
        )
    }

    fun isHalal(ingredients: List<IngredientEntity>): Boolean {
        return analyze(ingredients).status == HalalStatus.HALAL
    }
}
