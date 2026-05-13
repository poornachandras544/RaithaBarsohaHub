package com.raitha.bharosa.ui.screens.onboarding

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.raitha.bharosa.data.local.dao.FarmerDao
import com.raitha.bharosa.data.repository.FarmerRepository
import com.raitha.bharosa.domain.model.*
import com.raitha.bharosa.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────

data class OnboardingUiState(
    val isLoading: Boolean = true,
    val existingFarmers: List<Farmer> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val farmerRepository: FarmerRepository,
    private val farmerDao: FarmerDao // Injecting DAO directly for convenience in this specific case
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadExistingProfiles()
    }

    private fun loadExistingProfiles() {
        viewModelScope.launch {
            farmerDao.getAllFarmers().collect { entities ->
                val farmers = entities.map { entity ->
                    Farmer(
                        id = entity.id,
                        name = entity.name,
                        primaryCrop = CropType.valueOf(entity.primaryCrop),
                        language = if (entity.language == "kn") AppLanguage.KANNADA else AppLanguage.ENGLISH,
                        villageName = entity.villageName,
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        createdAt = entity.createdAt
                    )
                }
                _uiState.update { it.copy(isLoading = false, existingFarmers = farmers) }
            }
        }
    }

    fun selectFarmer(farmer: Farmer, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Re-saving with current timestamp to make it "latest"
            farmerRepository.saveFarmer(farmer.copy(createdAt = System.currentTimeMillis()))
            onSuccess()
        }
    }

    fun saveFarmer(
        name: String,
        crop: CropType,
        language: AppLanguage,
        villageName: String,
        onSuccess: () -> Unit
    ) {
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your name") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val farmer = Farmer(
                    name = name.trim(),
                    primaryCrop = crop,
                    language = language,
                    villageName = villageName.trim()
                )
                farmerRepository.saveFarmer(farmer)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Failed to save profile. Please try again."
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onOnboardingComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCreateForm by remember { mutableStateOf(false) }
    var farmerName by remember { mutableStateOf("") }
    var villageName by remember { mutableStateOf("") }
    var selectedCrop by remember { mutableStateOf(CropType.PADDY) }
    var selectedLanguage by remember { mutableStateOf(AppLanguage.ENGLISH) }

    val isKannada = selectedLanguage == AppLanguage.KANNADA

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        RaithaColors.GreenPrimary,
                        RaithaColors.GreenLight,
                        RaithaColors.GreenSurface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Text("🌾", fontSize = 72.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isKannada) "ರೈತ-ಭರೋಸಾ ಹಬ್" else "Raitha-Bharosa Hub",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = RaithaColors.OnPrimary,
                    fontWeight = FontWeight.ExtraBold
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            if (uiState.existingFarmers.isNotEmpty() && !showCreateForm) {
                Text(
                    text = if (isKannada) "ಪ್ರೊಫೈಲ್ ಆಯ್ಕೆಮಾಡಿ" else "Select a Profile",
                    style = MaterialTheme.typography.titleLarge.copy(color = RaithaColors.OnPrimary, fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(16.dp))

                uiState.existingFarmers.forEach { farmer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.selectFarmer(farmer, onOnboardingComplete)
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(farmer.primaryCrop.iconEmoji, fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(farmer.name, fontWeight = FontWeight.Bold)
                                Text(farmer.villageName, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showCreateForm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RaithaColors.OnPrimary),
                    border = BorderStroke(1.dp, RaithaColors.OnPrimary)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isKannada) "ಹೊಸ ಪ್ರೊಫೈಲ್ ಸೇರಿಸಿ" else "Add New Profile")
                }
            } else {
                // Creation Form
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = RaithaColors.OnPrimary.copy(alpha = 0.15f))
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            val isSelected = selectedLanguage == lang
                            TextButton(
                                onClick = { selectedLanguage = lang },
                                modifier = Modifier
                                    .then(
                                        if (isSelected) Modifier.background(
                                            RaithaColors.OnPrimary,
                                            RoundedCornerShape(8.dp)
                                        ) else Modifier
                                    )
                            ) {
                                Text(
                                    text = lang.displayName,
                                    color = if (isSelected) RaithaColors.GreenPrimary else RaithaColors.OnPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (isKannada) "ಹೊಸ ಪ್ರೊಫೈಲ್" else "New Profile",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = RaithaColors.GreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = farmerName,
                            onValueChange = { farmerName = it },
                            label = { Text(if (isKannada) "ನಿಮ್ಮ ಹೆಸರು *" else "Your Name *") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = villageName,
                            onValueChange = { villageName = it },
                            label = { Text(if (isKannada) "ಗ್ರಾಮ / ಊರು" else "Village / Town") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = if (isKannada) "ನಿಮ್ಮ ಬೆಳೆ ಆಯ್ಕೆ ಮಾಡಿ" else "Select Your Crop",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(Modifier.height(8.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.heightIn(max = 300.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(CropType.entries) { crop ->
                                val isSelected = selectedCrop == crop
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCrop = crop },
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
                                        Text(crop.iconEmoji, fontSize = 24.sp)
                                        Text(
                                            text = if (isKannada) crop.displayNameKn else crop.displayNameEn,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                uiState.error?.let {
                    Text(text = it, color = RaithaColors.StatusPoor, modifier = Modifier.padding(8.dp))
                }

                Button(
                    onClick = {
                        viewModel.saveFarmer(farmerName, selectedCrop, selectedLanguage, villageName) {
                            onOnboardingComplete()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !uiState.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = RaithaColors.GreenPrimary)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = RaithaColors.OnPrimary)
                    } else {
                        Text(if (isKannada) "ಉಳಿಸಿ ಮತ್ತು ಮುಂದುವರಿಯಿರಿ" else "Save and Get Started")
                    }
                }

                if (uiState.existingFarmers.isNotEmpty()) {
                    TextButton(onClick = { showCreateForm = false }) {
                        Text(if (isKannada) "ಹಿಂದಕ್ಕೆ ಹೋಗಿ" else "Back to Profiles", color = RaithaColors.OnPrimary)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
