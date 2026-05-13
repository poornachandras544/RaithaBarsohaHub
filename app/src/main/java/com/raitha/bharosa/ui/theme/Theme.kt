package com.raitha.bharosa.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
//  Brand Colors
// ─────────────────────────────────────────────

object RaithaColors {
    // Primary greens (agriculture)
    val GreenPrimary = Color(0xFF2E7D32)
    val GreenLight   = Color(0xFF4CAF50)
    val GreenSurface = Color(0xFFE8F5E9)
    val GreenAccent  = Color(0xFF81C784)

    // Status colors matching spec
    val StatusOptimal  = Color(0xFF2E7D32)
    val StatusGood     = Color(0xFF558B2F)
    val StatusFair     = Color(0xFFF57F17)
    val StatusPoor     = Color(0xFFB71C1C)
    val StatusCritical = Color(0xFF880E4F)

    // Earth tones
    val SoilBrown    = Color(0xFF5D4037)
    val SkyBlue      = Color(0xFF1565C0)
    val SunYellow    = Color(0xFFF9A825)
    val WaterBlue    = Color(0xFF0288D1)

    // Neutrals
    val Background   = Color(0xFFF1F8E9)
    val Surface      = Color(0xFFFFFFFF)
    val OnPrimary    = Color(0xFFFFFFFF)
    val TextPrimary  = Color(0xFF1B2B1E)
    val TextSecondary = Color(0xFF4A6741)
}

// ─────────────────────────────────────────────
//  Color Scheme
// ─────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary          = RaithaColors.GreenPrimary,
    onPrimary        = RaithaColors.OnPrimary,
    primaryContainer = RaithaColors.GreenSurface,
    onPrimaryContainer = RaithaColors.TextPrimary,
    secondary        = RaithaColors.SoilBrown,
    tertiary         = RaithaColors.SkyBlue,
    background       = RaithaColors.Background,
    surface          = RaithaColors.Surface,
    onBackground     = RaithaColors.TextPrimary,
    onSurface        = RaithaColors.TextPrimary,
    error            = RaithaColors.StatusPoor
)

// ─────────────────────────────────────────────
//  Typography
// ─────────────────────────────────────────────

val RaithaTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 0.15.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    )
)

// ─────────────────────────────────────────────
//  App Theme
// ─────────────────────────────────────────────

@Composable
fun RaithaBharosaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = RaithaTypography,
        content = content
    )
}

// ─────────────────────────────────────────────
//  Status Color Helpers
// ─────────────────────────────────────────────

fun sowingStatusColor(score: Int): Color = when {
    score >= 80 -> RaithaColors.StatusOptimal
    score >= 65 -> RaithaColors.StatusGood
    score >= 45 -> RaithaColors.StatusFair
    score >= 25 -> RaithaColors.StatusPoor
    else        -> RaithaColors.StatusCritical
}
