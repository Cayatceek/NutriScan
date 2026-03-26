package com.nutriscan.app.data.remote.datasource

import com.nutriscan.app.data.mapper.OpenFoodFactsProductMapper
import com.nutriscan.app.data.remote.NetworkModule
import com.nutriscan.app.data.remote.service.OpenFoodFactsProductApiService
import com.nutriscan.app.data.remote.service.ProductApiService
import com.nutriscan.app.data.repository.ProductLookupResult
import com.nutriscan.app.data.repository.ProductLookupSource
import retrofit2.HttpException
import java.io.IOException

class RemoteProductDataSource(
    private val apiService: ProductApiService = OpenFoodFactsProductApiService(NetworkModule.openFoodFactsApi)
) {
    suspend fun lookupBarcode(barcode: String): ProductLookupResult {
        return try {
            val response = apiService.getProductByBarcode(barcode)
            val product = response.product
            if (product != null) {
                ProductLookupResult.Success(
                    product = OpenFoodFactsProductMapper.toEntity(product, source = "api"),
                    source = ProductLookupSource.REMOTE
                )
            } else {
                ProductLookupResult.NotFound
            }
        } catch (exception: IOException) {
            ProductLookupResult.NetworkError()
        } catch (exception: HttpException) {
            when (exception.code()) {
                404 -> ProductLookupResult.NotFound
                500, 502, 503, 504 -> ProductLookupResult.ServerUnavailable()
                else -> ProductLookupResult.UnknownError(throwable = exception)
            }
        } catch (exception: Exception) {
            ProductLookupResult.UnknownError(throwable = exception)
        }
    }
}
