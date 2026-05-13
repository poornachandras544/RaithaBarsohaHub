package com.raitha.bharosa.data.repository

import com.raitha.bharosa.BuildConfig
import com.raitha.bharosa.data.local.dao.CropHistoryDao
import com.raitha.bharosa.data.local.dao.FarmerDao
import com.raitha.bharosa.data.local.dao.SimulatedSnapshotDao
import com.raitha.bharosa.data.local.dao.SoilDataDao
import com.raitha.bharosa.data.local.entity.CropSeasonEntity
import com.raitha.bharosa.data.local.entity.FarmerEntity
import com.raitha.bharosa.data.local.entity.SimulatedSnapshotEntity
import com.raitha.bharosa.data.local.entity.SoilDataEntity
import com.raitha.bharosa.data.remote.WeatherApiService
import com.raitha.bharosa.data.simulation.DataGenerator
import com.raitha.bharosa.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────
//  Farmer Repository
// ─────────────────────────────────────────────

@Singleton
class FarmerRepository @Inject constructor(
    private val farmerDao: FarmerDao
) {
    suspend fun saveFarmer(farmer: Farmer): Long {
        val entity = FarmerEntity(
            id = farmer.id,
            name = farmer.name,
            primaryCrop = farmer.primaryCrop.name,
            language = farmer.language.code,
            villageName = farmer.villageName,
            latitude = farmer.latitude,
            longitude = farmer.longitude,
            createdAt = farmer.createdAt
        )
        return farmerDao.insertFarmer(entity)
    }

    fun getLatestFarmer(): Flow<Farmer?> =
        farmerDao.getLatestFarmer().map { entity ->
            entity?.toDomain()
        }

    suspend fun getFarmerCount(): Int = farmerDao.getFarmerCount()

    suspend fun isOnboarded(): Boolean = farmerDao.getFarmerCount() > 0

    private fun FarmerEntity.toDomain(): Farmer = Farmer(
        id = id,
        name = name,
        primaryCrop = CropType.valueOf(primaryCrop),
        language = if (language == "kn") AppLanguage.KANNADA else AppLanguage.ENGLISH,
        villageName = villageName,
        latitude = latitude,
        longitude = longitude,
        createdAt = createdAt
    )
}

// ─────────────────────────────────────────────
//  Soil Data Repository
// ─────────────────────────────────────────────

@Singleton
class SoilDataRepository @Inject constructor(
    private val soilDataDao: SoilDataDao,
    private val dataGenerator: DataGenerator
) {
    suspend fun saveSoilData(data: SoilData): Long {
        val entity = SoilDataEntity(
            id = data.id,
            farmerId = data.farmerId,
            nitrogen = data.nitrogen,
            phosphorus = data.phosphorus,
            potassium = data.potassium,
            moisturePercent = data.moisturePercent,
            phLevel = data.phLevel,
            organicCarbon = data.organicCarbon,
            recordedAt = data.recordedAt
        )
        return soilDataDao.insertSoilData(entity)
    }

    fun getLatestSoilData(farmerId: Int): Flow<SoilData?> =
        soilDataDao.getLatestSoilData(farmerId).map { entity ->
            entity?.toDomain()
        }

    fun getAllSoilData(farmerId: Int): Flow<List<SoilData>> =
        soilDataDao.getAllSoilDataForFarmer(farmerId).map { list ->
            list.map { it.toDomain() }
        }

    /** Generate simulated soil data and persist it */
    suspend fun generateAndSaveSimulatedData(farmerId: Int, crop: CropType): SoilData {
        val moisture = dataGenerator.generateMoisturePercent()
        val data = SoilData(
            farmerId = farmerId,
            nitrogen = dataGenerator.generateNitrogenLevel(crop),
            phosphorus = dataGenerator.generatePhosphorusLevel(crop),
            potassium = dataGenerator.generatePotassiumLevel(crop),
            moisturePercent = moisture,
            phLevel = dataGenerator.generatePhLevel(),
            organicCarbon = dataGenerator.generateOrganicCarbon()
        )
        saveSoilData(data)
        return data
    }

    private fun SoilDataEntity.toDomain(): SoilData = SoilData(
        id = id, farmerId = farmerId,
        nitrogen = nitrogen, phosphorus = phosphorus, potassium = potassium,
        moisturePercent = moisturePercent, phLevel = phLevel,
        organicCarbon = organicCarbon, recordedAt = recordedAt
    )
}

// ─────────────────────────────────────────────
//  Crop History Repository
// ─────────────────────────────────────────────

@Singleton
class CropHistoryRepository @Inject constructor(
    private val cropHistoryDao: CropHistoryDao
) {
    suspend fun saveRecord(record: CropSeasonRecord): Long {
        return cropHistoryDao.insertRecord(record.toEntity())
    }

    suspend fun updateRecord(record: CropSeasonRecord) {
        cropHistoryDao.updateRecord(record.toEntity())
    }

    fun getAllRecords(farmerId: Int): Flow<List<CropSeasonRecord>> =
        cropHistoryDao.getAllRecordsForFarmer(farmerId).map { list ->
            list.map { it.toDomain() }
        }

    private fun CropSeasonRecord.toEntity() = CropSeasonEntity(
        id = id, farmerId = farmerId, cropType = cropType.name, season = season,
        sowingDate = sowingDate, harvestDate = harvestDate,
        estimatedYieldKg = estimatedYieldKg, actualYieldKg = actualYieldKg,
        notes = notes, createdAt = createdAt
    )

    private fun CropSeasonEntity.toDomain() = CropSeasonRecord(
        id = id, farmerId = farmerId, cropType = CropType.valueOf(cropType),
        season = season, sowingDate = sowingDate, harvestDate = harvestDate,
        estimatedYieldKg = estimatedYieldKg, actualYieldKg = actualYieldKg,
        notes = notes, createdAt = createdAt
    )
}

// ─────────────────────────────────────────────
//  Weather Repository (API + Fallback Simulation)
// ─────────────────────────────────────────────

sealed class WeatherResult {
    data class Success(val data: WeatherData) : WeatherResult()
    data class SimulatedFallback(val data: WeatherData) : WeatherResult()
    data class Error(val message: String) : WeatherResult()
}

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApiService: WeatherApiService,
    private val dataGenerator: DataGenerator,
    private val simulatedSnapshotDao: SimulatedSnapshotDao
) {
    // Default coordinates: Bengaluru, Karnataka
    private val defaultLat = 12.9716
    private val defaultLon = 77.5946

    suspend fun getCurrentWeather(lat: Double? = null, lon: Double? = null): WeatherResult {
        return try {
            val response = weatherApiService.getCurrentWeather(
                lat = lat ?: defaultLat,
                lon = lon ?: defaultLon,
                apiKey = BuildConfig.OWM_API_KEY
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val condition = body.weather.firstOrNull()
                WeatherResult.Success(
                    WeatherData(
                        temperatureCelsius = body.main.temp,
                        humidity = body.main.humidity,
                        windSpeedKmh = body.wind.speed * 3.6f,
                        precipitationMm = body.rain?.oneHour ?: 0f,
                        conditionCode = condition?.id ?: 800,
                        conditionDescription = condition?.description?.replaceFirstChar { it.uppercase() } ?: "Clear",
                        feelsLike = body.main.feelsLike
                    )
                )
            } else {
                // Fallback to simulation if API key not configured
                WeatherResult.SimulatedFallback(dataGenerator.generateCurrentWeather())
            }
        } catch (e: Exception) {
            // Network unavailable → use simulation
            WeatherResult.SimulatedFallback(dataGenerator.generateCurrentWeather())
        }
    }

    fun getLiveForecast(): Flow<List<WeatherForecastDay>> = flow {
        emit(dataGenerator.generate7DayForecast())
    }.flowOn(Dispatchers.IO)

    suspend fun saveSnapshot(snapshot: SimulatedSnapshotEntity) {
        simulatedSnapshotDao.insertSnapshot(snapshot)
        // Keep DB lean: prune data older than 7 days
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        simulatedSnapshotDao.pruneOldSnapshots(sevenDaysAgo)
    }
}
