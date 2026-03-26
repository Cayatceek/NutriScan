package com.nutriscan.app.data.repository

import android.content.Context
import com.nutriscan.app.data.local.AppDatabase
import com.nutriscan.app.data.local.UzbekProductsData
import com.nutriscan.app.data.local.datasource.LocalProductCacheDataSource
import com.nutriscan.app.data.local.dao.ProductDao
import com.nutriscan.app.data.local.dao.ScanHistoryDao
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.data.mapper.OpenFoodFactsProductMapper
import com.nutriscan.app.data.remote.NetworkModule
import com.nutriscan.app.data.remote.api.OpenFoodFactsApi
import com.nutriscan.app.data.remote.datasource.RemoteProductDataSource
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
    private val localCacheDataSource = LocalProductCacheDataSource(productDao)
    private val remoteProductDataSource = RemoteProductDataSource()

    override fun searchProducts(query: String): Flow<List<ProductEntity>> = flow {
        val localResults = productDao.searchProducts(query).first()
        if (localResults.isNotEmpty()) {
            emit(localResults)
            return@flow
        }

        try {
            val allResults = (fetchFromApi(query) + fetchFromUzbekDb(query)).distinctBy { it.id }
            if (allResults.isNotEmpty()) {
                localCacheDataSource.saveProducts(allResults)
            }
            emit(allResults)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getProductById(productId: String): ProductEntity? {
        return localCacheDataSource.getProductById(productId)
    }

    override suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return when (val result = lookupBarcode(barcode)) {
            is ProductLookupResult.Success -> result.product
            else -> null
        }
    }

    suspend fun lookupBarcode(barcode: String): ProductLookupResult {
        val cachedProduct = localCacheDataSource.getProductByBarcode(barcode)

        return when (val remoteResult = remoteProductDataSource.lookupBarcode(barcode)) {
            is ProductLookupResult.Success -> {
                val remoteProduct = remoteResult.product.copy(source = "api")
                localCacheDataSource.saveProduct(remoteProduct)
                ProductLookupResult.Success(
                    product = remoteProduct,
                    source = ProductLookupSource.REMOTE
                )
            }

            ProductLookupResult.NotFound -> ProductLookupResult.NotFound

            is ProductLookupResult.NetworkError -> {
                cachedProduct?.let {
                    ProductLookupResult.Success(
                        product = it.copy(source = "cache"),
                        source = ProductLookupSource.CACHE,
                        isFallback = true
                    )
                } ?: ProductLookupResult.NetworkError()
            }

            is ProductLookupResult.ServerUnavailable -> {
                cachedProduct?.let {
                    ProductLookupResult.Success(
                        product = it.copy(source = "cache"),
                        source = ProductLookupSource.CACHE,
                        isFallback = true
                    )
                } ?: ProductLookupResult.ServerUnavailable()
            }

            is ProductLookupResult.UnknownError -> {
                cachedProduct?.let {
                    ProductLookupResult.Success(
                        product = it.copy(source = "cache"),
                        source = ProductLookupSource.CACHE,
                        isFallback = true
                    )
                } ?: ProductLookupResult.UnknownError(throwable = remoteResult.throwable)
            }
        }
    }

    override fun getRecentScans(): Flow<List<ProductEntity>> {
        return scanHistoryDao.getScanHistoryWithProducts()
            .map { historyItems -> historyItems.take(10).map { it.product } }
    }

    override suspend fun saveProduct(product: ProductEntity) {
        localCacheDataSource.saveProduct(product)
    }

    override suspend fun toggleFavorite(productId: String, isFavorite: Boolean) {
        productDao.updateFavoriteStatus(productId, isFavorite)
    }

    private suspend fun fetchFromApi(query: String): List<ProductEntity> {
        return try {
            val response = api.searchProducts(query)
            response.products.map { OpenFoodFactsProductMapper.toEntity(it, source = "api") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun fetchFromUzbekDb(query: String): List<ProductEntity> {
        return UzbekProductsData.search(query)
    }

}
