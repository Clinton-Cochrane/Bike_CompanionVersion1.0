package com.you.bikecompanion.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icon
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.you.bikecompanion.R
import com.you.bikecompanion.ui.navigation.Screen
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.component.DefaultComponentTypes
import com.you.bikecompanion.data.ride.RideEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BikeDetailScreen(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
) {
    var showAddComponentDialog by remember { mutableStateOf(false) }
    val viewModel: BikeDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        viewModelStoreOwner = backStackEntry,
    )
    val uiState by viewModel.uiState.collectAsState()

    if (showAddComponentDialog) {
        AlertDialog(
            onDismissRequest = { showAddComponentDialog = false },
            title = { Text(stringResource(R.string.component_suggested_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DefaultComponentTypes.SUGGESTED.forEach { suggested ->
                        TextButton(
                            onClick = {
                                val name = suggested.type.replaceFirstChar { it.uppercase() }
                                viewModel.addComponent(suggested.type, name, suggested.defaultLifespanKm)
                                showAddComponentDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "${suggested.type.replaceFirstChar { it.uppercase() }} — ${stringResource(R.string.component_lifespan_km, suggested.defaultLifespanKm)}",
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddComponentDialog = false }) {
                    Text(stringResource(R.string.component_done))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.bike?.name ?: stringResource(R.string.garage_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.semantics { contentDescription = stringResource(R.string.common_back_content_description) },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (uiState.bike != null) {
                        IconButton(
                            onClick = { navController.navigate(Screen.EditBike.withId(uiState.bike!!.id)) },
                            modifier = Modifier.semantics { contentDescription = stringResource(R.string.common_edit) },
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { paddingValues ->
        if (uiState.bike == null && !uiState.loading) {
            Text(
                stringResource(R.string.common_error),
                modifier = Modifier.padding(paddingValues).padding(16.dp),
            )
            return@Scaffold
        }
        val bike = uiState.bike ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(bike.name, style = MaterialTheme.typography.titleLarge)
                        if (bike.make.isNotEmpty() || bike.model.isNotEmpty()) {
                            Text(
                                listOf(bike.make, bike.model).filter { it.isNotEmpty() }.joinToString(" "),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(
                            stringResource(R.string.garage_bike_total_km, bike.totalDistanceKm),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.garage_components),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = { showAddComponentDialog = true }) {
                        Text(stringResource(R.string.bike_add_component))
                    }
                }
            }
            items(uiState.components, key = { it.id }) { component ->
                ComponentHealthCard(
                    component = component,
                    onMarkReplaced = { viewModel.markComponentReplaced(component) },
                    onSnooze = { viewModel.snoozeComponent(component, 500.0) },
                    onAlertsOff = { viewModel.turnOffAlerts(component) },
                )
            }
            item {
                Text(
                    stringResource(R.string.garage_rides),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(uiState.rides, key = { it.id }) { ride ->
                RideSummaryCard(ride = ride)
            }
        }
    }
}

@Composable
private fun ComponentHealthCard(component: ComponentEntity) {
    val healthPercent = (100.0 - (component.distanceUsedKm / component.lifespanKm).coerceIn(0.0, 1.0) * 100).toInt().coerceIn(0, 100)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = component.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.bike_component_used_km, component.distanceUsedKm, component.lifespanKm),
                style = MaterialTheme.typography.bodySmall,
            )
            LinearProgressIndicator(
                progress = { healthPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .semantics { contentDescription = stringResource(R.string.bike_component_health, healthPercent) },
            )
            Text(
                text = stringResource(R.string.bike_component_health, healthPercent),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun RideSummaryCard(ride: RideEntity) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(dateFormat.format(Date(ride.endedAt)), style = MaterialTheme.typography.labelMedium)
            Text(stringResource(R.string.trip_ride_distance, ride.distanceKm), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
