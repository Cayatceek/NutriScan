package com.nutriscan.app.data.remote.api

import com.nutriscan.app.data.remote.dto.OpenFoodFactsResponse
import com.nutriscan.app.data.remote.dto.OpenFoodFactsSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {

    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): OpenFoodFactsResponse

    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") query: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("json") json: Int = 1,
        @Query("fields") fields: String = "code,product_name,brands,ingredients_text,nutriscore_grade,image_url,image_front_url,nutriments,additives_tags,allergens"
    ): OpenFoodFactsSearchResponse

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"
    }
}
