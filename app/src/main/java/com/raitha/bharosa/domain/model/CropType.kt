package com.raitha.bharosa.domain.model

/**
 * Supported crops with their optimal agronomic parameters.
 *
 * @param displayNameEn  English display name
 * @param displayNameKn  Kannada display name
 * @param optimalMoistureMin  Minimum optimal soil moisture %
 * @param optimalMoistureMax  Maximum optimal soil moisture %
 * @param optimalTempMin  Minimum optimal temperature °C
 * @param optimalTempMax  Maximum optimal temperature °C
 * @param optimalNitrogen  Ideal nitrogen level (kg/ha)
 * @param optimalPhosphorus  Ideal phosphorus level (kg/ha)
 * @param optimalPotassium  Ideal potassium level (kg/ha)
 * @param sowingWindowDays  Typical duration of the sowing season in days
 */
enum class CropType(
    val displayNameEn: String,
    val displayNameKn: String,
    val optimalMoistureMin: Float,
    val optimalMoistureMax: Float,
    val optimalTempMin: Float,
    val optimalTempMax: Float,
    val optimalNitrogen: Float,
    val optimalPhosphorus: Float,
    val optimalPotassium: Float,
    val sowingWindowDays: Int,
    val iconEmoji: String
) {
    PADDY(
        displayNameEn = "Paddy (Rice)",
        displayNameKn = "ಭತ್ತ",
        optimalMoistureMin = 25f, optimalMoistureMax = 35f,
        optimalTempMin = 20f, optimalTempMax = 35f,
        optimalNitrogen = 120f, optimalPhosphorus = 60f, optimalPotassium = 60f,
        sowingWindowDays = 14,
        iconEmoji = "🌾"
    ),
    SUGARCANE(
        displayNameEn = "Sugarcane",
        displayNameKn = "ಕಬ್ಬು",
        optimalMoistureMin = 20f, optimalMoistureMax = 30f,
        optimalTempMin = 25f, optimalTempMax = 38f,
        optimalNitrogen = 250f, optimalPhosphorus = 80f, optimalPotassium = 120f,
        sowingWindowDays = 21,
        iconEmoji = "🎋"
    ),
    RAGI(
        displayNameEn = "Ragi (Finger Millet)",
        displayNameKn = "ರಾಗಿ",
        optimalMoistureMin = 15f, optimalMoistureMax = 25f,
        optimalTempMin = 18f, optimalTempMax = 30f,
        optimalNitrogen = 50f, optimalPhosphorus = 25f, optimalPotassium = 25f,
        sowingWindowDays = 10,
        iconEmoji = "🌿"
    ),
    MAIZE(
        displayNameEn = "Maize (Corn)",
        displayNameKn = "ಮೆಕ್ಕೆ ಜೋಳ",
        optimalMoistureMin = 20f, optimalMoistureMax = 30f,
        optimalTempMin = 21f, optimalTempMax = 32f,
        optimalNitrogen = 150f, optimalPhosphorus = 75f, optimalPotassium = 75f,
        sowingWindowDays = 12,
        iconEmoji = "🌽"
    ),
    COTTON(
        displayNameEn = "Cotton",
        displayNameKn = "ಹತ್ತಿ",
        optimalMoistureMin = 18f, optimalMoistureMax = 28f,
        optimalTempMin = 25f, optimalTempMax = 40f,
        optimalNitrogen = 100f, optimalPhosphorus = 50f, optimalPotassium = 50f,
        sowingWindowDays = 14,
        iconEmoji = "🌱"
    ),
    GROUNDNUT(
        displayNameEn = "Groundnut",
        displayNameKn = "ಕಡಲೆಕಾಯಿ",
        optimalMoistureMin = 20f, optimalMoistureMax = 30f,
        optimalTempMin = 25f, optimalTempMax = 35f,
        optimalNitrogen = 20f, optimalPhosphorus = 40f, optimalPotassium = 40f,
        sowingWindowDays = 10,
        iconEmoji = "🥜"
    ),
    SOYBEAN(
        displayNameEn = "Soybean",
        displayNameKn = "ಸೋಯಾಬೀನ್",
        optimalMoistureMin = 22f, optimalMoistureMax = 32f,
        optimalTempMin = 20f, optimalTempMax = 32f,
        optimalNitrogen = 40f, optimalPhosphorus = 60f, optimalPotassium = 40f,
        sowingWindowDays = 14,
        iconEmoji = "🫘"
    ),
    TOMATO(
        displayNameEn = "Tomato",
        displayNameKn = "ಟೊಮೇಟೊ",
        optimalMoistureMin = 25f, optimalMoistureMax = 35f,
        optimalTempMin = 18f, optimalTempMax = 28f,
        optimalNitrogen = 120f, optimalPhosphorus = 100f, optimalPotassium = 150f,
        sowingWindowDays = 21,
        iconEmoji = "🍅"
    )
}
