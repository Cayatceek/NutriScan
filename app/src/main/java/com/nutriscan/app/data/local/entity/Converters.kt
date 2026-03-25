package com.nutriscan.app.data.local.entity

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nutriscan.app.model.IngredientType
import com.nutriscan.app.model.NutriScore

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromNutriScore(score: NutriScore): String = score.name

    @TypeConverter
    fun toNutriScore(value: String): NutriScore = NutriScore.valueOf(value)

    @TypeConverter
    fun fromIngredientType(type: IngredientType): String = type.name

    @TypeConverter
    fun toIngredientType(value: String): IngredientType = IngredientType.valueOf(value)

    @TypeConverter
    fun fromIngredientList(ingredients: List<IngredientEntity>): String {
        return gson.toJson(ingredients)
    }

    @TypeConverter
    fun toIngredientList(value: String): List<IngredientEntity> {
        val type = object : TypeToken<List<IngredientEntity>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
