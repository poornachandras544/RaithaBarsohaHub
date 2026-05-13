package com.raitha.bharosa.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.raitha.bharosa.ui.screens.calendar.KrishiCalendarScreen
import com.raitha.bharosa.ui.screens.dashboard.DashboardScreen
import com.raitha.bharosa.ui.screens.history.HistoryScreen
import com.raitha.bharosa.ui.screens.inputcenter.InputCenterScreen
import com.raitha.bharosa.ui.screens.onboarding.OnboardingScreen

// ─────────────────────────────────────────────
//  Screen Route Definitions
// ─────────────────────────────────────────────

sealed class Screen(val route: String) {
    object Onboarding     : Screen("onboarding")
    object Dashboard      : Screen("dashboard")
    object InputCenter    : Screen("input_center")
    object KrishiCalendar : Screen("krishi_calendar")
    object History        : Screen("history")
}

// Bottom nav items
data class BottomNavItem(
    val screen: Screen,
    val labelEn: String,
    val labelKn: String,
    val iconRes: String   // material icon name
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard,      "Dashboard",  "ಡ್ಯಾಶ್‌ಬೋರ್ಡ್", "home"),
    BottomNavItem(Screen.InputCenter,    "Field Data", "ಭೂಮಿ ದತ್ತಾಂಶ",  "science"),
    BottomNavItem(Screen.KrishiCalendar, "Calendar",   "ಕ್ಯಾಲೆಂಡರ್",    "calendar_today"),
    BottomNavItem(Screen.History,        "History",    "ಇತಿಹಾಸ",         "history")
)

// ─────────────────────────────────────────────
//  NavGraph
// ─────────────────────────────────────────────

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Onboarding.route,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                navController = navController,
                onNavigateToInputCenter = { navController.navigate(Screen.InputCenter.route) },
                onNavigateToCalendar = { navController.navigate(Screen.KrishiCalendar.route) }
            )
        }

        composable(Screen.InputCenter.route) {
            InputCenterScreen(navController = navController)
        }

        composable(Screen.KrishiCalendar.route) {
            KrishiCalendarScreen(navController = navController)
        }

        composable(Screen.History.route) {
            HistoryScreen(navController = navController)
        }
    }
}
