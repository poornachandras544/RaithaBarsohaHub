package com.raitha.bharosa.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raitha.bharosa.domain.model.*
import com.raitha.bharosa.ui.theme.*

// ─────────────────────────────────────────────
//  Sowing Index Gauge
// ─────────────────────────────────────────────

/**
 * Circular arc gauge displaying the composite Sowing Index (0-100).
 * Color transitions from red → yellow → green based on score.
 */
@Composable
fun SowingIndexGauge(
    score: Int,
    status: SowingStatus,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "sowingIndex"
    )

    val gaugeColor = sowingStatusColor(animatedScore)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Arc background
        androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 18.dp.toPx()
            val diameter = size.toPx() - strokeWidth
            val radius = diameter / 2f
            val cx = size.toPx() / 2f
            val cy = size.toPx() / 2f

            // Background arc (grey track)
            drawArc(
                color = Color(0xFFE0E0E0),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Filled arc
            val sweep = (animatedScore / 100f) * 270f
            drawArc(
                color = gaugeColor,
                startAngle = 135f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$animatedScore",
                style = MaterialTheme.typography.displayLarge.copy(
                    color = gaugeColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 52.sp
                )
            )
            Text(
                text = "Sowing Index",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = RaithaColors.TextSecondary
                )
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Status Banner
// ─────────────────────────────────────────────

@Composable
fun StatusBanner(
    status: SowingStatus,
    primaryRecommendation: String,
    modifier: Modifier = Modifier
) {
    val bgColor = sowingStatusColor(
        when (status) {
            SowingStatus.OPTIMAL  -> 90
            SowingStatus.GOOD     -> 75
            SowingStatus.FAIR     -> 55
            SowingStatus.POOR     -> 35
            SowingStatus.CRITICAL -> 10
        }
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(2.dp, bgColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = status.labelEn,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = bgColor,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = primaryRecommendation,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = RaithaColors.TextPrimary
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Parameter Tile
// ─────────────────────────────────────────────

@Composable
fun ParameterTile(
    label: String,
    value: String,
    unit: String,
    score: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val color = sowingStatusColor(score)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = RaithaColors.TextPrimary
                )
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = RaithaColors.TextSecondary,
                    fontSize = 11.sp
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = RaithaColors.TextSecondary,
                    fontSize = 11.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Weather Summary Card
// ─────────────────────────────────────────────

@Composable
fun WeatherSummaryCard(
    weather: WeatherData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RaithaColors.SkyBlue.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = weatherEmoji(weather.conditionCode),
                fontSize = 40.sp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${weather.temperatureCelsius.toInt()}°C  •  ${weather.conditionDescription}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Humidity ${weather.humidity.toInt()}%  •  Wind ${weather.windSpeedKmh.toInt()} km/h",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = RaithaColors.TextSecondary
                    )
                )
                if (weather.precipitationMm > 0) {
                    Text(
                        text = "🌧 Rain: ${weather.precipitationMm.toInt()} mm",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = RaithaColors.SkyBlue
                        )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Bottom Navigation Bar
// ─────────────────────────────────────────────

@Composable
fun RaithaBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(com.raitha.bharosa.ui.navigation.Screen.Dashboard.route,      "Dashboard",  Icons.Default.Home),
            Triple(com.raitha.bharosa.ui.navigation.Screen.InputCenter.route,    "Field Data", Icons.Default.Science),
            Triple(com.raitha.bharosa.ui.navigation.Screen.KrishiCalendar.route, "Calendar",   Icons.Default.CalendarToday),
            Triple(com.raitha.bharosa.ui.navigation.Screen.History.route,        "History",    Icons.Default.History)
        )

        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { onNavigate(route) },
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RaithaColors.GreenPrimary,
                    selectedTextColor = RaithaColors.GreenPrimary,
                    indicatorColor = RaithaColors.GreenSurface
                )
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────

fun weatherEmoji(conditionCode: Int): String = when (conditionCode) {
    in 200..232 -> "⛈️"
    in 300..321 -> "🌦️"
    in 500..531 -> "🌧️"
    in 600..622 -> "❄️"
    in 700..781 -> "🌫️"
    800         -> "☀️"
    801         -> "🌤️"
    in 802..804 -> "☁️"
    else        -> "🌡️"
}
