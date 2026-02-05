package com.you.bikecompanion.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.you.bikecompanion.ui.ai.AiScreen
import com.you.bikecompanion.ui.garage.AddEditBikeScreen
import com.you.bikecompanion.ui.garage.BikeDetailScreen
import com.you.bikecompanion.ui.garage.GarageScreen
import com.you.bikecompanion.ui.stats.StatsScreen
import com.you.bikecompanion.ui.trip.TripScreen

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
}

@Composable
fun BikeCompanionNavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(),
) {
    NavHost(
        modifier = modifier.padding(paddingValues),
        navController = navController,
        startDestination = Screen.Trip.route,
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
    }
}
