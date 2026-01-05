package com.example.campergas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.campergas.ui.screens.bleconnect.BleConnectScreen
import com.example.campergas.ui.screens.caravanconfig.CaravanConfigScreen
import com.example.campergas.ui.screens.consumption.ConsumptionScreen
import com.example.campergas.ui.screens.home.HomeScreen
import com.example.campergas.ui.screens.inclination.InclinationScreen
import com.example.campergas.ui.screens.settings.SettingsScreen
import com.example.campergas.ui.screens.weight.WeightScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.Weight.route) {
            WeightScreen(navController = navController)
        }

        composable(Screen.Inclination.route) {
            InclinationScreen(navController = navController)
        }

        composable(Screen.Consumption.route) {
            ConsumptionScreen(navController = navController)
        }

        composable(Screen.BleConnect.route) {
            BleConnectScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }

        composable(Screen.CaravanConfig.route) {
            CaravanConfigScreen(navController = navController)
        }

    }
}

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Weight : Screen("weight")
    data object Inclination : Screen("inclination")
    data object Consumption : Screen("consumption")
    data object BleConnect : Screen("ble_connect")
    data object Settings : Screen("settings")
    data object CaravanConfig : Screen("caravan_config")
}
