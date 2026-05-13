package com.raitha.bharosa.ui.screens.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.raitha.bharosa.data.local.entity.SimulatedSnapshotEntity
import com.raitha.bharosa.data.repository.*
import com.raitha.bharosa.data.simulation.DataGenerator
import com.raitha.bharosa.domain.model.*
import com.raitha.bharosa.domain.usecase.CalculateSowingIndexUseCase
import com.raitha.bharosa.domain.usecase.GenerateKrishiCalendarUseCase
import com.raitha.bharosa.ui.components.*
import com.raitha.bharosa.ui.navigation.Screen
import com.raitha.bharosa.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ─────────────────────────────────────────────
//  UI State
// ─────────────────────────────────────────────

data class DashboardUiState(
    val isLoading: Boolean = true,
    val farmer: Farmer? = null,
    val weather: WeatherData? = null,
    val soilData: SoilData? = null,
    val sowingIndex: SowingIndex? = null,
    val forecast: List<WeatherForecastDay> = emptyList(),
    val calendar: List<KrishiCalendarEntry> = emptyList(),
    val isWeatherSimulated: Boolean = false,
    val lastRefreshed: Long = 0L,
    val error: String? = null
)

// ─────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val farmerRepository: FarmerRepository,
    private val soilDataRepository: SoilDataRepository,
    private val weatherRepository: WeatherRepository,
    private val dataGenerator: DataGenerator,
    private val calculateSowingIndex: CalculateSowingIndexUseCase,
    private val generateCalendar: GenerateKrishiCalendarUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        loadDashboard()
        startAutoRefresh()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 1. Get farmer profile
            val farmer = farmerRepository.getLatestFarmer().firstOrNull()
            if (farmer == null) {
                _uiState.update { it.copy(isLoading = false, error = "No farmer profile found") }
                return@launch
            }

            // 2. Get/generate soil data
            val soilData = soilDataRepository.getLatestSoilData(farmer.id).firstOrNull()
                ?: soilDataRepository.generateAndSaveSimulatedData(farmer.id, farmer.primaryCrop)

            // 3. Get weather (API or simulation fallback)
            val weatherResult = weatherRepository.getCurrentWeather(farmer.latitude, farmer.longitude)
            val (weather, isSimulated) = when (weatherResult) {
                is WeatherResult.Success -> Pair(weatherResult.data, false)
                is WeatherResult.SimulatedFallback -> Pair(weatherResult.data, true)
                is WeatherResult.Error -> Pair(dataGenerator.generateCurrentWeather(), true)
            }

            // 4. Get 7-day forecast
            val forecast = weatherRepository.getLiveForecast().firstOrNull() ?: emptyList()

            // 5. Calculate sowing index
            val sowingIndex = calculateSowingIndex(farmer.primaryCrop, soilData, weather)

            // 6. Generate Krishi Calendar
            val calendar = generateCalendar(forecast, sowingIndex, soilData, farmer.primaryCrop)

            // 7. Persist snapshot
            weatherRepository.saveSnapshot(
                SimulatedSnapshotEntity(
                    farmerId = farmer.id,
                    temperature = weather.temperatureCelsius,
                    humidity = weather.humidity,
                    moisturePercent = soilData.moisturePercent,
                    windSpeedKmh = weather.windSpeedKmh,
                    precipitationMm = weather.precipitationMm,
                    conditionCode = weather.conditionCode,
                    conditionDescription = weather.conditionDescription,
                    sowingIndexScore = sowingIndex.score,
                    sowingStatus = sowingIndex.status.name
                )
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    farmer = farmer,
                    weather = weather,
                    soilData = soilData,
                    sowingIndex = sowingIndex,
                    forecast = forecast,
                    calendar = calendar,
                    isWeatherSimulated = isSimulated,
                    lastRefreshed = System.currentTimeMillis()
                )
            }
        }
    }

    fun updateCrop(newCrop: CropType) {
        viewModelScope.launch {
            val currentFarmer = _uiState.value.farmer ?: return@launch
            val updatedFarmer = currentFarmer.copy(primaryCrop = newCrop)
            farmerRepository.saveFarmer(updatedFarmer)
            loadDashboard() // Reload with new crop logic
        }
    }

    /** Refreshes simulated sensor data — re-generates random values to show dynamism */
    fun refreshSimulatedData() {
        viewModelScope.launch {
            val farmer = _uiState.value.farmer ?: return@launch
            val crop = farmer.primaryCrop

            val newSoil = soilDataRepository.generateAndSaveSimulatedData(farmer.id, crop)
            val newWeather = dataGenerator.generateCurrentWeather()
            val forecast = weatherRepository.getLiveForecast().firstOrNull() ?: emptyList()
            val sowingIndex = calculateSowingIndex(crop, newSoil, newWeather)
            val calendar = generateCalendar(forecast, sowingIndex, newSoil, crop)

            _uiState.update {
                it.copy(
                    soilData = newSoil,
                    weather = newWeather,
                    sowingIndex = sowingIndex,
                    forecast = forecast,
                    calendar = calendar,
                    isWeatherSimulated = true,
                    lastRefreshed = System.currentTimeMillis()
                )
            }
        }
    }

    /** Auto-refresh every 5 minutes to simulate live sensor updates */
    private fun startAutoRefresh() {
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000L)
                refreshSimulatedData()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}

// ─────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    navController: NavHostController,
    onNavigateToInputCenter: () -> Unit,
    onNavigateToCalendar: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    var showCropDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.farmer?.let {
                                "ನಮಸ್ಕಾರ, ${it.name} 👋"
                            } ?: "Raitha-Bharosa Hub",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        uiState.farmer?.let {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showCropDialog = true }
                            ) {
                                Text(
                                    text = "${it.primaryCrop.iconEmoji} ${it.primaryCrop.displayNameEn}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = RaithaColors.TextSecondary
                                    )
                                )
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Crop",
                                    modifier = Modifier.size(14.dp).padding(start = 4.dp),
                                    tint = RaithaColors.GreenPrimary
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        navController.navigate(Screen.Onboarding.route) 
                    }) {
                        Icon(Icons.Default.AccountCircle, "Switch Profile", tint = RaithaColors.GreenPrimary)
                    }
                    IconButton(onClick = { viewModel.refreshSimulatedData() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = RaithaColors.GreenPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            RaithaBottomNavBar(
                currentRoute = Screen.Dashboard.route,
                onNavigate = { route -> navController.navigate(route) }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = RaithaColors.GreenPrimary)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading field data...", color = RaithaColors.TextSecondary)
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Simulated data badge ──
            if (uiState.isWeatherSimulated) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = RaithaColors.SunYellow.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = RaithaColors.SunYellow, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Using simulated data — Add API key for live weather",
                            style = MaterialTheme.typography.bodySmall.copy(color = RaithaColors.SoilBrown)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Sowing Index Gauge ──
            uiState.sowingIndex?.let { idx ->
                SowingIndexGauge(
                    score = idx.score,
                    status = idx.status,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Crop Health Velocity",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = RaithaColors.TextSecondary
                    )
                )

                Spacer(Modifier.height(12.dp))

                // ── Status Banner ──
                StatusBanner(
                    status = idx.status,
                    primaryRecommendation = idx.primaryRecommendation
                )

                Spacer(Modifier.height(8.dp))

                // ── Detailed reason ──
                if (idx.detailedReason.isNotBlank()) {
                    Text(
                        text = idx.detailedReason,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = RaithaColors.TextSecondary
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Weather Card ──
            uiState.weather?.let { weather ->
                Text(
                    "Current Weather",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                WeatherSummaryCard(weather = weather)
            }

            Spacer(Modifier.height(16.dp))

            // ── 4-Parameter Sub-scores ──
            uiState.sowingIndex?.let { idx ->
                Text(
                    "Parameter Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ParameterTile(
                        label = "Moisture",
                        value = uiState.soilData?.moisturePercent?.let { "%.0f%%".format(it) } ?: "--",
                        unit = "soil %",
                        score = idx.moistureScore,
                        icon = Icons.Default.WaterDrop,
                        modifier = Modifier.weight(1f)
                    )
                    ParameterTile(
                        label = "Temperature",
                        value = uiState.weather?.temperatureCelsius?.let { "${it.toInt()}°C" } ?: "--",
                        unit = "ambient",
                        score = idx.temperatureScore,
                        icon = Icons.Default.Thermostat,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ParameterTile(
                        label = "Nutrients",
                        value = "${idx.nutrientScore}%",
                        unit = "N-P-K score",
                        score = idx.nutrientScore,
                        icon = Icons.Default.Science,
                        modifier = Modifier.weight(1f)
                    )
                    ParameterTile(
                        label = "Weather",
                        value = "${idx.weatherScore}%",
                        unit = "conditions",
                        score = idx.weatherScore,
                        icon = Icons.Default.Cloud,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Soil NPK Summary ──
            uiState.soilData?.let { soil ->
                val crop = uiState.farmer?.primaryCrop
                Text(
                    "Soil Nutrients (kg/ha)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        NPKBadge("N", soil.nitrogen, crop?.optimalNitrogen)
                        NPKBadge("P", soil.phosphorus, crop?.optimalPhosphorus)
                        NPKBadge("K", soil.potassium, crop?.optimalPotassium)
                        NPKBadge("pH", soil.phLevel, 6.5f, isDecimal = true)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Quick Actions ──
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToInputCenter,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, RaithaColors.GreenPrimary)
                ) {
                    Icon(Icons.Default.Science, null, tint = RaithaColors.GreenPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text("Log Soil Data", color = RaithaColors.GreenPrimary)
                }
                OutlinedButton(
                    onClick = onNavigateToCalendar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, RaithaColors.SkyBlue)
                ) {
                    Icon(Icons.Default.CalendarToday, null, tint = RaithaColors.SkyBlue)
                    Spacer(Modifier.width(4.dp))
                    Text("Krishi Calendar", color = RaithaColors.SkyBlue)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Last refreshed
            uiState.lastRefreshed.takeIf { it > 0 }?.let {
                Text(
                    text = "Last updated: ${timeFormat.format(Date(it))}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = RaithaColors.TextSecondary.copy(alpha = 0.6f)
                    )
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCropDialog) {
        AlertDialog(
            onDismissRequest = { showCropDialog = false },
            title = { Text("Change Primary Crop", fontWeight = FontWeight.Bold) },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.heightIn(max = 400.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CropType.entries) { crop ->
                        val isSelected = uiState.farmer?.primaryCrop == crop
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    viewModel.updateCrop(crop)
                                    showCropDialog = false 
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) BorderStroke(2.dp, RaithaColors.GreenPrimary) else null,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) RaithaColors.GreenSurface
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(crop.iconEmoji, fontSize = 28.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = crop.displayNameEn,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) RaithaColors.GreenPrimary else RaithaColors.TextPrimary
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCropDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun NPKBadge(label: String, value: Float, optimal: Float?, isDecimal: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (isDecimal) "%.1f".format(value) else value.toInt().toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        if (optimal != null) {
            Text(
                text = "/${if (isDecimal) "%.1f".format(optimal) else optimal.toInt()}",
                style = MaterialTheme.typography.bodySmall.copy(color = RaithaColors.TextSecondary)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                color = RaithaColors.GreenPrimary,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
