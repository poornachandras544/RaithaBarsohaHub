package com.raitha.bharosa.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.raitha.bharosa.data.local.dao.CropHistoryDao
import com.raitha.bharosa.data.local.dao.FarmerDao
import com.raitha.bharosa.data.local.dao.SimulatedSnapshotDao
import com.raitha.bharosa.data.local.dao.SoilDataDao
import com.raitha.bharosa.data.local.entity.CropSeasonEntity
import com.raitha.bharosa.data.local.entity.FarmerEntity
import com.raitha.bharosa.data.local.entity.SimulatedSnapshotEntity
import com.raitha.bharosa.data.local.entity.SoilDataEntity

@Database(
    entities = [
        FarmerEntity::class,
        SoilDataEntity::class,
        CropSeasonEntity::class,
        SimulatedSnapshotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun farmerDao(): FarmerDao
    abstract fun soilDataDao(): SoilDataDao
    abstract fun cropHistoryDao(): CropHistoryDao
    abstract fun simulatedSnapshotDao(): SimulatedSnapshotDao

    companion object {
        const val DATABASE_NAME = "raitha_bharosa_db"
    }
}
