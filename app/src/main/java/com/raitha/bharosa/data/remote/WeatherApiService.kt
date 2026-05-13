package com.raitha.bharosa.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// ─────────────────────────────────────────────
//  API Service Interface
// ─────────────────────────────────────────────

interface WeatherApiService {

    /**
     * Current weather by geographic coordinates.
     * Endpoint: GET /weather
     */
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Response<CurrentWeatherResponse>

    /**
     * 5-day / 3-hour forecast.
     * Endpoint: GET /forecast
     */
    @GET("forecast")
    suspend fun getWeatherForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("cnt") count: Int = 40   // 5 days × 8 readings/day
    ): Response<ForecastResponse>
}

// ─────────────────────────────────────────────
//  Current Weather Response DTOs
// ─────────────────────────────────────────────

data class CurrentWeatherResponse(
    @SerializedName("weather") val weather: List<WeatherConditionDto>,
    @SerializedName("main") val main: MainDto,
    @SerializedName("wind") val wind: WindDto,
    @SerializedName("rain") val rain: RainDto?,
    @SerializedName("clouds") val clouds: CloudsDto,
    @SerializedName("dt") val timestamp: Long,
    @SerializedName("name") val cityName: String
)

data class WeatherConditionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("main") val main: String,
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String
)

data class MainDto(
    @SerializedName("temp") val temp: Float,
    @SerializedName("feels_like") val feelsLike: Float,
    @SerializedName("temp_min") val tempMin: Float,
    @SerializedName("temp_max") val tempMax: Float,
    @SerializedName("humidity") val humidity: Float
)

data class WindDto(
    @SerializedName("speed") val speed: Float,     // m/s → convert to km/h × 3.6
    @SerializedName("deg") val direction: Int? = null
)

data class RainDto(
    @SerializedName("1h") val oneHour: Float? = null,
    @SerializedName("3h") val threeHour: Float? = null
)

data class CloudsDto(
    @SerializedName("all") val cloudCoverPercent: Int
)

// ─────────────────────────────────────────────
//  Forecast Response DTOs
// ─────────────────────────────────────────────

data class ForecastResponse(
    @SerializedName("list") val forecastList: List<ForecastItemDto>,
    @SerializedName("city") val city: CityDto
)

data class ForecastItemDto(
    @SerializedName("dt") val timestamp: Long,
    @SerializedName("main") val main: MainDto,
    @SerializedName("weather") val weather: List<WeatherConditionDto>,
    @SerializedName("wind") val wind: WindDto,
    @SerializedName("rain") val rain: RainDto?,
    @SerializedName("pop") val precipitationProbability: Float = 0f,
    @SerializedName("dt_txt") val dateText: String
)

data class CityDto(
    @SerializedName("name") val name: String,
    @SerializedName("country") val country: String,
    @SerializedName("coord") val coord: CoordDto
)

data class CoordDto(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double
)
