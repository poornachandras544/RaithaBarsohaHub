package com.raitha.bharosa.domain.usecase

import com.raitha.bharosa.domain.model.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

// ─────────────────────────────────────────────
//  Calculate Sowing Index Use Case
// ─────────────────────────────────────────────

/**
 * Computes the composite Sowing Index (0-100) from soil, weather,
 * and nutrient parameters against the crop's optimal benchmarks.
 *
 * The index is a weighted average:
 *   - Moisture Score  : 30%
 *   - Temperature Score: 30%
 *   - Nutrient Score  : 25%
 *   - Weather Score   : 15%
 */
class CalculateSowingIndexUseCase @Inject constructor() {

    operator fun invoke(
        crop: CropType,
        soilData: SoilData?,
        weatherData: WeatherData
    ): SowingIndex {
        val moistureScore = calculateMoistureScore(crop, soilData?.moisturePercent ?: 20f)
        val tempScore = calculateTemperatureScore(crop, weatherData.temperatureCelsius)
        val nutrientScore = if (soilData != null) calculateNutrientScore(crop, soilData) else 60
        val weatherScore = calculateWeatherScore(weatherData)

        val composite = (
            moistureScore * 0.30f +
            tempScore * 0.30f +
            nutrientScore * 0.25f +
            weatherScore * 0.15f
        ).toInt()

        val status = scoreToStatus(composite)
        val recommendation = buildRecommendation(
            status, crop, soilData, weatherData, moistureScore, tempScore, nutrientScore
        )

        return SowingIndex(
            score = composite.coerceIn(0, 100),
            status = status,
            moistureScore = moistureScore,
            temperatureScore = tempScore,
            nutrientScore = nutrientScore,
            weatherScore = weatherScore,
            primaryRecommendation = recommendation.first,
            detailedReason = recommendation.second
        )
    }

    private fun calculateMoistureScore(crop: CropType, moisture: Float): Int {
        // Spec rule: if moisture > 30 → "Soil too wet to sow"
        return when {
            moisture > 30f -> {
                val excess = moisture - 30f
                (80 - excess * 3).toInt().coerceIn(0, 80)
            }
            moisture < 10f -> 20
            moisture in crop.optimalMoistureMin..crop.optimalMoistureMax -> 100
            else -> {
                val deviation = minOf(
                    abs(moisture - crop.optimalMoistureMin),
                    abs(moisture - crop.optimalMoistureMax)
                )
                (100 - deviation * 3f).toInt().coerceIn(30, 95)
            }
        }
    }

    private fun calculateTemperatureScore(crop: CropType, temp: Float): Int {
        return when {
            temp in crop.optimalTempMin..crop.optimalTempMax -> 100
            temp < crop.optimalTempMin - 5 || temp > crop.optimalTempMax + 5 -> 10
            temp < crop.optimalTempMin -> {
                val deviation = crop.optimalTempMin - temp
                (100 - deviation * 10f).toInt().coerceIn(20, 90)
            }
            else -> {
                val deviation = temp - crop.optimalTempMax
                (100 - deviation * 8f).toInt().coerceIn(20, 90)
            }
        }
    }

    private fun calculateNutrientScore(crop: CropType, soilData: SoilData): Int {
        val nRatio = soilData.nitrogen / crop.optimalNitrogen
        val pRatio = soilData.phosphorus / crop.optimalPhosphorus
        val kRatio = soilData.potassium / crop.optimalPotassium

        val scores = listOf(nRatio, pRatio, kRatio).map { ratio ->
            when {
                ratio >= 0.9f && ratio <= 1.2f -> 100
                ratio >= 0.7f -> 80
                ratio >= 0.5f -> 60
                ratio >= 0.3f -> 40
                else -> 20
            }
        }
        return scores.average().toInt()
    }

    private fun calculateWeatherScore(weather: WeatherData): Int {
        return when {
            weather.precipitationMm > 20f  -> 10   // Heavy rain: avoid all activity
            weather.precipitationMm > 10f  -> 40   // Moderate rain
            weather.precipitationMm > 5f   -> 60   // Light rain: might be ok
            weather.windSpeedKmh > 40f     -> 50   // High wind: seeds scattered
            else -> 100
        }
    }

    private fun scoreToStatus(score: Int): SowingStatus = when {
        score >= 80 -> SowingStatus.OPTIMAL
        score >= 65 -> SowingStatus.GOOD
        score >= 45 -> SowingStatus.FAIR
        score >= 25 -> SowingStatus.POOR
        else        -> SowingStatus.CRITICAL
    }

    private fun buildRecommendation(
        status: SowingStatus,
        crop: CropType,
        soilData: SoilData?,
        weather: WeatherData,
        moistureScore: Int,
        tempScore: Int,
        nutrientScore: Int
    ): Pair<String, String> {
        val cropName = crop.displayNameEn
        val reasons = mutableListOf<String>()

        if (soilData != null && soilData.moisturePercent > 30f) {
            reasons.add("Soil moisture ${soilData.moisturePercent.toInt()}% is too high — wait for it to drop below 30%.")
        } else if (moistureScore < 50) {
            reasons.add("Moisture level is suboptimal for $cropName.")
        }

        if (tempScore < 60) {
            reasons.add("Temperature ${weather.temperatureCelsius.toInt()}°C is outside ideal range (${crop.optimalTempMin.toInt()}–${crop.optimalTempMax.toInt()}°C).")
        }

        if (nutrientScore < 60 && soilData != null) {
            reasons.add("Soil nutrients need attention — consider fertilization.")
        }

        if (weather.precipitationMm > 10f) {
            reasons.add("Heavy rainfall expected (${weather.precipitationMm.toInt()} mm) — postpone sowing.")
        }

        val primary = when (status) {
            SowingStatus.OPTIMAL  -> "✅ Conditions are ideal! Sow your $cropName today."
            SowingStatus.GOOD     -> "👍 Good window for sowing $cropName. Proceed with care."
            SowingStatus.FAIR     -> "⚠️ Marginal conditions. Monitor for 24 hours before sowing."
            SowingStatus.POOR     -> "🛑 Conditions not favourable. Wait and reassess tomorrow."
            SowingStatus.CRITICAL -> "🚨 Critical! Do NOT sow — significant risk of crop loss."
        }

        val detail = if (reasons.isEmpty()) {
            "All agronomic parameters are within acceptable range for $cropName."
        } else {
            reasons.joinToString(" ")
        }

        return Pair(primary, detail)
    }
}

// ─────────────────────────────────────────────
//  Generate Krishi Calendar Use Case
// ─────────────────────────────────────────────

class GenerateKrishiCalendarUseCase @Inject constructor() {

    private val dateFormat = SimpleDateFormat("EEE, dd MMM", Locale.ENGLISH)

    operator fun invoke(
        forecast: List<WeatherForecastDay>,
        sowingIndex: SowingIndex,
        soilData: SoilData?,
        crop: CropType
    ): List<KrishiCalendarEntry> {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        return forecast.mapIndexed { index, day ->
            val dayLabel = when (index) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> dateFormat.format(Date(day.dateEpoch))
            }

            val hasHeavyRain = day.precipitationMm > 15f
            val hasModerateRain = day.precipitationMm in 5f..15f
            val isOptimalTemp = day.maxTemp in crop.optimalTempMin..crop.optimalTempMax
            val soilTooWet = soilData?.moisturePercent?.let { it > 30f } ?: false

            // Advance warning: if storm is predicted 3 days out, advise fertilizing 24h before
            val stormIn3Days = forecast.getOrNull(2)?.precipitationMm?.let { it > 20f } ?: false
            val stormIn4Days = forecast.getOrNull(3)?.precipitationMm?.let { it > 20f } ?: false

            val (activity, priority, desc, descKn) = when {
                hasHeavyRain -> ActivityPlan(
                    FarmActivity.AVOID_RAIN, ActivityPriority.HIGH,
                    "Heavy rain (${day.precipitationMm.toInt()} mm). Stay off the field.",
                    "ಭಾರೀ ಮಳೆ (${day.precipitationMm.toInt()} mm). ಹೊಲದಿಂದ ದೂರ ಇರಿ."
                )
                index == 1 && stormIn3Days -> ActivityPlan(
                    FarmActivity.FERTILIZE, ActivityPriority.HIGH,
                    "⚡ Fertilize TODAY – heavy storm predicted in 2 days. Apply ${crop.optimalNitrogen.toInt()} kg/ha N.",
                    "⚡ ಇಂದೇ ರಸಗೊಬ್ಬರ ಹಾಕಿ – 2 ದಿನಗಳಲ್ಲಿ ಭಾರೀ ಮಳೆ ಇದೆ."
                )
                index == 0 && sowingIndex.score >= 80 && !soilTooWet -> ActivityPlan(
                    FarmActivity.SOW, ActivityPriority.HIGH,
                    "🌟 OPTIMAL WINDOW! Sow ${crop.displayNameEn} seeds now.",
                    "🌟 ಸೂಕ್ತ ಸಮಯ! ಈಗ ${crop.displayNameKn} ಬಿತ್ತನೆ ಮಾಡಿ."
                )
                soilTooWet && index == 0 -> ActivityPlan(
                    FarmActivity.WAIT, ActivityPriority.MEDIUM,
                    "Soil moisture too high (${soilData?.moisturePercent?.toInt()}%). Wait for drainage.",
                    "ಮಣ್ಣಿನ ತೇವಾಂಶ ಹೆಚ್ಚಾಗಿದೆ. ಒಣಗಲು ಬಿಡಿ."
                )
                hasModerateRain -> ActivityPlan(
                    FarmActivity.IRRIGATE, ActivityPriority.LOW,
                    "Light rain coming. Skip irrigation — natural moisture is sufficient.",
                    "ಲಘು ಮಳೆ ಬರಲಿದೆ. ನೀರಾವರಿ ಬೇಡ."
                )
                isOptimalTemp && !hasHeavyRain && index in 1..3 -> ActivityPlan(
                    FarmActivity.SOW, ActivityPriority.MEDIUM,
                    "Conditions look good for sowing ${crop.displayNameEn}.",
                    "${crop.displayNameKn} ಬಿತ್ತನೆಗೆ ಉತ್ತಮ ಪರಿಸ್ಥಿತಿ."
                )
                index in 4..6 -> ActivityPlan(
                    FarmActivity.SOIL_PREP, ActivityPriority.LOW,
                    "Good day for soil preparation and field inspection.",
                    "ಮಣ್ಣು ತಯಾರಿ ಮತ್ತು ಹೊಲ ಪರಿಶೀಲನೆಗೆ ಉತ್ತಮ ದಿನ."
                )
                else -> ActivityPlan(
                    FarmActivity.WAIT, ActivityPriority.LOW,
                    "Monitor conditions. No critical action required today.",
                    "ಪರಿಸ್ಥಿತಿ ಗಮನಿಸಿ. ಇಂದು ವಿಶೇಷ ಕ್ರಮ ಬೇಡ."
                )
            }

            KrishiCalendarEntry(
                dateEpoch = day.dateEpoch,
                dayLabel = dayLabel,
                activity = activity,
                priority = priority,
                description = desc,
                descriptionKn = descKn,
                isOptimalWindow = activity == FarmActivity.SOW && sowingIndex.score >= 75
            )
        }
    }

    private data class ActivityPlan(
        val activity: FarmActivity,
        val priority: ActivityPriority,
        val desc: String,
        val descKn: String
    )
}
