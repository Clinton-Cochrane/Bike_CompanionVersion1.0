package com.clintoncochrane.bikecompanion.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.clintoncochrane.bikecompanion.ui.garage.AddEditBikeScreen
import com.clintoncochrane.bikecompanion.ui.garage.BikeDetailScreen
import com.clintoncochrane.bikecompanion.ui.garage.SimpleAddBikeScreen
import com.clintoncochrane.bikecompanion.ui.garage.ComponentDetailScreen
import com.clintoncochrane.bikecompanion.ui.garage.GarageScreen
import com.clintoncochrane.bikecompanion.ui.garage.ServiceListScreen
import com.clintoncochrane.bikecompanion.ui.garage.WallOfHonorScreen
import com.clintoncochrane.bikecompanion.ui.settings.SettingsScreen
import com.clintoncochrane.bikecompanion.ui.stats.StatsScreen
import com.clintoncochrane.bikecompanion.ui.trip.TripScreen
import com.clintoncochrane.bikecompanion.ui.trip.TripStartSplashScreen
import com.clintoncochrane.bikecompanion.ui.trip.TripSettingsPlaceholderScreen
import com.clintoncochrane.bikecompanion.ui.trip.EditRidePlaceholderScreen
import com.clintoncochrane.bikecompanion.ui.trip.RideDetailScreen

sealed class Screen(val route: String) {
    data object Trip : Screen("trip")
    data object Garage : Screen("garage")
    data object Stats : Screen("stats")
    data object BikeDetail : Screen("bike_detail/{bikeId}") {
        fun withId(id: Long) = "bike_detail/$id"
    }
    data object AddBike : Screen("add_bike")
    data object AddBikeSimple : Screen("add_bike_simple")
    data object EditBike : Screen("edit_bike/{bikeId}") {
        fun withId(id: Long) = "edit_bike/$id"
    }
    data object TripStartSplash : Screen("trip_start_splash/{bikeId}/{hadPlaceholders}") {
        fun withId(id: Long, hadPlaceholders: Boolean = false) =
            "trip_start_splash/$id/${hadPlaceholders}"
    }
    data object TripSettings : Screen("trip_settings")
    data object EditRide : Screen("edit_ride/{rideId}") {
        fun withId(rideId: Long) = "edit_ride/$rideId"
    }
    data object RideDetail : Screen("ride_detail/{rideId}") {
        fun withId(rideId: Long) = "ride_detail/$rideId"
    }
    data object ComponentDetail : Screen("component_detail/{componentId}") {
        fun withId(id: Long) = "component_detail/$id"
    }
    data object EditComponent : Screen("edit_component/{componentId}") {
        fun withId(id: Long) = "edit_component/$id"
    }
    data object ServiceList : Screen("service_list")
    data object WallOfHonor : Screen("wall_of_honor")
    data object Settings : Screen("settings")
}

@Composable
fun BikeCompanionNavGraph(
    navController: NavHostController,
    startDestination: String,
    onStartRide: () -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(),
) {
    NavHost(
        modifier = modifier.padding(paddingValues),
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Screen.Trip.route) { TripScreen(navController = navController, onStartRide = onStartRide) }
        composable(Screen.Garage.route) { GarageScreen(navController = navController, onStartRide = onStartRide) }
        composable(Screen.Stats.route) { StatsScreen(navController = navController, onStartRide = onStartRide) }
        composable(Screen.BikeDetail.route) { backStackEntry ->
            BikeDetailScreen(navController = navController, backStackEntry = backStackEntry)
        }
        composable(Screen.AddBike.route) { backStackEntry ->
            AddEditBikeScreen(navController = navController, backStackEntry = backStackEntry, bikeId = null)
        }
        composable(Screen.AddBikeSimple.route) { backStackEntry ->
            SimpleAddBikeScreen(navController = navController, backStackEntry = backStackEntry)
        }
        composable(Screen.EditBike.route) { backStackEntry ->
            val bikeId = backStackEntry.arguments?.getString("bikeId")?.toLongOrNull() ?: 0L
            AddEditBikeScreen(navController = navController, backStackEntry = backStackEntry, bikeId = bikeId)
        }
        composable(Screen.TripStartSplash.route) { backStackEntry ->
            val bikeId = backStackEntry.arguments?.getString("bikeId")?.toLongOrNull() ?: -1L
            val hadPlaceholders = backStackEntry.arguments?.getString("hadPlaceholders")?.toBooleanStrictOrNull() ?: false
            TripStartSplashScreen(
                navController = navController,
                bikeId = bikeId,
                hadPlaceholdersAtStart = hadPlaceholders,
            )
        }
        composable(Screen.TripSettings.route) {
            TripSettingsPlaceholderScreen(navController = navController)
        }
        composable(Screen.EditRide.route) { backStackEntry ->
            val rideId = backStackEntry.arguments?.getString("rideId")?.toLongOrNull() ?: 0L
            EditRidePlaceholderScreen(navController = navController, rideId = rideId)
        }
        composable(Screen.RideDetail.route) { backStackEntry ->
            RideDetailScreen(navController = navController, backStackEntry = backStackEntry)
        }
        composable(Screen.ComponentDetail.route) { backStackEntry ->
            ComponentDetailScreen(navController = navController, backStackEntry = backStackEntry)
        }
        composable(Screen.EditComponent.route) { backStackEntry ->
            ComponentDetailScreen(
                navController = navController,
                backStackEntry = backStackEntry,
                openEditorOnLaunch = true,
            )
        }
        composable(Screen.ServiceList.route) {
            ServiceListScreen(navController = navController)
        }
        composable(Screen.WallOfHonor.route) {
            WallOfHonorScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}
