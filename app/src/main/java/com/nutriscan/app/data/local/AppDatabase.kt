package com.nutriscan.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nutriscan.app.data.local.dao.ProductDao
import com.nutriscan.app.data.local.dao.ScanHistoryDao
import com.nutriscan.app.data.local.entity.Converters
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.data.local.entity.ScanHistoryEntity

@Database(
    entities = [ProductEntity::class, ScanHistoryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutriscan_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
