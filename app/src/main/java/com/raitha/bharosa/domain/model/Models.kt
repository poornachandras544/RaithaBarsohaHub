package com.raitha.bharosa.domain.model

// ─────────────────────────────────────────────
//  Farmer
// ─────────────────────────────────────────────

data class Farmer(
    val id: Int = 0,
    val name: String,
    val primaryCrop: CropType,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val villageName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    KANNADA("kn", "ಕನ್ನಡ")
}

// ─────────────────────────────────────────────
//  Soil Data
// ─────────────────────────────────────────────

data class SoilData(
    val id: Int = 0,
    val farmerId: Int,
    val nitrogen: Float,           // kg/ha
    val phosphorus: Float,         // kg/ha
    val potassium: Float,          // kg/ha
    val moisturePercent: Float,    // 0-100%
    val phLevel: Float = 6.5f,     // pH 0-14
    val organicCarbon: Float = 0.5f,
    val recordedAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────
//  Weather Data
// ─────────────────────────────────────────────

data class WeatherData(
    val temperatureCelsius: Float,
    val humidity: Float,           // %
    val windSpeedKmh: Float,
    val precipitationMm: Float,
    val conditionCode: Int,        // OWM weather condition code
    val conditionDescription: String,
    val feelsLike: Float,
    val uvIndex: Float = 0f,
    val fetchedAt: Long = System.currentTimeMillis()
)

data class WeatherForecastDay(
    val dateEpoch: Long,
    val maxTemp: Float,
    val minTemp: Float,
    val precipitationMm: Float,
    val humidity: Float,
    val conditionDescription: String,
    val conditionCode: Int
)

// ─────────────────────────────────────────────
//  Sowing Index
// ─────────────────────────────────────────────

/**
 * Composite index (0-100) representing how ideal current conditions
 * are for sowing the farmer's selected crop.
 */
data class SowingIndex(
    val score: Int,                      // 0-100
    val status: SowingStatus,
    val moistureScore: Int,              // 0-100 sub-score
    val temperatureScore: Int,           // 0-100 sub-score
    val nutrientScore: Int,              // 0-100 sub-score
    val weatherScore: Int,               // 0-100 sub-score
    val primaryRecommendation: String,
    val detailedReason: String,
    val calculatedAt: Long = System.currentTimeMillis()
)

enum class SowingStatus(val labelEn: String, val labelKn: String, val colorHex: String) {
    OPTIMAL("✅ Go – Sow Now!", "✅ ಈಗಲೇ ಬಿತ್ತನೆ ಮಾಡಿ!", "#2E7D32"),
    GOOD("👍 Good Conditions", "👍 ಉತ್ತಮ ಪರಿಸ್ಥಿತಿ", "#558B2F"),
    FAIR("⚠️ Wait – Monitor", "⚠️ ನಿರೀಕ್ಷಿಸಿ", "#F57F17"),
    POOR("🛑 Not Advised", "🛑 ಶಿಫಾರಸು ಮಾಡಲಾಗಿಲ್ಲ", "#B71C1C"),
    CRITICAL("🚨 Critical – Delay", "🚨 ವಿಳಂಬ ಮಾಡಿ", "#880E4F")
}

// ─────────────────────────────────────────────
//  Krishi Calendar
// ─────────────────────────────────────────────

data class KrishiCalendarEntry(
    val dateEpoch: Long,
    val dayLabel: String,              // "Today", "Tomorrow", "Mon Jun 9"
    val activity: FarmActivity,
    val priority: ActivityPriority,
    val description: String,
    val descriptionKn: String,
    val isOptimalWindow: Boolean = false
)

enum class FarmActivity(val labelEn: String, val labelKn: String, val emoji: String) {
    SOW("Sow Seeds", "ಬಿತ್ತನೆ ಮಾಡಿ", "🌱"),
    FERTILIZE("Apply Fertilizer", "ರಸಗೊಬ್ಬರ ಹಾಕಿ", "🪣"),
    IRRIGATE("Irrigate Field", "ನೀರಾವರಿ", "💧"),
    WAIT("Wait – Monitor", "ನಿರೀಕ್ಷಿಸಿ", "⏳"),
    HARVEST_PREP("Prepare for Harvest", "ಕೊಯ್ಲಿಗೆ ಸಿದ್ಧತೆ", "🌾"),
    PEST_CONTROL("Pest Control Check", "ಕೀಟ ನಿಯಂತ್ರಣ", "🐛"),
    AVOID_RAIN("Avoid – Heavy Rain", "ಮಳೆಯಿಂದ ದೂರ ಇರಿ", "🌧️"),
    SOIL_PREP("Prepare Soil", "ಮಣ್ಣು ತಯಾರಿ", "🚜")
}

enum class ActivityPriority { HIGH, MEDIUM, LOW }

// ─────────────────────────────────────────────
//  Crop History Season Record
// ─────────────────────────────────────────────

data class CropSeasonRecord(
    val id: Int = 0,
    val farmerId: Int,
    val cropType: CropType,
    val season: String,            // "Kharif 2024", "Rabi 2024-25"
    val sowingDate: Long,
    val harvestDate: Long? = null,
    val estimatedYieldKg: Float? = null,
    val actualYieldKg: Float? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
