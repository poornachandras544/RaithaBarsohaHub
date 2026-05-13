package com.raitha.bharosa.ui.screens.inputcenter

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.raitha.bharosa.data.repository.FarmerRepository
import com.raitha.bharosa.data.repository.SoilDataRepository
import com.raitha.bharosa.data.simulation.DataGenerator
import com.raitha.bharosa.domain.model.*
import com.raitha.bharosa.ui.components.RaithaBottomNavBar
import com.raitha.bharosa.ui.navigation.Screen
import com.raitha.bharosa.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────

data class InputCenterUiState(
    val isLoading: Boolean = true,
    val farmer: Farmer? = null,
    val latestSoilData: SoilData? = null,
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class InputCenterViewModel @Inject constructor(
    private val farmerRepository: FarmerRepository,
    private val soilDataRepository: SoilDataRepository,
    private val dataGenerator: DataGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(InputCenterUiState())
    val uiState: StateFlow<InputCenterUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            farmerRepository.getLatestFarmer().collect { farmer ->
                if (farmer == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@collect
                }
                soilDataRepository.getLatestSoilData(farmer.id).collect { soilData ->
                    _uiState.update {
                        it.copy(isLoading = false, farmer = farmer, latestSoilData = soilData)
                    }
                }
            }
        }
    }

    fun saveSoilData(
        nitrogen: String, phosphorus: String, potassium: String,
        moisture: String, phLevel: String
    ) {
        val farmerId = _uiState.value.farmer?.id ?: return

        val n = nitrogen.toFloatOrNull() ?: run {
            _uiState.update { it.copy(error = "Invalid Nitrogen value") }; return
        }
        val p = phosphorus.toFloatOrNull() ?: run {
            _uiState.update { it.copy(error = "Invalid Phosphorus value") }; return
        }
        val k = potassium.toFloatOrNull() ?: run {
            _uiState.update { it.copy(error = "Invalid Potassium value") }; return
        }
        val m = moisture.toFloatOrNull() ?: run {
            _uiState.update { it.copy(error = "Invalid Moisture value") }; return
        }
        val ph = phLevel.toFloatOrNull() ?: run {
            _uiState.update { it.copy(error = "Invalid pH value") }; return
        }

        if (m > 100 || m < 0) {
            _uiState.update { it.copy(error = "Moisture must be between 0-100%") }; return
        }
        if (ph < 0 || ph > 14) {
            _uiState.update { it.copy(error = "pH must be between 0-14") }; return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val data = SoilData(
                farmerId = farmerId,
                nitrogen = n, phosphorus = p, potassium = k,
                moisturePercent = m, phLevel = ph
            )
            soilDataRepository.saveSoilData(data)
            _uiState.update { it.copy(isSaving = false, savedSuccess = true) }
        }
    }

    fun generateSimulatedData() {
        val farmer = _uiState.value.farmer ?: return
        viewModelScope.launch {
            val data = soilDataRepository.generateAndSaveSimulatedData(farmer.id, farmer.primaryCrop)
            _uiState.update { it.copy(latestSoilData = data, savedSuccess = true) }
        }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(savedSuccess = false) }
    }
}

// ─────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputCenterScreen(
    viewModel: InputCenterViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Pre-fill with latest data
    val latest = uiState.latestSoilData
    var nitrogen    by remember(latest) { mutableStateOf(latest?.nitrogen?.toInt()?.toString() ?: "") }
    var phosphorus  by remember(latest) { mutableStateOf(latest?.phosphorus?.toInt()?.toString() ?: "") }
    var potassium   by remember(latest) { mutableStateOf(latest?.potassium?.toInt()?.toString() ?: "") }
    var moisture    by remember(latest) { mutableStateOf(latest?.moisturePercent?.toInt()?.toString() ?: "") }
    var phLevel     by remember(latest) { mutableStateOf(latest?.phLevel?.let { "%.1f".format(it) } ?: "6.5") }

    LaunchedEffect(uiState.savedSuccess) {
        if (uiState.savedSuccess) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Field Data Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            RaithaBottomNavBar(
                currentRoute = Screen.InputCenter.route,
                onNavigate = { navController.navigate(it) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Current crop context
            uiState.farmer?.let { farmer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RaithaColors.GreenSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(farmer.primaryCrop.iconEmoji, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Logging for: ${farmer.primaryCrop.displayNameEn}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Optimal N:${farmer.primaryCrop.optimalNitrogen.toInt()} | P:${farmer.primaryCrop.optimalPhosphorus.toInt()} | K:${farmer.primaryCrop.optimalPotassium.toInt()} kg/ha",
                                style = MaterialTheme.typography.bodySmall.copy(color = RaithaColors.TextSecondary)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Moisture status chip (spec requirement) ──
            latest?.let { soil ->
                val isWet = soil.moisturePercent > 30f
                val isBad = soil.moisturePercent < 10f
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isWet -> RaithaColors.StatusPoor.copy(alpha = 0.1f)
                            isBad -> RaithaColors.StatusFair.copy(alpha = 0.1f)
                            else  -> RaithaColors.StatusOptimal.copy(alpha = 0.1f)
                        }
                    )
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.WaterDrop,
                            null,
                            tint = when {
                                isWet -> RaithaColors.StatusPoor
                                isBad -> RaithaColors.StatusFair
                                else  -> RaithaColors.StatusOptimal
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                isWet -> "⚠️ Soil moisture ${soil.moisturePercent.toInt()}% — Soil too wet to sow"
                                isBad -> "⚠️ Soil moisture ${soil.moisturePercent.toInt()}% — Too dry, irrigate field"
                                else  -> "✅ Soil moisture ${soil.moisturePercent.toInt()}% — Optimal for sowing"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Text(
                "Manual Soil Test Entry",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))

            @Composable
            fun SoilField(label: String, value: String, onChange: (String) -> Unit, unit: String) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    label = { Text("$label ($unit)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RaithaColors.GreenPrimary,
                        focusedLabelColor = RaithaColors.GreenPrimary
                    )
                )
            }

            SoilField("Nitrogen (N)", nitrogen, { nitrogen = it }, "kg/ha")
            Spacer(Modifier.height(8.dp))
            SoilField("Phosphorus (P)", phosphorus, { phosphorus = it }, "kg/ha")
            Spacer(Modifier.height(8.dp))
            SoilField("Potassium (K)", potassium, { potassium = it }, "kg/ha")
            Spacer(Modifier.height(8.dp))
            SoilField("Soil Moisture", moisture, { moisture = it }, "%")
            Spacer(Modifier.height(8.dp))
            SoilField("Soil pH", phLevel, { phLevel = it }, "0-14")

            Spacer(Modifier.height(16.dp))

            uiState.error?.let {
                Text(it, color = RaithaColors.StatusPoor, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }

            AnimatedVisibility(uiState.savedSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RaithaColors.StatusOptimal.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = RaithaColors.StatusOptimal)
                        Spacer(Modifier.width(8.dp))
                        Text("✅ Data saved successfully!", color = RaithaColors.StatusOptimal)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.generateSimulatedData() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Simulate Data")
                }
                Button(
                    onClick = {
                        viewModel.saveSoilData(nitrogen, phosphorus, potassium, moisture, phLevel)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = RaithaColors.GreenPrimary)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = RaithaColors.OnPrimary
                        )
                    } else {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save Data")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
