package com.nutriscan.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.R
import com.nutriscan.app.data.local.SettingsDataStore
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.data.repository.ProductRepositoryImpl
import com.nutriscan.app.domain.analysis.AnalysisEngine
import com.nutriscan.app.domain.analysis.AnalysisSettings
import com.nutriscan.app.domain.analyzer.Allergen
import com.nutriscan.app.domain.analyzer.NutriScoreCalculator
import com.nutriscan.app.localization.UiText
import com.nutriscan.app.localization.uiText
import com.nutriscan.app.model.Ingredient
import com.nutriscan.app.model.IngredientType
import com.nutriscan.app.model.NutriScore
import com.nutriscan.app.model.Product
import com.nutriscan.app.model.ProductAnalysis
import com.nutriscan.app.model.ScanResult
import com.nutriscan.app.util.BrandedLoadingOutcome
import com.nutriscan.app.util.runBrandedLoadingGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ResultUiState(
    val scanResult: ScanResult? = null,
    val productEntity: ProductEntity? = null,
    val productAnalysis: ProductAnalysis? = null,
    val isCachedResult: Boolean = false,
    val isLoading: Boolean = false,
    val isFavorite: Boolean = false,
    val error: UiText? = null,
    val loadState: ScreenLoadState = ScreenLoadState.Idle
)

class ResultViewModel(application: Application) : AndroidViewModel(application) {

    private val productRepository = ProductRepositoryImpl(application)
    private val settingsDataStore = SettingsDataStore(application)

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    fun loadResultIfNeeded(productId: String) {
        val currentProductId = _uiState.value.productEntity?.id
        if (
            _uiState.value.loadState == ScreenLoadState.Success &&
            currentProductId == productId
        ) {
            return
        }
        loadResult(productId)
    }

    fun loadResult(productId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                loadState = ScreenLoadState.Loading
            )
            when (val gatedLoad = runBrandedLoadingGate {
                val productEntity = productRepository.getProductById(productId)
                if (productEntity != null) {
                    val calculatedNutriScore = if (productEntity.nutriscore == NutriScore.C) {
                        NutriScoreCalculator.calculateFromIngredients(productEntity.ingredients)
                    } else {
                        productEntity.nutriscore
                    }

                    val scanResult = createScanResult(productEntity, calculatedNutriScore, emptyList())
                    val analysisSettings = AnalysisSettings(
                        selectedAllergens = mapSelectedAllergens(settingsDataStore.selectedAllergens.first()),
                        halalModeEnabled = settingsDataStore.halalMode.first()
                    )
                    val productAnalysis = AnalysisEngine.analyze(scanResult.product, analysisSettings)

                    ResultUiState(
                        scanResult = scanResult,
                        productEntity = productEntity,
                        productAnalysis = productAnalysis,
                        isCachedResult = productEntity.source == "cache",
                        isFavorite = productEntity.isFavorite,
                        isLoading = false,
                        loadState = ScreenLoadState.Success
                    )
                } else {
                    ResultUiState(
                        error = uiText(R.string.result_error_product_not_found),
                        isLoading = false,
                        loadState = ScreenLoadState.Error
                    )
                }
            }) {
                is BrandedLoadingOutcome.Success -> {
                    _uiState.value = gatedLoad.value
                }

                BrandedLoadingOutcome.Timeout -> {
                    _uiState.value = _uiState.value.copy(
                        error = uiText(R.string.result_error_default),
                        isLoading = false,
                        loadState = ScreenLoadState.Error
                    )
                }

                is BrandedLoadingOutcome.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = gatedLoad.throwable.message
                            ?.takeIf { it.isNotBlank() }
                            ?.let(UiText::DynamicString)
                            ?: uiText(R.string.error_generic),
                        isLoading = false,
                        loadState = ScreenLoadState.Error
                    )
                }
            }
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

        return ScanResult(
            product = product,
            explanation = "",
            alternatives = alternatives
        )
    }
}
