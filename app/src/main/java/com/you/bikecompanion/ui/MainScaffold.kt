package com.you.bikecompanion.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.you.bikecompanion.R
import com.you.bikecompanion.ui.navigation.BikeCompanionNavGraph
import com.you.bikecompanion.ui.navigation.Screen

@Composable
fun MainScaffold(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appViewModel: AppViewModel = androidx.hilt.navigation.compose.hiltViewModel(
        viewModelStoreOwner = context as ComponentActivity,
    )
    val isInitialized by appViewModel.isInitialized.collectAsState()
    val hasAnyBike by appViewModel.hasAnyBike.collectAsState()

    if (!isInitialized) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Text(
                    text = stringResource(R.string.common_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        return
    }

    val startDestination =
        if (hasAnyBike) Screen.Trip.route else Screen.Garage.route
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var hadAnyBikeWhenNavigated by remember { mutableStateOf(hasAnyBike) }
    LaunchedEffect(hasAnyBike, currentDestination?.route) {
        if (hasAnyBike && !hadAnyBikeWhenNavigated) {
            hadAnyBikeWhenNavigated = true
            if (currentDestination?.route == Screen.Garage.route) {
                navController.navigate(Screen.Trip.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        } else if (!hasAnyBike) {
            hadAnyBikeWhenNavigated = false
        }
    }

    val bottomNavItems = listOf(
        Screen.Trip to (Icons.Default.Home to R.string.nav_trip),
        Screen.Garage to (Icons.Default.DirectionsBike to R.string.nav_garage),
        Screen.Stats to (Icons.Default.StackedBarChart to R.string.nav_stats),
        Screen.Ai to (Icons.Default.Psychology to R.string.nav_ai),
    )

    androidx.compose.material3.Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { (screen, pair) ->
                    val (icon, labelRes) = pair
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = stringResource(labelRes),
                            )
                        },
                        label = { Text(stringResource(labelRes)) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { paddingValues ->
        BikeCompanionNavGraph(
            navController = navController,
            startDestination = startDestination,
            paddingValues = paddingValues,
        )
    }
}
