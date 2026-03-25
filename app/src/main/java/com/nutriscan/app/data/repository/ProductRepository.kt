package com.nutriscan.app.data.repository

import com.nutriscan.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun searchProducts(query: String): Flow<List<ProductEntity>>
    suspend fun getProductById(productId: String): ProductEntity?
    suspend fun getProductByBarcode(barcode: String): ProductEntity?
    fun getRecentScans(): Flow<List<ProductEntity>>
    suspend fun saveProduct(product: ProductEntity)
    suspend fun toggleFavorite(productId: String, isFavorite: Boolean)
}
