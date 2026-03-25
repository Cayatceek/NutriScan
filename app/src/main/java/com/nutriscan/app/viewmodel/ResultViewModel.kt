package com.nutriscan.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.data.local.SettingsDataStore
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.data.repository.FakeProductRepository
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

data class ResultUiState(
    val scanResult: ScanResult? = null,
    val productEntity: ProductEntity? = null,
    val productAnalysis: ProductAnalysis? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isFavorite: Boolean = false,
    val error: String? = null
)

class ResultViewModel(application: Application) : AndroidViewModel(application) {

    private val fakeProductRepository = FakeProductRepository()
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
                    val alternatives = fakeProductRepository
                        .getSuggestedAlternatives(excludeProductId = productEntity.id)
                        .map { alternative ->
                            Product(
                                id = alternative.id,
                                name = alternative.name,
                                brand = alternative.brand,
                                nutriscore = alternative.nutriscore,
                                ingredients = alternative.ingredients.map { ingredient ->
                                    Ingredient(
                                        name = ingredient.name,
                                        type = ingredient.type,
                                        description = ingredient.description
                                    )
                                },
                                imageUrl = alternative.imageUrl
                            )
                        }
                    val scanResult = createScanResult(productEntity, calculatedNutriScore, alternatives)
                    val analysisSettings = AnalysisSettings(
                        selectedAllergens = mapSelectedAllergens(settingsDataStore.selectedAllergens.first()),
                        halalModeEnabled = settingsDataStore.halalMode.first()
                    )
                    val productAnalysis = AnalysisEngine.analyze(scanResult.product, analysisSettings)

                    _uiState.value = _uiState.value.copy(
                        scanResult = scanResult,
                        productEntity = productEntity,
                        productAnalysis = productAnalysis,
                        isSaved = isSaved,
                        isFavorite = productEntity.isFavorite,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Product not found",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Unknown error",
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
        
        val shareText = buildString {
            appendLine("${scanResult.product.name} - ${scanResult.product.brand}")
            appendLine("Nutri-Score: ${scanResult.product.nutriscore.grade}")
            appendLine()
            
            analysis?.let { productAnalysis ->
                appendLine("Safety Status: ${productAnalysis.safetyStatus.name}")
                if (productAnalysis.warnings.isNotEmpty()) {
                    appendLine("Warnings:")
                    productAnalysis.warnings.forEach { warning ->
                        appendLine("- ${warning.title}: ${warning.message}")
                    }
                }
                appendLine()
                appendLine("Recommendation: ${productAnalysis.recommendation.title}")
                appendLine(productAnalysis.recommendation.message)
                appendLine()
                appendLine("Explanation:")
                appendLine(productAnalysis.explanation)
                if (productAnalysis.detectedAllergens.isNotEmpty()) {
                    appendLine()
                    appendLine("Detected allergens: ${productAnalysis.detectedAllergens.joinToString(", ")}")
                }
            }
            
            appendLine("Ingredients:")
            scanResult.product.ingredients.forEach { ingredient ->
                appendLine("- ${ingredient.name}")
            }
            
            appendLine()
            appendLine("Shared from NutriScan app")
        }
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Product Analysis: ${scanResult.product.name}")
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
            NutriScore.A -> "Excellent choice! This product has a very good nutritional quality."
            NutriScore.B -> "Good nutritional quality. This product can be part of a balanced diet."
            NutriScore.C -> "Average nutritional quality. Consume in moderation."
            NutriScore.D -> "Poor nutritional quality. Limit consumption."
            NutriScore.E -> "Very poor nutritional quality. Avoid regular consumption."
        }

        return ScanResult(product, explanation, alternatives)
    }
}
