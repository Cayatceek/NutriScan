package com.nutriscan.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ScanHistoryWithProduct(
    @Embedded
    val scanHistory: ScanHistoryEntity,
    
    @Relation(
        parentColumn = "productId",
        entityColumn = "id"
    )
    val product: ProductEntity
)
