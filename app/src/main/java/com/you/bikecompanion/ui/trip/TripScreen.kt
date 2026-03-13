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
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.you.bikecompanion.R
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.ride.RideEntity
import com.you.bikecompanion.ui.navigation.Screen
import com.you.bikecompanion.ui.trip.HealthConnectImportResult
import com.you.bikecompanion.util.DisplayFormatHelper
import com.you.bikecompanion.util.DurationFormatHelper
import com.you.bikecompanion.util.RideDisplayHelper
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
                navController.navigate(
                    Screen.TripStartSplash.withId(bikeId, uiState.placeholdersAddedThisSession),
                )
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.healthConnectImportResult.collect { result ->
            val message = when (result) {
                is HealthConnectImportResult.Success ->
                    if (result.showDisclaimer) {
                        context.getString(R.string.trip_import_success_with_disclaimer, result.count)
                    } else {
                        context.getString(R.string.trip_import_success, result.count)
                    }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (rideActiveBikeId >= 0) {
                item(key = "current_ride") {
                    CurrentRideSection(
                        bikeName = uiState.bikes.find { it.id == rideActiveBikeId }?.name ?: "",
                        onViewRide = { ActiveRideActivity.start(context, rideActiveBikeId) },
                    )
                }
            }
            item(key = "start_trip") {
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
            }
            item(key = "past_rides_header") {
                Text(
                    text = stringResource(R.string.trip_past_rides),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (uiState.rides.isEmpty()) {
                item(key = "past_rides_empty") {
                    Text(
                        text = stringResource(R.string.trip_no_rides),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(uiState.rides, key = { it.id }) { ride ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        RideCard(
                            ride = ride,
                            bikeName = ride.bikeId?.let { uiState.bikes.associateBy { b -> b.id }[it]?.name } ?: "",
                            dismissedRideFlagIds = uiState.dismissedRideFlagIds,
                            dismissedPlaceholderReminderIds = uiState.dismissedPlaceholderReminderIds,
                            snoozedPlaceholderReminderUntilMs = uiState.snoozedPlaceholderReminderUntilMs,
                            onEditTrip = { navController.navigate(com.you.bikecompanion.ui.navigation.Screen.EditRide.withId(ride.id)) },
                            onDismissAlert = { viewModel.dismissRideFlag(ride.id) },
                            onDeleteRide = { viewModel.deleteRide(ride) },
                            onEditBike = {
                                ride.bikeId?.let { bikeId ->
                                    navController.navigate(com.you.bikecompanion.ui.navigation.Screen.BikeDetail.withId(bikeId))
                                }
                            },
                            onDismissPlaceholderReminder = { viewModel.dismissPlaceholderReminder(ride.id) },
                            onSnoozePlaceholderReminder = { viewModel.snoozePlaceholderReminder() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentRideSection(
    bikeName: String,
    onViewRide: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.ride_active_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (bikeName.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.trip_ride_bike, bikeName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Button(
                onClick = onViewRide,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                Text(stringResource(R.string.trip_view_current_trip))
            }
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
private fun RideCard(
    ride: RideEntity,
    bikeName: String,
    dismissedRideFlagIds: Set<Long>,
    dismissedPlaceholderReminderIds: Set<Long>,
    snoozedPlaceholderReminderUntilMs: Long?,
    onEditTrip: () -> Unit,
    onDismissAlert: () -> Unit,
    onDeleteRide: () -> Unit,
    onEditBike: () -> Unit,
    onDismissPlaceholderReminder: () -> Unit,
    onSnoozePlaceholderReminder: () -> Unit,
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
    val flagReason = RideDisplayHelper.getRideFlagReason(ride)
    var showReviewDialog by remember { mutableStateOf(false) }
    var showPlaceholderReminderDialog by remember { mutableStateOf(false) }

    if (showReviewDialog && flagReason != null) {
        RideReviewDialog(
            ride = ride,
            flagReason = flagReason,
            onEditTrip = onEditTrip,
            onDismissAlert = onDismissAlert,
            onDeleteRide = onDeleteRide,
            onDismiss = { showReviewDialog = false },
        )
    }

    if (showPlaceholderReminderDialog) {
        PlaceholderReminderDialog(
            ride = ride,
            onEditBike = onEditBike,
            onSnooze = onSnoozePlaceholderReminder,
            onDismiss = onDismissPlaceholderReminder,
            onClose = { showPlaceholderReminderDialog = false },
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dateFormat.format(Date(ride.endedAt)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (RideDisplayHelper.shouldShowReviewChip(ride, dismissedRideFlagIds)) {
                        FilterChip(
                            selected = false,
                            onClick = { showReviewDialog = true },
                            label = { Text(stringResource(R.string.ride_review)) },
                        )
                    }
                    if (RideDisplayHelper.shouldShowPlaceholderReminderChip(
                            ride,
                            dismissedPlaceholderReminderIds,
                            snoozedPlaceholderReminderUntilMs,
                        )) {
                        FilterChip(
                            selected = false,
                            onClick = { showPlaceholderReminderDialog = true },
                            label = { Text(stringResource(R.string.ride_placeholder_reminder)) },
                        )
                    }
                }
            }
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
                text = stringResource(
                    R.string.trip_ride_duration,
                    DurationFormatHelper.formatDurationBreakdownMs(
                        ride.durationMs,
                        over24hPlaceholder = stringResource(R.string.ride_duration_over_24h),
                    ),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val unavailable = stringResource(R.string.ride_stat_unavailable)
                Text(
                    text = stringResource(
                        R.string.ride_speed_max,
                        RideDisplayHelper.formatMaxSpeedKmh(ride.maxSpeedKmh, ride.source, unavailable),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val elevValue = RideDisplayHelper.formatElevationGainLoss(
                    ride.elevGainM, ride.elevLossM, ride.source, unavailable,
                )
                val elevNet = RideDisplayHelper.elevationNetDelta(ride.elevGainM, ride.elevLossM)
                val elevColor = when {
                    RideDisplayHelper.isElevationUnavailable(ride.elevGainM, ride.elevLossM, ride.source) ->
                        MaterialTheme.colorScheme.onSurfaceVariant
                    elevNet > 0 -> Color(0xFF2E7D32)
                    elevNet < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.ride_elevation_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = elevValue,
                        style = MaterialTheme.typography.labelMedium,
                        color = elevColor,
                    )
                }
            }
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

