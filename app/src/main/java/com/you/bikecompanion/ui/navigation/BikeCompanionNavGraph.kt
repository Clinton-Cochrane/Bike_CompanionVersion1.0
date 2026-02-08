package com.you.bikecompanion.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.you.bikecompanion.ui.ai.AiScreen
import com.you.bikecompanion.ui.garage.AddEditBikeScreen
import com.you.bikecompanion.ui.garage.BikeDetailScreen
import com.you.bikecompanion.ui.garage.ComponentDetailScreen
import com.you.bikecompanion.ui.garage.GarageScreen
import com.you.bikecompanion.ui.garage.ServiceListScreen
import com.you.bikecompanion.ui.settings.SettingsScreen
import com.you.bikecompanion.ui.stats.StatsScreen
import com.you.bikecompanion.ui.trip.TripScreen
import com.you.bikecompanion.ui.trip.TripStartSplashScreen

sealed class Screen(val route: String) {
    data object Trip : Screen("trip")
    data object Garage : Screen("garage")
    data object Stats : Screen("stats")
    data object Ai : Screen("ai")
    data object BikeDetail : Screen("bike_detail/{bikeId}") {
        fun withId(id: Long) = "bike_detail/$id"
    }
    data object AddBike : Screen("add_bike")
    data object EditBike : Screen("edit_bike/{bikeId}") {
        fun withId(id: Long) = "edit_bike/$id"
    }
    data object TripStartSplash : Screen("trip_start_splash/{bikeId}") {
        fun withId(id: Long) = "trip_start_splash/$id"
    }
    data object ComponentDetail : Screen("component_detail/{componentId}") {
        fun withId(id: Long) = "component_detail/$id"
    }
    data object ServiceList : Screen("service_list")
    data object Settings : Screen("settings")
}

@Composable
fun BikeCompanionNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(),
) {
    NavHost(
        modifier = modifier.padding(paddingValues),
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Screen.Trip.route) { TripScreen(navController = navController) }
        composable(Screen.Garage.route) { GarageScreen(navController = navController) }
        composable(Screen.Stats.route) { StatsScreen(navController = navController) }
        composable(Screen.Ai.route) { AiScreen(navController = navController) }
        composable(Screen.BikeDetail.route) { backStackEntry ->
            BikeDetailScreen(navController = navController, backStackEntry = backStackEntry)
        }
        composable(Screen.AddBike.route) { backStackEntry ->
            AddEditBikeScreen(navController = navController, backStackEntry = backStackEntry, bikeId = null)
        }
        composable(Screen.EditBike.route) { backStackEntry ->
            val bikeId = backStackEntry.arguments?.getString("bikeId")?.toLongOrNull() ?: 0L
            AddEditBikeScreen(navController = navController, backStackEntry = backStackEntry, bikeId = bikeId)
        }
        composable(Screen.TripStartSplash.route) { backStackEntry ->
            val bikeId = backStackEntry.arguments?.getString("bikeId")?.toLongOrNull() ?: -1L
            TripStartSplashScreen(navController = navController, bikeId = bikeId)
        }
        composable(Screen.ComponentDetail.route) { backStackEntry ->
            ComponentDetailScreen(navController = navController, backStackEntry = backStackEntry)
        }
        composable(Screen.ServiceList.route) {
            ServiceListScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}
