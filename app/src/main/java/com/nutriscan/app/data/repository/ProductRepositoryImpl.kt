package com.nutriscan.app.data.repository

import android.content.Context
import com.nutriscan.app.data.local.AppDatabase
import com.nutriscan.app.data.local.UzbekProductsData
import com.nutriscan.app.data.local.dao.ProductDao
import com.nutriscan.app.data.local.dao.ScanHistoryDao
import com.nutriscan.app.data.local.entity.IngredientEntity
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.data.remote.NetworkModule
import com.nutriscan.app.data.remote.api.OpenFoodFactsApi
import com.nutriscan.app.model.IngredientType
import com.nutriscan.app.model.NutriScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ProductRepositoryImpl(context: Context) : ProductRepository {

    private val productDao: ProductDao = AppDatabase.getDatabase(context).productDao()
    private val scanHistoryDao: ScanHistoryDao = AppDatabase.getDatabase(context).scanHistoryDao()
    private val api: OpenFoodFactsApi = NetworkModule.openFoodFactsApi

    override fun searchProducts(query: String): Flow<List<ProductEntity>> = flow {
        val localResults = productDao.searchProducts(query).first()
        if (localResults.isNotEmpty()) {
            emit(localResults)
            return@flow
        }

        try {
            val allResults = (fetchFromApi(query) + fetchFromUzbekDb(query)).distinctBy { it.id }
            if (allResults.isNotEmpty()) {
                productDao.insertProducts(allResults)
            }
            emit(allResults)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getProductById(productId: String): ProductEntity? {
        return productDao.getProductById(productId)
    }

    override suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        // Check local first
        productDao.getProductByBarcode(barcode)?.let { return it }
        
        // Check Uzbek DB
        UzbekProductsData.getByBarcode(barcode)?.let { return it }
        
        // Fetch from API
        return try {
            val response = api.getProductByBarcode(barcode)
            response.product?.let { apiProduct ->
                mapApiProductToEntity(apiProduct).also {
                    productDao.insertProduct(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun getRecentScans(): Flow<List<ProductEntity>> {
        return scanHistoryDao.getScanHistoryWithProducts()
            .map { historyItems -> historyItems.take(10).map { it.product } }
    }

    override suspend fun saveProduct(product: ProductEntity) {
        productDao.insertProduct(product)
    }

    override suspend fun toggleFavorite(productId: String, isFavorite: Boolean) {
        productDao.updateFavoriteStatus(productId, isFavorite)
    }

    private suspend fun fetchFromApi(query: String): List<ProductEntity> {
        return try {
            val response = api.searchProducts(query)
            response.products.map { mapApiProductToEntity(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun fetchFromUzbekDb(query: String): List<ProductEntity> {
        return UzbekProductsData.search(query)
    }

    private fun mapApiProductToEntity(apiProduct: com.nutriscan.app.data.remote.dto.OpenFoodFactsProduct): ProductEntity {
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
            source = "api"
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
