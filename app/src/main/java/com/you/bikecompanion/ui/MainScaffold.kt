package com.you.bikecompanion.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.you.bikecompanion.R
import com.you.bikecompanion.ui.navigation.BikeCompanionNavGraph
import com.you.bikecompanion.ui.navigation.Screen

@Composable
fun MainScaffold(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

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
            paddingValues = paddingValues,
        )
    }
}
