package com.nutriscan.app.data.repository

import android.content.Context
import com.nutriscan.app.data.local.AppDatabase
import com.nutriscan.app.data.local.dao.ScanHistoryDao
import com.nutriscan.app.data.local.entity.ScanHistoryEntity
import com.nutriscan.app.data.local.entity.ScanHistoryWithProduct
import kotlinx.coroutines.flow.Flow

class ScanHistoryRepository(context: Context) {

    private val scanHistoryDao: ScanHistoryDao = AppDatabase.getDatabase(context).scanHistoryDao()

    fun getScanHistoryWithProducts(): Flow<List<ScanHistoryWithProduct>> {
        return scanHistoryDao.getScanHistoryWithProducts()
    }

    suspend fun addToHistory(productId: String, imagePath: String? = null) {
        val scanHistory = ScanHistoryEntity(
            id = "scan_${System.currentTimeMillis()}",
            productId = productId,
            imagePath = imagePath
        )
        scanHistoryDao.insertScanHistory(scanHistory)
    }

    suspend fun deleteScanHistory(scanId: String) {
        scanHistoryDao.deleteScanHistory(scanId)
    }

    suspend fun deleteAllHistory() {
        scanHistoryDao.deleteAllScanHistory()
    }

    suspend fun getHistoryCount(): Int {
        return scanHistoryDao.getScanHistoryCount()
    }

    suspend fun hasHistoryForProduct(productId: String): Boolean {
        return scanHistoryDao.getScanHistoryCountForProduct(productId) > 0
    }
}
