package com.nutriscan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.nutriscan.app.model.IngredientType
import com.nutriscan.app.model.NutriScore

@Entity(tableName = "products")
@TypeConverters(Converters::class)
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val barcode: String? = null,
    val name: String,
    val brand: String,
    val nutriscore: NutriScore,
    val ingredients: List<IngredientEntity>,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false,
    val source: String = "api", // "api", "local", "uzbek"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class IngredientEntity(
    val name: String,
    val type: IngredientType,
    val description: String? = null,
    val eCode: String? = null
)

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey
    val id: String,
    val productId: String,
    val scannedAt: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    val notes: String? = null
)
