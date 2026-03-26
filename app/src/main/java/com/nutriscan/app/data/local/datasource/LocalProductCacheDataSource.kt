package com.nutriscan.app.data.local.datasource

import com.nutriscan.app.data.local.dao.ProductDao
import com.nutriscan.app.data.local.entity.ProductEntity

class LocalProductCacheDataSource(
    private val productDao: ProductDao
) {
    suspend fun getProductById(productId: String): ProductEntity? {
        return productDao.getProductById(productId)
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return productDao.getProductByBarcode(barcode)
    }

    suspend fun saveProduct(product: ProductEntity) {
        productDao.insertProduct(product)
    }

    suspend fun saveProducts(products: List<ProductEntity>) {
        productDao.insertProducts(products)
    }
}
