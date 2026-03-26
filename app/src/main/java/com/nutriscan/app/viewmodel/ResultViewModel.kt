package com.nutriscan.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.R
import com.nutriscan.app.data.local.SettingsDataStore
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.data.repository.ProductRepositoryImpl
import com.nutriscan.app.data.repository.ScanHistoryRepository
import com.nutriscan.app.domain.analysis.AnalysisEngine
import com.nutriscan.app.domain.analysis.AnalysisSettings
import com.nutriscan.app.domain.analyzer.Allergen
import com.nutriscan.app.domain.analyzer.NutriScoreCalculator
import com.nutriscan.app.model.Ingredient
import com.nutriscan.app.model.IngredientType
import com.nutriscan.app.model.NutriScore
import com.nutriscan.app.model.Product
import com.nutriscan.app.model.ProductAnalysis
import com.nutriscan.app.model.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.nutriscan.app.localization.localizedAnalysisItem
import com.nutriscan.app.localization.localizedNutriScoreNote
import com.nutriscan.app.localization.localizedRecommendation
import com.nutriscan.app.localization.localizedStatusLabel

data class ResultUiState(
    val scanResult: ScanResult? = null,
    val productEntity: ProductEntity? = null,
    val productAnalysis: ProductAnalysis? = null,
    val isCachedResult: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isFavorite: Boolean = false,
    val error: String? = null
)

class ResultViewModel(application: Application) : AndroidViewModel(application) {

    private val productRepository = ProductRepositoryImpl(application)
    private val scanHistoryRepository = ScanHistoryRepository(application)
    private val settingsDataStore = SettingsDataStore(application)

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    fun loadResult(productId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val productEntity = productRepository.getProductById(productId)
                if (productEntity != null) {
                    val calculatedNutriScore = if (productEntity.nutriscore == NutriScore.C) {
                        NutriScoreCalculator.calculateFromIngredients(productEntity.ingredients)
                    } else {
                        productEntity.nutriscore
                    }

                    val isSaved = scanHistoryRepository.hasHistoryForProduct(productId)
                    val scanResult = createScanResult(productEntity, calculatedNutriScore, emptyList())
                    val analysisSettings = AnalysisSettings(
                        selectedAllergens = mapSelectedAllergens(settingsDataStore.selectedAllergens.first()),
                        halalModeEnabled = settingsDataStore.halalMode.first()
                    )
                    val productAnalysis = AnalysisEngine.analyze(scanResult.product, analysisSettings)

                    _uiState.value = _uiState.value.copy(
                        scanResult = scanResult,
                        productEntity = productEntity,
                        productAnalysis = productAnalysis,
                        isCachedResult = productEntity.source == "cache",
                        isSaved = isSaved,
                        isFavorite = productEntity.isFavorite,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = getApplication<Application>().getString(R.string.result_error_product_not_found),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: getApplication<Application>().getString(R.string.error_generic),
                    isLoading = false
                )
            }
        }
    }

    fun saveToHistory() {
        viewModelScope.launch {
            _uiState.value.productEntity?.let { product ->
                if (_uiState.value.isSaved) {
                    return@let
                }
                scanHistoryRepository.addToHistory(product.id, product.imageUrl)
                _uiState.value = _uiState.value.copy(isSaved = true)
            }
        }
    }

    fun shareResult(): Intent? {
        val scanResult = _uiState.value.scanResult ?: return null
        val analysis = _uiState.value.productAnalysis
        val app = getApplication<Application>()
        val recommendation = analysis?.let { app.localizedRecommendation(it.safetyStatus) }
        
        val shareText = buildString {
            appendLine("${scanResult.product.name} - ${scanResult.product.brand}")
            appendLine(app.getString(R.string.result_meta_nutriscore, scanResult.product.nutriscore.grade))
            appendLine()
            
            analysis?.let { productAnalysis ->
                appendLine(app.getString(R.string.share_safety_status, app.localizedStatusLabel(productAnalysis.safetyStatus)))
                if (productAnalysis.detectedAllergens.isNotEmpty()) {
                    appendLine(app.getString(R.string.share_detected_allergens, productAnalysis.detectedAllergens.joinToString(", ") { item ->
                        app.localizedAnalysisItem(item)
                    }))
                }
                if (productAnalysis.harmfulIngredients.isNotEmpty()) {
                    appendLine(app.getString(R.string.share_harmful_ingredients, productAnalysis.harmfulIngredients.joinToString(", ") { item ->
                        app.localizedAnalysisItem(item)
                    }))
                }
                appendLine()
                recommendation?.let { (title, message) ->
                    appendLine(app.getString(R.string.share_recommendation, title))
                    appendLine(message)
                }
                appendLine()
                appendLine(app.getString(R.string.share_explanation_heading))
                appendLine(app.localizedNutriScoreNote(scanResult.product.nutriscore))
            }
            
            appendLine(app.getString(R.string.share_ingredients_heading))
            scanResult.product.ingredients.forEach { ingredient ->
                appendLine("- ${ingredient.name}")
            }
            
            appendLine()
            appendLine(app.getString(R.string.share_footer))
        }
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, app.getString(R.string.share_subject, scanResult.product.name))
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val currentProduct = _uiState.value.productEntity ?: return@launch
            val newFavoriteValue = !_uiState.value.isFavorite
            productRepository.toggleFavorite(currentProduct.id, newFavoriteValue)
            _uiState.value = _uiState.value.copy(
                isFavorite = newFavoriteValue,
                productEntity = currentProduct.copy(isFavorite = newFavoriteValue)
            )
        }
    }

    private fun mapSelectedAllergens(selectedAllergens: Set<Allergen>): Set<String> {
        return selectedAllergens.mapNotNull { allergen ->
            when (allergen) {
                Allergen.MILK -> "dairy"
                Allergen.GLUTEN -> "gluten"
                Allergen.PEANUTS, Allergen.TREE_NUTS -> "nuts"
                else -> null
            }
        }.toSet()
    }

    private fun createScanResult(
        productEntity: ProductEntity,
        nutriScore: NutriScore,
        alternatives: List<Product>
    ): ScanResult {
        val product = Product(
            id = productEntity.id,
            name = productEntity.name,
            brand = productEntity.brand,
            nutriscore = nutriScore,
            ingredients = productEntity.ingredients.map { entity ->
                Ingredient(
                    name = entity.name,
                    type = entity.type,
                    description = entity.description
                )
            },
            imageUrl = productEntity.imageUrl
        )

        val explanation = when (nutriScore) {
            NutriScore.A -> getApplication<Application>().getString(R.string.nutriscore_a)
            NutriScore.B -> getApplication<Application>().getString(R.string.nutriscore_b)
            NutriScore.C -> getApplication<Application>().getString(R.string.nutriscore_c)
            NutriScore.D -> getApplication<Application>().getString(R.string.nutriscore_d)
            NutriScore.E -> getApplication<Application>().getString(R.string.nutriscore_e)
        }

        return ScanResult(product, explanation, alternatives)
    }
}
