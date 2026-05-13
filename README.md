<<<<<<< HEAD
# 🌾 Raitha-Bharosa Hub
### Smart Sowing Assistant — Android App

> *"ರೈತ-ಭರೋಸಾ ಹಬ್" — Empowering Karnataka's farmers with data-driven decisions*

---

## 📱 App Overview

**Raitha-Bharosa Hub** is a GenAI-powered Android app that serves as a decision-support system for Indian farmers. It bridges the gap between complex agronomic data and actionable field guidance — telling farmers exactly **what to do today** based on real-time soil, weather, and nutrient analysis.

### Core Problem Solved
Farmers miss the **48-hour Optimal Sowing Window** where soil moisture and temperature are perfect for a specific crop. Without a data-driven guide, they face reduced yields or wasted seeds.

---

## 🏗️ Architecture — MVVM with Clean Architecture

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                      │
│   Jetpack Compose Screens + ViewModels          │
│   ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│   │Onboarding│ │Dashboard │ │KrishiCalendar│   │
│   └──────────┘ └──────────┘ └──────────────┘   │
│   ┌──────────┐ ┌──────────┐                    │
│   │InputCenter│ │History   │                    │
│   └──────────┘ └──────────┘                    │
└─────────────────────────────────────────────────┘
                      ↕ StateFlow / Compose State
┌─────────────────────────────────────────────────┐
│                 Domain Layer                    │
│   Use Cases (Pure Kotlin business logic)        │
│   ┌────────────────────┐ ┌───────────────────┐  │
│   │CalculateSowingIndex│ │GenerateKrishiCal  │  │
│   └────────────────────┘ └───────────────────┘  │
│   Domain Models: Farmer, SoilData, SowingIndex  │
└─────────────────────────────────────────────────┘
                      ↕ Repositories
┌─────────────────────────────────────────────────┐
│                  Data Layer                     │
│  ┌──────────────┐  ┌────────────┐  ┌─────────┐ │
│  │ Room Database│  │ Retrofit   │  │DataGen  │ │
│  │  (Local DB)  │  │  API/Mock  │  │Simulator│ │
│  └──────────────┘  └────────────┘  └─────────┘ │
└─────────────────────────────────────────────────┘
```

### Tech Stack

| Component           | Technology                        |
|---------------------|-----------------------------------|
| UI Framework        | Jetpack Compose + Material 3      |
| Architecture        | MVVM + Clean Architecture         |
| DI                  | Hilt (Dagger)                     |
| Local DB            | Room + DAOs                       |
| Networking          | Retrofit 2 + OkHttp               |
| State Management    | Kotlin StateFlow + collectAsState |
| Navigation          | Navigation Compose                |
| Build               | Gradle KTS + Version Catalog      |
| Language Support    | English + ಕನ್ನಡ (Kannada)        |

---

## 📂 Project Structure

```
app/src/main/java/com/raitha/bharosa/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt              # Room database
│   │   ├── dao/Daos.kt                 # FarmerDao, SoilDataDao, CropHistoryDao, SnapshotDao
│   │   └── entity/Entities.kt          # Room @Entity classes
│   ├── remote/
│   │   └── WeatherApiService.kt        # Retrofit interface + OWM DTOs
│   ├── repository/
│   │   └── Repositories.kt             # FarmerRepo, SoilDataRepo, WeatherRepo, HistoryRepo
│   └── simulation/
│       └── DataGenerator.kt            # ✨ Core simulation engine
├── domain/
│   ├── model/
│   │   ├── CropType.kt                 # 8 crops with agronomic thresholds
│   │   └── Models.kt                   # Farmer, SoilData, SowingIndex, KrishiCalendar…
│   └── usecase/
│       └── UseCases.kt                 # CalculateSowingIndexUseCase, GenerateKrishiCalendarUseCase
├── ui/
│   ├── components/Components.kt        # SowingIndexGauge, WeatherCard, ParameterTile…
│   ├── navigation/NavGraph.kt          # Screen routes + NavHost
│   ├── screens/
│   │   ├── onboarding/OnboardingScreen.kt
│   │   ├── dashboard/DashboardScreen.kt
│   │   ├── inputcenter/InputCenterScreen.kt
│   │   ├── calendar/KrishiCalendarScreen.kt
│   │   └── history/HistoryScreen.kt
│   └── theme/Theme.kt                  # RaithaColors, RaithaTypography
├── di/AppModules.kt                    # Hilt DI: DatabaseModule, NetworkModule
├── MainActivity.kt
└── RaithaBharosaApp.kt
```

---

## 🌱 Core Features

### 1. 📊 Dashboard — Sowing Index Gauge
- **Animated circular gauge** (0–100) showing composite Sowing Index
- **Crop Health Velocity** computed from 4 weighted parameters:
  - 🌊 Moisture Score (30%) — spec rule: `moisture > 30% → "Soil too wet to sow"`
  - 🌡️ Temperature Score (30%)
  - 🧪 Nutrient Score (25%) — NPK ratios vs crop-specific optima
  - 🌥️ Weather Score (15%) — precipitation & wind
- **Color-coded status**: 🟢 Optimal → 🟡 Fair → 🔴 Critical
- **Auto-refresh** every 5 minutes simulating live sensor updates

### 2. 🌱 Onboarding
- Bilingual: **English / ಕನ್ನಡ** toggle
- Select from 8 crops (Paddy, Sugarcane, Ragi, Maize, Cotton, Groundnut, Soybean, Tomato)
- Farmer name + village + primary crop saved to Room DB

### 3. 🔬 Input Center (Field Data)
- Manual entry of **N, P, K, Moisture, pH** values
- Instant soil moisture status: *"Soil too wet to sow"* / *"Too dry"* / *"Optimal"*
- **Simulate Data** button generates realistic seasonal values via `DataGenerator`
- Data persisted to Room and reflected live in Dashboard

### 4. 📅 Krishi Calendar (7-Day Action Plan)
- **Storm warning banner** if heavy rain predicted in next 3 days
- Advance alert: *"Fertilize TODAY — heavy storm predicted in 2 days"*
- Each day shows: 🌱 Sow / 🪣 Fertilize / 💧 Irrigate / ⏳ Wait / 🌧️ Avoid
- Priority color bar (High/Medium/Low)
- Optimal sowing window badge ⭐

### 5. 📖 Season History
- Log each Kharif/Rabi season with sowing date, estimated & actual yield
- **Inline yield editing** — update actual yield after harvest
- **Yield variance analysis**: *"📈 +120 kg (15% above estimate)"*
- Summary stats card (total seasons, average yield)

---

## ⚙️ DataGenerator — Simulation Engine

```kotlin
// Spec-required moisture check:
fun generateMoisturePercent(): Float {
    return random.nextFloat() * 30f + 10f  // range: 10..40%
}

// App logic:
if (moisture > 30%) { display "Soil too wet to sow" }

// Seasonal temperature (Karnataka climate):
val baseTemp = when (month) {
    in 6..9  -> 26f   // Monsoon / Kharif
    in 3..5  -> 32f   // Summer
    else     -> 22f   // Rabi / Winter
}
```

The `DataGenerator` class simulates:
- Soil moisture (10–40%, influenced by precipitation)
- Temperature with diurnal variation and seasonal patterns
- NPK levels as % of crop-specific optimal values
- Complete weather snapshots with monsoon probability curves
- 7-day forecast with realistic storm simulation on days 3–4

---

## 🌐 Weather API Integration

```
Base URL: https://api.openweathermap.org/data/2.5/
Endpoints used:
  GET /weather   → Current weather by lat/lon
  GET /forecast  → 5-day / 3-hour forecast
```

### To enable live weather:
1. Register at [openweathermap.org](https://openweathermap.org) (free tier)
2. In `app/build.gradle.kts`, replace:
   ```kotlin
   buildConfigField("String", "OWM_API_KEY", "\"YOUR_API_KEY_HERE\"")
   ```
3. **Without an API key**, the app automatically falls back to `DataGenerator` simulation — fully functional.

---

## 🗄️ Room Database Schema

```
farmers            → farmer profile (name, crop, language, village, GPS)
soil_data          → N/P/K/moisture/pH logs (FK → farmers)
crop_history       → season records + yield data (FK → farmers)
simulated_snapshots → hourly parameter snapshots for trend analysis
```

---

## 🚀 Setup & Build

### Prerequisites
- Android Studio Koala (2024.1+)
- JDK 17
- Android SDK 26+ (minSdk), 35 (targetSdk)
- Gradle 8.5

### Steps
```bash
git clone <repo>
cd RaithaBharosaHub

# Optional: add your OWM API key in app/build.gradle.kts

./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

---

## ✅ Success Criteria (All Met)

| Criteria | Implementation |
|----------|----------------|
| Dashboard loads < 2 seconds | StateFlow + coroutine async loading |
| Sowing Index changes dynamically | `refreshSimulatedData()` regenerates random values |
| Fully bilingual UI (Kannada/English) | `values/strings.xml` + `values-kn/strings.xml` |
| MVVM architecture | ViewModel → UseCase → Repository → Room/Retrofit |

---

## 🌍 Impact Goals

- **Precision Farming**: 15–20% seed waste reduction via optimal sowing timing
- **Digital Literacy**: Engineering-grade logic at grassroots level
- **Sustainability**: Targeted fertilizer use to prevent soil degradation

---

## 📜 License
Academic / Educational use — 4th Year Engineering Project
*Raitha-Bharosa Hub © 2024 — Built for Karnataka's farming community*
=======
# RaithaBarsohaHub
>>>>>>> 64c5cba178e9189886fcc4f30d118b844e98341c
