package com.raitha.bharosa.data.local.dao

import androidx.room.*
import com.raitha.bharosa.data.local.entity.FarmerEntity
import com.raitha.bharosa.data.local.entity.SoilDataEntity
import com.raitha.bharosa.data.local.entity.CropSeasonEntity
import com.raitha.bharosa.data.local.entity.SimulatedSnapshotEntity
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────
//  FarmerDao
// ─────────────────────────────────────────────

@Dao
interface FarmerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarmer(farmer: FarmerEntity): Long

    @Update
    suspend fun updateFarmer(farmer: FarmerEntity)

    @Delete
    suspend fun deleteFarmer(farmer: FarmerEntity)

    @Query("SELECT * FROM farmers WHERE id = :id")
    suspend fun getFarmerById(id: Int): FarmerEntity?

    @Query("SELECT * FROM farmers ORDER BY createdAt DESC LIMIT 1")
    fun getLatestFarmer(): Flow<FarmerEntity?>

    @Query("SELECT * FROM farmers ORDER BY createdAt DESC")
    fun getAllFarmers(): Flow<List<FarmerEntity>>

    @Query("SELECT COUNT(*) FROM farmers")
    suspend fun getFarmerCount(): Int
}

// ─────────────────────────────────────────────
//  SoilDataDao
// ─────────────────────────────────────────────

@Dao
interface SoilDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSoilData(data: SoilDataEntity): Long

    @Query("SELECT * FROM soil_data WHERE farmerId = :farmerId ORDER BY recordedAt DESC LIMIT 1")
    fun getLatestSoilData(farmerId: Int): Flow<SoilDataEntity?>

    @Query("SELECT * FROM soil_data WHERE farmerId = :farmerId ORDER BY recordedAt DESC")
    fun getAllSoilDataForFarmer(farmerId: Int): Flow<List<SoilDataEntity>>

    @Query("DELETE FROM soil_data WHERE farmerId = :farmerId")
    suspend fun clearSoilDataForFarmer(farmerId: Int)

    @Query("SELECT * FROM soil_data WHERE farmerId = :farmerId ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestSoilDataOnce(farmerId: Int): SoilDataEntity?
}

// ─────────────────────────────────────────────
//  CropHistoryDao
// ─────────────────────────────────────────────

@Dao
interface CropHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: CropSeasonEntity): Long

    @Update
    suspend fun updateRecord(record: CropSeasonEntity)

    @Delete
    suspend fun deleteRecord(record: CropSeasonEntity)

    @Query("SELECT * FROM crop_history WHERE farmerId = :farmerId ORDER BY sowingDate DESC")
    fun getAllRecordsForFarmer(farmerId: Int): Flow<List<CropSeasonEntity>>

    @Query("SELECT * FROM crop_history WHERE id = :id")
    suspend fun getRecordById(id: Int): CropSeasonEntity?

    @Query("SELECT COUNT(*) FROM crop_history WHERE farmerId = :farmerId")
    suspend fun getRecordCountForFarmer(farmerId: Int): Int
}

// ─────────────────────────────────────────────
//  SimulatedSnapshotDao
// ─────────────────────────────────────────────

@Dao
interface SimulatedSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SimulatedSnapshotEntity): Long

    @Query("SELECT * FROM simulated_snapshots WHERE farmerId = :farmerId ORDER BY recordedAt DESC LIMIT 1")
    fun getLatestSnapshot(farmerId: Int): Flow<SimulatedSnapshotEntity?>

    @Query("SELECT * FROM simulated_snapshots WHERE farmerId = :farmerId ORDER BY recordedAt DESC LIMIT :limit")
    fun getRecentSnapshots(farmerId: Int, limit: Int = 24): Flow<List<SimulatedSnapshotEntity>>

    @Query("DELETE FROM simulated_snapshots WHERE recordedAt < :beforeEpoch")
    suspend fun pruneOldSnapshots(beforeEpoch: Long)
}
