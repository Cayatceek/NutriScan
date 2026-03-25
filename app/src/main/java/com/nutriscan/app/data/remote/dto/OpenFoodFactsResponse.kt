package com.nutriscan.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OpenFoodFactsResponse(
    @SerializedName("code")
    val code: String?,
    @SerializedName("product")
    val product: OpenFoodFactsProduct?,
    @SerializedName("status")
    val status: Int,
    @SerializedName("status_verbose")
    val statusVerbose: String
)

data class OpenFoodFactsSearchResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_count")
    val pageCount: Int,
    @SerializedName("page_size")
    val pageSize: Int,
    @SerializedName("products")
    val products: List<OpenFoodFactsProduct>,
    @SerializedName("skip")
    val skip: Int
)

data class OpenFoodFactsProduct(
    @SerializedName("code")
    val code: String?,
    @SerializedName("product_name")
    val productName: String?,
    @SerializedName("brands")
    val brands: String?,
    @SerializedName("ingredients_text")
    val ingredientsText: String?,
    @SerializedName("ingredients")
    val ingredients: List<OpenFoodFactsIngredient>?,
    @SerializedName("nutriscore_grade")
    val nutriscoreGrade: String?,
    @SerializedName("nutriments")
    val nutriments: OpenFoodFactsNutriments?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("image_front_url")
    val imageFrontUrl: String?,
    @SerializedName("categories")
    val categories: String?,
    @SerializedName("labels")
    val labels: String?,
    @SerializedName("additives_tags")
    val additivesTags: List<String>?,
    @SerializedName("allergens")
    val allergens: String?
)

data class OpenFoodFactsIngredient(
    @SerializedName("id")
    val id: String?,
    @SerializedName("text")
    val text: String?,
    @SerializedName("percent_estimate")
    val percentEstimate: Double?,
    @SerializedName("vegan")
    val vegan: String?,
    @SerializedName("vegetarian")
    val vegetarian: String?
)

data class OpenFoodFactsNutriments(
    @SerializedName("energy-kcal_100g")
    val energyKcal100g: Double?,
    @SerializedName("energy_100g")
    val energy100g: Double?,
    @SerializedName("proteins_100g")
    val proteins100g: Double?,
    @SerializedName("carbohydrates_100g")
    val carbohydrates100g: Double?,
    @SerializedName("sugars_100g")
    val sugars100g: Double?,
    @SerializedName("fat_100g")
    val fat100g: Double?,
    @SerializedName("saturated-fat_100g")
    val saturatedFat100g: Double?,
    @SerializedName("fiber_100g")
    val fiber100g: Double?,
    @SerializedName("sodium_100g")
    val sodium100g: Double?,
    @SerializedName("salt_100g")
    val salt100g: Double?
)
