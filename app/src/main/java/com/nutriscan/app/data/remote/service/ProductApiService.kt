package com.nutriscan.app.data.remote.service

import com.nutriscan.app.data.remote.api.OpenFoodFactsApi
import com.nutriscan.app.data.remote.dto.OpenFoodFactsProduct

data class ProductBarcodeResponseDto(
    val product: OpenFoodFactsProduct?
)

interface ProductApiService {
    suspend fun getProductByBarcode(barcode: String): ProductBarcodeResponseDto
}

class OpenFoodFactsProductApiService(
    private val api: OpenFoodFactsApi
) : ProductApiService {
    override suspend fun getProductByBarcode(barcode: String): ProductBarcodeResponseDto {
        val response = api.getProductByBarcode(barcode)
        return ProductBarcodeResponseDto(product = response.product)
    }
}
