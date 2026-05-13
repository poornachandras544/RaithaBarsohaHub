package com.raitha.bharosa.data.simulation

import com.raitha.bharosa.domain.model.CropType
import com.raitha.bharosa.domain.model.WeatherData
import com.raitha.bharosa.domain.model.WeatherForecastDay
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * DataGenerator — Simulates realistic agronomic sensor and weather data.
 *
 * Generates values that follow Karnataka's seasonal patterns (Kharif/Rabi)
 * and responds dynamically to time-of-day and random variation.
 */
@Singleton
class DataGenerator @Inject constructor() {

    private val random = Random.Default

    // ─────────────────────────────────────────────
    //  Soil Moisture Simulation
    // ─────────────────────────────────────────────

    /**
     * Generates a random soil moisture % between 10% and 40%.
     * Per spec: if (moisture > 30%) → "Soil too wet to sow"
     */
    fun generateMoisturePercent(): Float {
        return random.nextFloat() * 30f + 10f  // range: 10..40
    }

    /**
     * Generates moisture level influenced by season and recent rainfall.
     * Returns value in range 5-45%.
     */
    fun generateSeasonalMoisture(precipitationMm: Float): Float {
        val baseMoisture = random.nextFloat() * 20f + 15f   // 15-35%
        val rainBonus = (precipitationMm * 0.4f).coerceAtMost(15f)
        return (baseMoisture + rainBonus).coerceIn(5f, 45f)
    }

    // ─────────────────────────────────────────────
    //  Temperature Simulation
    // ─────────────────────────────────────────────

    /**
     * Generates a temperature in °C typical for Karnataka's climate,
     * varying by season (month) and time of day.
     */
    fun generateTemperatureCelsius(): Float {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val baseTemp = when (month) {
            in 3..5  -> 32f   // Summer: Mar-May
            in 6..9  -> 26f   // Monsoon: Jun-Sep (Kharif)
            in 10..11 -> 28f  // Post-monsoon: Oct-Nov
            else     -> 22f   // Winter: Dec-Feb (Rabi)
        }

        // Diurnal variation: cooler at night, peak at ~2pm
        val diurnalShift = 5f * sin((hour - 6) * Math.PI.toFloat() / 12f)
        val noise = random.nextFloat() * 2f - 1f  // ±1°C noise

        return (baseTemp + diurnalShift + noise).coerceIn(15f, 42f)
    }

    // ─────────────────────────────────────────────
    //  NPK Simulation
    // ─────────────────────────────────────────────

    fun generateNitrogenLevel(crop: CropType): Float {
        // Simulate deficiency: return 60-110% of optimal
        val ratio = 0.6f + random.nextFloat() * 0.5f
        return crop.optimalNitrogen * ratio
    }

    fun generatePhosphorusLevel(crop: CropType): Float {
        val ratio = 0.5f + random.nextFloat() * 0.6f
        return crop.optimalPhosphorus * ratio
    }

    fun generatePotassiumLevel(crop: CropType): Float {
        val ratio = 0.7f + random.nextFloat() * 0.4f
        return crop.optimalPotassium * ratio
    }

    // ─────────────────────────────────────────────
    //  Full Weather Snapshot
    // ─────────────────────────────────────────────

    fun generateCurrentWeather(): WeatherData {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        val isMonsooon = month in 6..9

        val temp = generateTemperatureCelsius()
        val humidity = if (isMonsooon) {
            random.nextFloat() * 30f + 60f  // 60-90%
        } else {
            random.nextFloat() * 40f + 30f  // 30-70%
        }

        val precipitation = if (isMonsooon && random.nextFloat() > 0.6f) {
            random.nextFloat() * 25f  // 0-25mm during monsoon
        } else if (!isMonsooon && random.nextFloat() > 0.9f) {
            random.nextFloat() * 5f   // sporadic rain off-season
        } else {
            0f
        }

        val conditionCode = when {
            precipitation > 15f -> 502   // Heavy rain
            precipitation > 5f  -> 501   // Moderate rain
            precipitation > 0f  -> 300   // Light drizzle
            humidity > 80f      -> 801   // Few clouds
            else                -> 800   // Clear sky
        }

        val conditionDesc = when (conditionCode) {
            502 -> "Heavy Rain"
            501 -> "Moderate Rain"
            300 -> "Light Drizzle"
            801 -> "Partly Cloudy"
            else -> "Clear Sky"
        }

        return WeatherData(
            temperatureCelsius = temp,
            humidity = humidity,
            windSpeedKmh = random.nextFloat() * 20f + 2f,
            precipitationMm = precipitation,
            conditionCode = conditionCode,
            conditionDescription = conditionDesc,
            feelsLike = temp - (humidity / 100f) * 3f
        )
    }

    // ─────────────────────────────────────────────
    //  7-Day Forecast
    // ─────────────────────────────────────────────

    fun generate7DayForecast(): List<WeatherForecastDay> {
        val today = Calendar.getInstance()
        return (0..6).map { dayOffset ->
            val cal = today.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, dayOffset)

            val baseWeather = generateCurrentWeather()
            val isStormDay = dayOffset == 2 || dayOffset == 3  // simulate storm on day 3-4

            WeatherForecastDay(
                dateEpoch = cal.timeInMillis,
                maxTemp = baseWeather.temperatureCelsius + random.nextFloat() * 4f,
                minTemp = baseWeather.temperatureCelsius - random.nextFloat() * 6f,
                precipitationMm = if (isStormDay) {
                    random.nextFloat() * 40f + 20f
                } else {
                    baseWeather.precipitationMm
                },
                humidity = if (isStormDay) {
                    (baseWeather.humidity + 20f).coerceAtMost(100f)
                } else {
                    baseWeather.humidity
                },
                conditionDescription = if (isStormDay) "Heavy Storm" else baseWeather.conditionDescription,
                conditionCode = if (isStormDay) 502 else baseWeather.conditionCode
            )
        }
    }

    // ─────────────────────────────────────────────
    //  Soil pH
    // ─────────────────────────────────────────────

    fun generatePhLevel(): Float = 5.5f + random.nextFloat() * 2.5f  // 5.5 - 8.0

    fun generateOrganicCarbon(): Float = 0.2f + random.nextFloat() * 1.3f  // 0.2 - 1.5%
}
