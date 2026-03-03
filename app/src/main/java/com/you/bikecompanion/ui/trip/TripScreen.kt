package com.you.bikecompanion.ui.trip

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.you.bikecompanion.R
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.ride.RideEntity
import com.you.bikecompanion.ui.navigation.Screen
import com.you.bikecompanion.ui.trip.HealthConnectImportResult
import com.you.bikecompanion.util.DisplayFormatHelper
import com.you.bikecompanion.util.DurationFormatHelper
import com.you.bikecompanion.location.RideTrackingService
import com.you.bikecompanion.ui.ride.ActiveRideActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val viewModel = androidx.hilt.navigation.compose.hiltViewModel<TripViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val rideActiveBikeId by RideTrackingService.rideActiveBikeId.collectAsState(initial = -1L)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) {
            val bikeId = uiState.selectedBike?.id ?: -1L
            if (bikeId >= 0) {
                navController.navigate(Screen.TripStartSplash.withId(bikeId))
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.healthConnectImportResult.collect { result ->
            val message = when (result) {
                is HealthConnectImportResult.Success ->
                    context.getString(R.string.trip_import_success, result.count)
                HealthConnectImportResult.None ->
                    context.getString(R.string.trip_import_none)
                HealthConnectImportResult.NoBikeSelected ->
                    context.getString(R.string.trip_no_bike_selected)
                HealthConnectImportResult.Error ->
                    context.getString(R.string.trip_import_error)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    fun startTrip() {
        val bikeId = uiState.selectedBike?.id
        if (uiState.bikes.isEmpty()) return
        scope.launch {
            val okToProceed = viewModel.checkMissingPartsBeforeStart()
            if (!okToProceed) return@launch
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
                )
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            }
        }
    }

    fun continueWithPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
            )
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    val missingParts = uiState.missingParts
    if (missingParts != null) {
        MissingPartsDialog(
            missingParts = missingParts,
            onAddPlaceholder = { viewModel.addPlaceholderFor(it) },
            onAddAllPlaceholders = {
                viewModel.addAllPlaceholders()
                continueWithPermissions()
            },
            onInstallFromGarage = { viewModel.installFromGarage(it) },
            onStartAnyway = {
                viewModel.clearMissingParts()
                continueWithPermissions()
            },
            onDismiss = { viewModel.clearMissingParts() },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.nav_trip)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            StartTripSection(
                selectedBike = uiState.selectedBike,
                bikes = uiState.bikes,
                rideActiveBikeId = rideActiveBikeId,
                onStartTrip = { startTrip() },
                onViewCurrentTrip = {
                    if (rideActiveBikeId >= 0) ActiveRideActivity.start(context, rideActiveBikeId)
                },
                onSelectBike = viewModel::selectBike,
                onImportFromHealthConnect = { viewModel.importFromHealthConnect() },
            )
            PastRidesSection(
                rides = uiState.rides,
                bikeNames = uiState.bikes.associateBy { it.id },
            )
        }
    }
}

@Composable
private fun StartTripSection(
    selectedBike: com.you.bikecompanion.data.bike.BikeEntity?,
    bikes: List<com.you.bikecompanion.data.bike.BikeEntity>,
    rideActiveBikeId: Long,
    onStartTrip: () -> Unit,
    onViewCurrentTrip: () -> Unit,
    onSelectBike: (com.you.bikecompanion.data.bike.BikeEntity?) -> Unit,
    onImportFromHealthConnect: () -> Unit,
) {
    val startButtonDesc = stringResource(R.string.trip_start_button_content_description)
    val importDesc = stringResource(R.string.trip_import_health_connect_content_description)
    val isRideActive = rideActiveBikeId >= 0
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = if (isRideActive) onViewCurrentTrip else onStartTrip,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = startButtonDesc }
                .minimumInteractiveComponentSize(),
            contentPadding = PaddingValues(vertical = 20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(
                imageVector = Icons.Filled.DirectionsBike,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = if (isRideActive) stringResource(R.string.trip_view_current_trip) else stringResource(R.string.trip_start_button),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        if (bikes.isNotEmpty()) {
            Text(
                text = selectedBike?.name?.let { stringResource(R.string.trip_ride_bike, it) }
                    ?: stringResource(R.string.trip_no_bike_selected),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Text(
                text = stringResource(R.string.trip_add_bike_first),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (bikes.isNotEmpty() && selectedBike != null) {
            TextButton(
                onClick = onImportFromHealthConnect,
                modifier = Modifier.semantics { contentDescription = importDesc },
            ) {
                Text(stringResource(R.string.trip_import_health_connect))
            }
        }
    }
}

@Composable
private fun PastRidesSection(
    rides: List<RideEntity>,
    bikeNames: Map<Long, com.you.bikecompanion.data.bike.BikeEntity>,
) {
    Text(
        text = stringResource(R.string.trip_past_rides),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    if (rides.isEmpty()) {
        Text(
            text = stringResource(R.string.trip_no_rides),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rides, key = { it.id }) { ride ->
                RideCard(
                    ride = ride,
                    bikeName = ride.bikeId?.let { bikeNames[it]?.name } ?: "",
                )
            }
        }
    }
}

@Composable
private fun RideCard(
    ride: RideEntity,
    bikeName: String,
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = dateFormat.format(Date(ride.endedAt)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.trip_ride_distance, ride.distanceKm),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (bikeName.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.trip_ride_bike, bikeName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(R.string.trip_ride_duration, DurationFormatHelper.formatDurationBreakdownMs(ride.durationMs)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MissingPartsDialog(
    missingParts: List<MissingPartInfo>,
    onAddPlaceholder: (MissingPartInfo) -> Unit,
    onAddAllPlaceholders: () -> Unit,
    onInstallFromGarage: (ComponentEntity) -> Unit,
    onStartAnyway: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trip_missing_parts_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.trip_missing_parts_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                missingParts.forEach { info ->
                    val displayName = DisplayFormatHelper.formatComponentTypeForDisplay(info.expected.type) +
                        if (info.expected.position != "none") " (${info.expected.position})" else ""
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TextButton(onClick = { onAddPlaceholder(info) }) {
                                Text(stringResource(R.string.trip_add_placeholder))
                            }
                            info.garageMatches.forEach { comp ->
                                TextButton(onClick = { onInstallFromGarage(comp) }) {
                                    Text(
                                        stringResource(R.string.trip_install_from_garage, DisplayFormatHelper.formatForDisplay(comp.name)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onAddAllPlaceholders) {
                    Text(stringResource(R.string.trip_add_all_placeholders))
                }
                TextButton(onClick = onStartAnyway) {
                    Text(stringResource(R.string.trip_start_anyway))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

