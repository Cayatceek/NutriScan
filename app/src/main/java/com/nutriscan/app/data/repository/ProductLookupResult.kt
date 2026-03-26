package com.nutriscan.app.data.repository

import com.nutriscan.app.data.local.entity.ProductEntity

enum class ProductLookupSource {
    REMOTE,
    CACHE
}

sealed interface ProductLookupResult {
    data class Success(
        val product: ProductEntity,
        val source: ProductLookupSource,
        val isFallback: Boolean = false
    ) : ProductLookupResult

    data object NotFound : ProductLookupResult

    data class NetworkError(
        val cachedProduct: ProductEntity? = null
    ) : ProductLookupResult

    data class ServerUnavailable(
        val cachedProduct: ProductEntity? = null
    ) : ProductLookupResult

    data class UnknownError(
        val cachedProduct: ProductEntity? = null,
        val throwable: Throwable? = null
    ) : ProductLookupResult
}
