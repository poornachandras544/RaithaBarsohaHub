package com.raitha.bharosa.ui.screens.history

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.raitha.bharosa.data.repository.CropHistoryRepository
import com.raitha.bharosa.data.repository.FarmerRepository
import com.raitha.bharosa.domain.model.*
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

data class HistoryUiState(
    val isLoading: Boolean = true,
    val farmer: Farmer? = null,
    val records: List<CropSeasonRecord> = emptyList(),
    val showAddDialog: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val farmerRepository: FarmerRepository,
    private val cropHistoryRepository: CropHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init { loadHistory() }

    private fun loadHistory() {
        viewModelScope.launch {
            farmerRepository.getLatestFarmer().collect { farmer ->
                if (farmer == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@collect
                }
                cropHistoryRepository.getAllRecords(farmer.id).collect { records ->
                    _uiState.update {
                        it.copy(isLoading = false, farmer = farmer, records = records)
                    }
                }
            }
        }
    }

    fun showAddDialog() { _uiState.update { it.copy(showAddDialog = true) } }
    fun hideAddDialog() { _uiState.update { it.copy(showAddDialog = false) } }

    fun addSeasonRecord(
        season: String,
        crop: CropType,
        sowingDateMs: Long,
        estimatedYield: String?,
        actualYield: String?,
        notes: String
    ) {
        val farmerId = _uiState.value.farmer?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val record = CropSeasonRecord(
                farmerId = farmerId,
                cropType = crop,
                season = season,
                sowingDate = sowingDateMs,
                estimatedYieldKg = estimatedYield?.toFloatOrNull(),
                actualYieldKg = actualYield?.toFloatOrNull(),
                notes = notes
            )
            cropHistoryRepository.saveRecord(record)
            _uiState.update { it.copy(isSaving = false, showAddDialog = false) }
        }
    }

    fun updateActualYield(record: CropSeasonRecord, actualYield: Float) {
        viewModelScope.launch {
            cropHistoryRepository.updateRecord(record.copy(actualYieldKg = actualYield))
        }
    }
}

// ─────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Season History", fontWeight = FontWeight.Bold)
                        Text("Compare your yield across seasons",
                            style = MaterialTheme.typography.bodySmall.copy(color = RaithaColors.TextSecondary))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, "Add Record", tint = RaithaColors.GreenPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            RaithaBottomNavBar(
                currentRoute = Screen.History.route,
                onNavigate = { navController.navigate(it) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = RaithaColors.GreenPrimary
            ) {
                Icon(Icons.Default.Add, null, tint = RaithaColors.OnPrimary)
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RaithaColors.GreenPrimary)
            }
            return@Scaffold
        }

        if (uiState.records.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No Season Records Yet",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Track your sowing seasons and\ncompare yields over time.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = RaithaColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.showAddDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = RaithaColors.GreenPrimary)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add First Season")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary card
                item {
                    YieldSummaryCard(records = uiState.records)
                }

                // Season records
                items(uiState.records) { record ->
                    SeasonRecordCard(
                        record = record,
                        dateFormat = dateFormat,
                        onUpdateYield = { yield ->
                            viewModel.updateActualYield(record, yield)
                        }
                    )
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }

        // Add Dialog
        if (uiState.showAddDialog) {
            AddSeasonDialog(
                defaultCrop = uiState.farmer?.primaryCrop ?: CropType.PADDY,
                onDismiss = { viewModel.hideAddDialog() },
                onSave = { season, crop, sowingDate, estYield, actualYield, notes ->
                    viewModel.addSeasonRecord(season, crop, sowingDate, estYield, actualYield, notes)
                }
            )
        }
    }
}

@Composable
private fun YieldSummaryCard(records: List<CropSeasonRecord>) {
    val totalSeasons = records.size
    val seasonsWithYield = records.filter { it.actualYieldKg != null }
    val avgYield = if (seasonsWithYield.isNotEmpty()) {
        seasonsWithYield.mapNotNull { it.actualYieldKg }.average().toFloat()
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RaithaColors.GreenSurface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryStat("$totalSeasons", "Seasons", "📅")
            SummaryStat(
                if (avgYield != null) "${avgYield.toInt()} kg" else "--",
                "Avg Yield", "🌾"
            )
            SummaryStat(
                records.firstOrNull()?.cropType?.iconEmoji ?: "🌱",
                records.firstOrNull()?.cropType?.displayNameEn?.take(8) ?: "N/A",
                ""
            )
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (emoji.isNotEmpty()) Text(emoji, fontSize = 20.sp)
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = RaithaColors.TextSecondary))
    }
}

@Composable
private fun SeasonRecordCard(
    record: CropSeasonRecord,
    dateFormat: SimpleDateFormat,
    onUpdateYield: (Float) -> Unit
) {
    var showYieldEdit by remember { mutableStateOf(false) }
    var yieldInput by remember { mutableStateOf(record.actualYieldKg?.toString() ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(record.cropType.iconEmoji, fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            record.season,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            record.cropType.displayNameEn,
                            style = MaterialTheme.typography.bodySmall.copy(color = RaithaColors.TextSecondary)
                        )
                    }
                }
                IconButton(onClick = { showYieldEdit = !showYieldEdit }) {
                    Icon(
                        if (showYieldEdit) Icons.Default.Close else Icons.Default.Edit,
                        null,
                        tint = RaithaColors.GreenPrimary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LabelValue("Sowing Date", dateFormat.format(Date(record.sowingDate)))
                LabelValue("Est. Yield", record.estimatedYieldKg?.let { "${it.toInt()} kg" } ?: "--")
                LabelValue(
                    "Actual Yield",
                    record.actualYieldKg?.let { "${it.toInt()} kg" } ?: "Not set",
                    valueColor = if (record.actualYieldKg != null) RaithaColors.StatusOptimal else RaithaColors.TextSecondary
                )
            }

            // Yield variance
            if (record.estimatedYieldKg != null && record.actualYieldKg != null) {
                val variance = record.actualYieldKg - record.estimatedYieldKg
                val pct = (variance / record.estimatedYieldKg) * 100f
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (variance >= 0) "📈 +${variance.toInt()} kg (${pct.toInt()}% above estimate)"
                           else "📉 ${variance.toInt()} kg (${kotlin.math.abs(pct.toInt())}% below estimate)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (variance >= 0) RaithaColors.StatusOptimal else RaithaColors.StatusPoor,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            if (record.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "📝 ${record.notes}",
                    style = MaterialTheme.typography.bodySmall.copy(color = RaithaColors.TextSecondary)
                )
            }

            // Inline yield editor
            if (showYieldEdit) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = yieldInput,
                        onValueChange = { yieldInput = it },
                        label = { Text("Actual Yield (kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RaithaColors.GreenPrimary
                        )
                    )
                    Button(
                        onClick = {
                            yieldInput.toFloatOrNull()?.let {
                                onUpdateYield(it)
                                showYieldEdit = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaithaColors.GreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun LabelValue(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = RaithaColors.TextPrimary
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = RaithaColors.TextSecondary))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.SemiBold, color = valueColor
        ))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSeasonDialog(
    defaultCrop: CropType,
    onDismiss: () -> Unit,
    onSave: (String, CropType, Long, String?, String?, String) -> Unit
) {
    var season       by remember { mutableStateOf("Kharif ${Calendar.getInstance().get(Calendar.YEAR)}") }
    var selectedCrop by remember { mutableStateOf(defaultCrop) }
    var sowingDate   by remember { mutableStateOf("") }
    var estYield     by remember { mutableStateOf("") }
    var actualYield  by remember { mutableStateOf("") }
    var notes        by remember { mutableStateOf("") }
    var expanded     by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Season Record", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = season, onValueChange = { season = it },
                    label = { Text("Season (e.g. Kharif 2024)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Crop dropdown
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = "${selectedCrop.iconEmoji} ${selectedCrop.displayNameEn}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Crop") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        CropType.entries.forEach { crop ->
                            DropdownMenuItem(
                                text = { Text("${crop.iconEmoji} ${crop.displayNameEn}") },
                                onClick = { selectedCrop = crop; expanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = sowingDate, onValueChange = { sowingDate = it },
                    label = { Text("Sowing Date (dd/MM/yyyy)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = estYield, onValueChange = { estYield = it },
                    label = { Text("Estimated Yield (kg) — optional") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = actualYield, onValueChange = { actualYield = it },
                    label = { Text("Actual Yield (kg) — optional") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dateMs = try {
                        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                            .parse(sowingDate)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) { System.currentTimeMillis() }

                    onSave(season, selectedCrop, dateMs, estYield.ifBlank { null }, actualYield.ifBlank { null }, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RaithaColors.GreenPrimary)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
