package com.raitha.bharosa.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────
//  Farmer Entity
// ─────────────────────────────────────────────

@Entity(tableName = "farmers")
data class FarmerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val primaryCrop: String,       // CropType.name
    val language: String = "en",   // "en" | "kn"
    val villageName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────
//  Soil Data Entity
// ─────────────────────────────────────────────

@Entity(
    tableName = "soil_data",
    foreignKeys = [ForeignKey(
        entity = FarmerEntity::class,
        parentColumns = ["id"],
        childColumns = ["farmerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["farmerId"])]
)
data class SoilDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmerId: Int,
    val nitrogen: Float,
    val phosphorus: Float,
    val potassium: Float,
    val moisturePercent: Float,
    val phLevel: Float = 6.5f,
    val organicCarbon: Float = 0.5f,
    val recordedAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────
//  Crop Season Record Entity
// ─────────────────────────────────────────────

@Entity(
    tableName = "crop_history",
    foreignKeys = [ForeignKey(
        entity = FarmerEntity::class,
        parentColumns = ["id"],
        childColumns = ["farmerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["farmerId"])]
)
data class CropSeasonEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmerId: Int,
    val cropType: String,          // CropType.name
    val season: String,
    val sowingDate: Long,
    val harvestDate: Long? = null,
    val estimatedYieldKg: Float? = null,
    val actualYieldKg: Float? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────
//  Simulated Parameter Snapshot Entity
// ─────────────────────────────────────────────

@Entity(tableName = "simulated_snapshots")
data class SimulatedSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmerId: Int,
    val temperature: Float,
    val humidity: Float,
    val moisturePercent: Float,
    val windSpeedKmh: Float,
    val precipitationMm: Float,
    val conditionCode: Int,
    val conditionDescription: String,
    val sowingIndexScore: Int,
    val sowingStatus: String,
    val recordedAt: Long = System.currentTimeMillis()
)
