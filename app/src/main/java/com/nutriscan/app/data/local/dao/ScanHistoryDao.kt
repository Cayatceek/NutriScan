package com.nutriscan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.data.local.entity.ScanHistoryEntity
import com.nutriscan.app.data.local.entity.ScanHistoryWithProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Transaction
    @Query("SELECT * FROM scan_history ORDER BY scannedAt DESC")
    fun getScanHistoryWithProducts(): Flow<List<ScanHistoryWithProduct>>

    @Query("SELECT * FROM scan_history ORDER BY scannedAt DESC")
    fun getAllScanHistory(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE productId = :productId ORDER BY scannedAt DESC")
    fun getScanHistoryForProduct(productId: String): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanHistory(scanHistory: ScanHistoryEntity)

    @Query("DELETE FROM scan_history WHERE id = :scanId")
    suspend fun deleteScanHistory(scanId: String)

    @Query("DELETE FROM scan_history WHERE productId = :productId")
    suspend fun deleteScanHistoryForProduct(productId: String)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAllScanHistory()

    @Query("SELECT COUNT(*) FROM scan_history")
    suspend fun getScanHistoryCount(): Int

    @Query("SELECT COUNT(*) FROM scan_history WHERE productId = :productId")
    suspend fun getScanHistoryCountForProduct(productId: String): Int
}
