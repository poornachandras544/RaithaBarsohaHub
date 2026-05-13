package com.raitha.bharosa.ui.screens.calendar

import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.raitha.bharosa.data.repository.*
import com.raitha.bharosa.data.simulation.DataGenerator
import com.raitha.bharosa.domain.model.*
import com.raitha.bharosa.domain.usecase.CalculateSowingIndexUseCase
import com.raitha.bharosa.domain.usecase.GenerateKrishiCalendarUseCase
import com.raitha.bharosa.ui.components.RaithaBottomNavBar
import com.raitha.bharosa.ui.navigation.Screen
import com.raitha.bharosa.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ─────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────

data class CalendarUiState(
    val isLoading: Boolean = true,
    val farmer: Farmer? = null,
    val calendarEntries: List<KrishiCalendarEntry> = emptyList(),
    val forecast: List<WeatherForecastDay> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class KrishiCalendarViewModel @Inject constructor(
    private val farmerRepository: FarmerRepository,
    private val soilDataRepository: SoilDataRepository,
    private val weatherRepository: WeatherRepository,
    private val dataGenerator: DataGenerator,
    private val calculateSowingIndex: CalculateSowingIndexUseCase,
    private val generateCalendar: GenerateKrishiCalendarUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init { loadCalendar() }

    fun loadCalendar() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val farmer = farmerRepository.getLatestFarmer().firstOrNull() ?: return@launch
            val soilData = soilDataRepository.getLatestSoilData(farmer.id).firstOrNull()
            val weatherResult = weatherRepository.getCurrentWeather(farmer.latitude, farmer.longitude)
            val weather = when (weatherResult) {
                is WeatherResult.Success          -> weatherResult.data
                is WeatherResult.SimulatedFallback -> weatherResult.data
                is WeatherResult.Error            -> dataGenerator.generateCurrentWeather()
            }
            val forecast = weatherRepository.getLiveForecast().firstOrNull() ?: emptyList()
            val sowingIndex = calculateSowingIndex(farmer.primaryCrop, soilData, weather)
            val calendar = generateCalendar(forecast, sowingIndex, soilData, farmer.primaryCrop)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    farmer = farmer,
                    calendarEntries = calendar,
                    forecast = forecast
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KrishiCalendarScreen(
    viewModel: KrishiCalendarViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.ENGLISH) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ಕೃಷಿ ಕ್ಯಾಲೆಂಡರ್", fontWeight = FontWeight.Bold)
                        Text("Krishi Calendar — 7-Day Action Plan",
                            style = MaterialTheme.typography.bodySmall.copy(color = RaithaColors.TextSecondary))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadCalendar() }) {
                        Icon(Icons.Default.Refresh, null, tint = RaithaColors.GreenPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            RaithaBottomNavBar(
                currentRoute = Screen.KrishiCalendar.route,
                onNavigate = { navController.navigate(it) }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RaithaColors.GreenPrimary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Storm warning banner ──
            val hasStorm = uiState.forecast.any { it.precipitationMm > 20f }
            if (hasStorm) {
                val stormDayIndex = uiState.forecast.indexOfFirst { it.precipitationMm > 20f }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = RaithaColors.StatusPoor.copy(alpha = 0.1f)),
                    border = BorderStroke(1.5.dp, RaithaColors.StatusPoor)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 24.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Storm Alert in $stormDayIndex Day${if (stormDayIndex > 1) "s" else ""}!",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = RaithaColors.StatusPoor
                                )
                            )
                            Text(
                                "Finish all fertilization & sowing activities at least 24 hours before.",
                                style = MaterialTheme.typography.bodySmall.copy(color = RaithaColors.TextPrimary)
                            )
                        }
                    }
                }
            }

            // ── 7-day weather strip ──
            Text(
                "7-Day Forecast",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.forecast.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.forecast.forEachIndexed { index, day ->
                        ForecastChip(
                            label = when (index) {
                                0 -> "Today"
                                1 -> "Tmrw"
                                else -> dateFormat.format(Date(day.dateEpoch))
                            },
                            maxTemp = day.maxTemp,
                            minTemp = day.minTemp,
                            precipitation = day.precipitationMm,
                            conditionCode = day.conditionCode
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Calendar entries ──
            Text(
                "Action Plan",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))

            uiState.calendarEntries.forEachIndexed { index, entry ->
                CalendarEntryCard(
                    entry = entry,
                    isToday = index == 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ForecastChip(
    label: String,
    maxTemp: Float,
    minTemp: Float,
    precipitation: Float,
    conditionCode: Int
) {
    val isRainy = precipitation > 10f
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRainy)
                RaithaColors.SkyBlue.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isRainy) BorderStroke(1.dp, RaithaColors.SkyBlue) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            Text(
                com.raitha.bharosa.ui.components.weatherEmoji(conditionCode),
                fontSize = 20.sp
            )
            Text(
                "${maxTemp.toInt()}°",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                "${minTemp.toInt()}°",
                style = MaterialTheme.typography.bodySmall.copy(color = RaithaColors.TextSecondary)
            )
            if (precipitation > 0) {
                Text(
                    "${precipitation.toInt()}mm",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = RaithaColors.SkyBlue,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun CalendarEntryCard(
    entry: KrishiCalendarEntry,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val activityColor = when (entry.activity) {
        FarmActivity.SOW          -> RaithaColors.StatusOptimal
        FarmActivity.FERTILIZE    -> RaithaColors.SunYellow
        FarmActivity.IRRIGATE     -> RaithaColors.SkyBlue
        FarmActivity.WAIT         -> RaithaColors.TextSecondary
        FarmActivity.HARVEST_PREP -> RaithaColors.GreenLight
        FarmActivity.PEST_CONTROL -> RaithaColors.SoilBrown
        FarmActivity.AVOID_RAIN   -> RaithaColors.StatusPoor
        FarmActivity.SOIL_PREP    -> RaithaColors.SoilBrown.copy(alpha = 0.7f)
    }

    val priorityIndicator = when (entry.priority) {
        ActivityPriority.HIGH   -> RaithaColors.StatusPoor
        ActivityPriority.MEDIUM -> RaithaColors.StatusFair
        ActivityPriority.LOW    -> RaithaColors.TextSecondary.copy(alpha = 0.4f)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 4.dp else 1.dp),
        border = if (isToday) BorderStroke(2.dp, RaithaColors.GreenPrimary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isOptimalWindow)
                RaithaColors.GreenSurface
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(priorityIndicator)
            )
            Spacer(Modifier.width(12.dp))

            // Activity emoji circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(activityColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.activity.emoji, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.dayLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) RaithaColors.GreenPrimary else RaithaColors.TextPrimary
                        )
                    )
                    if (isToday) {
                        Spacer(Modifier.width(6.dp))
                        Badge(containerColor = RaithaColors.GreenPrimary) {
                            Text("TODAY", style = MaterialTheme.typography.labelLarge.copy(fontSize = 9.sp))
                        }
                    }
                    if (entry.isOptimalWindow) {
                        Spacer(Modifier.width(6.dp))
                        Badge(containerColor = RaithaColors.SunYellow) {
                            Text("⭐ OPTIMAL", style = MaterialTheme.typography.labelLarge.copy(fontSize = 9.sp))
                        }
                    }
                }
                Text(
                    text = entry.activity.labelEn,
                    style = MaterialTheme.typography.labelLarge.copy(color = activityColor)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall.copy(color = RaithaColors.TextSecondary)
                )
            }
        }
    }
}
