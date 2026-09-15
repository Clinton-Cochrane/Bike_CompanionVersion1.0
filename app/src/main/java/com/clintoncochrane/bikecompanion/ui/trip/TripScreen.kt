package com.clintoncochrane.bikecompanion.ui.trip

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.health.connect.client.PermissionController
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clintoncochrane.bikecompanion.R
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.healthconnect.HEALTH_CONNECT_READ_PERMISSIONS
import com.clintoncochrane.bikecompanion.ui.navigation.Screen
import com.clintoncochrane.bikecompanion.ui.trip.HealthConnectImportResult
import com.clintoncochrane.bikecompanion.util.DisplayFormatHelper
import com.clintoncochrane.bikecompanion.util.DurationFormatHelper
import com.clintoncochrane.bikecompanion.util.RideDisplayHelper
import com.clintoncochrane.bikecompanion.location.RideTrackingService
import com.clintoncochrane.bikecompanion.location.RideLocationPermission
import com.clintoncochrane.bikecompanion.location.RideLocationPermissionAction
import com.clintoncochrane.bikecompanion.ui.ride.ActiveRideActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripScreen(
    navController: NavController,
    onStartRide: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = androidx.hilt.navigation.compose.hiltViewModel<TripViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val rideActiveBikeId by RideTrackingService.rideActiveBikeId.collectAsState(initial = -1L)
    val rideIsActive by RideTrackingService.rideIsActive.collectAsState(initial = false)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var hasRequestedLocationPermission by rememberSaveable { mutableStateOf(false) }
    var showLocationRationale by rememberSaveable { mutableStateOf(false) }
    var showLocationSettings by rememberSaveable { mutableStateOf(false) }
    var showManualMileageDialog by rememberSaveable { mutableStateOf(false) }

    fun beginRide() {
        val bikeId = uiState.selectedBike?.id ?: -1L
        val hadPlaceholders = uiState.placeholdersAddedThisSession
        navController.navigate(Screen.TripStartSplash.withId(bikeId, hadPlaceholders))
        viewModel.onRideStarted()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (RideLocationPermission.isFineLocationGranted(grants)) {
            beginRide()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.trip_location_permission_denied))
            }
        }
    }

    fun continueWithLocationPermission() {
        val shouldShowRationale = (context as? Activity)?.let { activity ->
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                RideLocationPermission.REQUIRED_PERMISSION,
            )
        } ?: false
        when (
            RideLocationPermission.nextAction(
                isGranted = RideLocationPermission.isGranted(context),
                hasRequestedPermission = hasRequestedLocationPermission,
                shouldShowRationale = shouldShowRationale,
            )
        ) {
            RideLocationPermissionAction.START_RIDE -> beginRide()
            RideLocationPermissionAction.SHOW_RATIONALE -> showLocationRationale = true
            RideLocationPermissionAction.OPEN_SETTINGS -> showLocationSettings = true
        }
    }

    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { grantedPermissions ->
        if (grantedPermissions.containsAll(HEALTH_CONNECT_READ_PERMISSIONS)) {
            viewModel.importFromHealthConnect()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.healthConnectImportResult.collect { result ->
            when (result) {
                is HealthConnectImportResult.Success ->
                    snackbarHostState.showSnackbar(
                        if (result.showDisclaimer) {
                            context.getString(R.string.trip_import_success_with_disclaimer, result.count)
                        } else {
                            context.getString(R.string.trip_import_success, result.count)
                        },
                    )
                HealthConnectImportResult.None ->
                    snackbarHostState.showSnackbar(context.getString(R.string.trip_import_none))
                HealthConnectImportResult.NoBikeSelected ->
                    snackbarHostState.showSnackbar(context.getString(R.string.trip_no_bike_selected))
                HealthConnectImportResult.Unavailable ->
                    snackbarHostState.showSnackbar(context.getString(R.string.trip_import_unavailable))
                HealthConnectImportResult.PermissionRequired -> {
                    val snackbarResult = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.trip_import_permission_required),
                        actionLabel = context.getString(R.string.trip_import_permission_action),
                        duration = SnackbarDuration.Long,
                    )
                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                        healthConnectPermissionLauncher.launch(HEALTH_CONNECT_READ_PERMISSIONS)
                    }
                }
                HealthConnectImportResult.ProviderUpdateRequired -> {
                    val snackbarResult = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.trip_import_provider_update_required),
                        actionLabel = context.getString(R.string.trip_import_provider_update_action),
                        duration = SnackbarDuration.Long,
                    )
                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                        openHealthConnectProviderListing(context)
                    }
                }
                HealthConnectImportResult.Error -> {
                    val snackbarResult = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.trip_import_error),
                        actionLabel = context.getString(R.string.trip_import_retry),
                        duration = SnackbarDuration.Long,
                    )
                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                        viewModel.importFromHealthConnect()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.manualMileageSaveResult.collect { result ->
            when (result) {
                ManualMileageSaveResult.Success -> {
                    showManualMileageDialog = false
                    snackbarHostState.showSnackbar(context.getString(R.string.trip_manual_mileage_success))
                }
                ManualMileageSaveResult.InvalidInput ->
                    snackbarHostState.showSnackbar(context.getString(R.string.trip_manual_mileage_invalid))
                ManualMileageSaveResult.Error ->
                    snackbarHostState.showSnackbar(context.getString(R.string.trip_manual_mileage_error))
            }
        }
    }

    fun startTrip() {
        scope.launch {
            val okToProceed = viewModel.checkMissingPartsBeforeStart()
            if (!okToProceed) return@launch
            continueWithLocationPermission()
        }
    }

    fun continueWithPermissions() {
        continueWithLocationPermission()
    }

    if (showLocationRationale) {
        AlertDialog(
            onDismissRequest = { showLocationRationale = false },
            title = { Text(stringResource(R.string.trip_location_permission_title)) },
            text = { Text(stringResource(R.string.trip_location_permission_rationale)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationRationale = false
                        hasRequestedLocationPermission = true
                        permissionLauncher.launch(RideLocationPermission.REQUEST_PERMISSIONS)
                    },
                ) {
                    Text(stringResource(R.string.trip_location_permission_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationRationale = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showLocationSettings) {
        AlertDialog(
            onDismissRequest = { showLocationSettings = false },
            title = { Text(stringResource(R.string.trip_location_permission_title)) },
            text = { Text(stringResource(R.string.trip_location_permission_settings)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationSettings = false
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                "package:${context.packageName}".toUri(),
                            ),
                        )
                    },
                ) {
                    Text(stringResource(R.string.trip_location_permission_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationSettings = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
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

    if (uiState.healthConnectImportReviews.isNotEmpty()) {
        HealthConnectImportReviewDialog(
            reviews = uiState.healthConnectImportReviews,
            bikes = uiState.bikes,
            isSaving = uiState.isSavingHealthConnectImports,
            onAssignBike = viewModel::assignBikeToHealthConnectImport,
            onSave = viewModel::saveReviewedHealthConnectImports,
            onCancel = viewModel::cancelHealthConnectImportReview,
        )
    }

    if (showManualMileageDialog) {
        ManualMileageDialog(
            bikes = uiState.bikes,
            initialBikeId = uiState.selectedBike?.id,
            isSaving = uiState.isSavingManualMileage,
            onSave = viewModel::saveManualMileage,
            onDismiss = { showManualMileageDialog = false },
        )
    }

    val visibleRideRows = remember(uiState.rides, uiState.bikes, uiState.rideHistory) {
        RideHistoryPresenter.toRows(
            rides = RideHistoryPresenter.visibleRides(uiState.rides, uiState.rideHistory),
            bikes = uiState.bikes,
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.nav_trip)) },
                actions = {
                    com.clintoncochrane.bikecompanion.ui.StartRideAction(onStartRide)
                },
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
            if (rideIsActive) {
                item(key = "current_ride") {
                    CurrentRideSection(
                        bikeName = uiState.bikes.find { it.id == rideActiveBikeId }?.name ?: "",
                        onViewRide = { ActiveRideActivity.start(context, rideActiveBikeId) },
                    )
                }
            }
            item(key = "rides_history_header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.ride_history_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.ride_history_total,
                            uiState.rides.size,
                            uiState.rides.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "rides_history_controls") {
                RideHistoryControls(
                    bikes = uiState.bikes,
                    state = uiState.rideHistory,
                    onBikeFilterSelected = viewModel::setRideHistoryBikeFilter,
                    onSortSelected = viewModel::setRideHistorySort,
                )
            }
            if (visibleRideRows.isEmpty()) {
                item(key = "past_rides_empty") {
                    Text(
                        text = if (uiState.rides.isEmpty()) {
                            stringResource(R.string.ride_history_empty)
                        } else {
                            stringResource(R.string.ride_history_filter_empty)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(visibleRideRows, key = { it.ride.id }) { row ->
                    RideCard(
                        row = row,
                        dismissedRideFlagIds = uiState.dismissedRideFlagIds,
                        dismissedPlaceholderReminderIds = uiState.dismissedPlaceholderReminderIds,
                        snoozedPlaceholderReminderUntilMs = uiState.snoozedPlaceholderReminderUntilMs,
                        onEditRide = {
                            navController.navigate(
                                com.clintoncochrane.bikecompanion.ui.navigation.Screen.EditRide.withId(row.ride.id),
                            )
                        },
                        onDismissAlert = { viewModel.dismissRideFlag(row.ride.id) },
                        onDeleteRide = { viewModel.deleteRide(row.ride) },
                        onEditBike = {
                            row.ride.bikeId?.let { bikeId ->
                                navController.navigate(
                                    com.clintoncochrane.bikecompanion.ui.navigation.Screen.BikeDetail.withId(bikeId),
                                )
                            }
                        },
                        onDismissPlaceholderReminder = { viewModel.dismissPlaceholderReminder(row.ride.id) },
                        onSnoozePlaceholderReminder = { viewModel.snoozePlaceholderReminder() },
                    )
                }
            }
            if (uiState.bikes.isNotEmpty()) {
                item(key = "ride_history_secondary_actions") {
                    RideHistorySecondaryActions(
                        onAddManualMileage = { showManualMileageDialog = true },
                        onImportFromHealthConnect = viewModel::importFromHealthConnect,
                    )
                }
            }
        }
    }
}

private fun openHealthConnectProviderListing(context: Context) {
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        "market://details?id=$HEALTH_CONNECT_PROVIDER_PACKAGE&url=healthconnect%3A%2F%2Fonboarding".toUri(),
    ).setPackage("com.android.vending")
    val browserIntent = Intent(
        Intent.ACTION_VIEW,
        "https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PROVIDER_PACKAGE".toUri(),
    )

    runCatching { context.startActivity(marketIntent) }
        .recoverCatching { context.startActivity(browserIntent) }
}

private const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

@Composable
private fun RideHistoryControls(
    bikes: List<com.clintoncochrane.bikecompanion.data.bike.BikeEntity>,
    state: RideHistoryUiState,
    onBikeFilterSelected: (Long?) -> Unit,
    onSortSelected: (RideHistorySort) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.ride_history_filter_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.bikeFilterId == null,
                onClick = { onBikeFilterSelected(null) },
                label = { Text(stringResource(R.string.ride_history_all_bikes)) },
            )
            bikes.forEach { bike ->
                FilterChip(
                    selected = state.bikeFilterId == bike.id,
                    onClick = { onBikeFilterSelected(bike.id) },
                    label = { Text(bike.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        Text(
            text = stringResource(R.string.ride_history_sort_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RideHistorySort.entries.forEach { sort ->
                FilterChip(
                    selected = state.sort == sort,
                    onClick = { onSortSelected(sort) },
                    label = { Text(stringResource(sort.labelRes)) },
                )
            }
        }
    }
}

private val RideHistorySort.labelRes: Int
    get() = when (this) {
        RideHistorySort.NEWEST -> R.string.ride_history_sort_newest
        RideHistorySort.DISTANCE -> R.string.ride_history_sort_distance
        RideHistorySort.DURATION -> R.string.ride_history_sort_duration
    }

@Composable
private fun RideHistorySecondaryActions(
    onAddManualMileage: () -> Unit,
    onImportFromHealthConnect: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.ride_history_add_title),
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(onClick = onAddManualMileage) {
            Text(stringResource(R.string.trip_add_manual_mileage))
        }
        TextButton(onClick = onImportFromHealthConnect) {
            Text(stringResource(R.string.trip_import_health_connect))
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
            } else {
                Text(
                    text = stringResource(R.string.trip_no_bike_selected),
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
    bikes: List<com.clintoncochrane.bikecompanion.data.bike.BikeEntity>,
    rideIsActive: Boolean,
    onStartTrip: () -> Unit,
    onViewCurrentTrip: () -> Unit,
    onAddManualMileage: () -> Unit,
    onImportFromHealthConnect: () -> Unit,
) {
    val startButtonDesc = stringResource(R.string.trip_start_button_content_description)
    val manualMileageDesc = stringResource(R.string.trip_add_manual_mileage_content_description)
    val importDesc = stringResource(R.string.trip_import_health_connect_content_description)
    val isRideActive = rideIsActive
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
        if (!isRideActive) {
            Text(
                text = stringResource(R.string.trip_start_ride_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (bikes.isNotEmpty()) {
            TextButton(
                onClick = onAddManualMileage,
                modifier = Modifier.semantics { contentDescription = manualMileageDesc },
            ) {
                Text(stringResource(R.string.trip_add_manual_mileage))
            }
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
private fun ManualMileageDialog(
    bikes: List<com.clintoncochrane.bikecompanion.data.bike.BikeEntity>,
    initialBikeId: Long?,
    isSaving: Boolean,
    onSave: (Long?, Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedBikeId by rememberSaveable { mutableStateOf(initialBikeId) }
    var distanceText by rememberSaveable { mutableStateOf("") }
    val distanceKm = distanceText.trim().toDoubleOrNull()
    val isDistanceValid = distanceKm != null && distanceKm.isFinite() && distanceKm > 0.0

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.trip_manual_mileage_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.trip_manual_mileage_message))
                Text(
                    text = stringResource(R.string.trip_select_bike),
                    style = MaterialTheme.typography.labelLarge,
                )
                bikes.forEach { bike ->
                    FilterChip(
                        selected = selectedBikeId == bike.id,
                        onClick = { selectedBikeId = bike.id },
                        label = { Text(bike.name) },
                        enabled = !isSaving,
                    )
                }
                OutlinedTextField(
                    value = distanceText,
                    onValueChange = { distanceText = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true,
                    label = { Text(stringResource(R.string.trip_manual_mileage_distance)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = distanceText.isNotBlank() && !isDistanceValid,
                    supportingText = if (distanceText.isNotBlank() && !isDistanceValid) {
                        { Text(stringResource(R.string.trip_manual_mileage_invalid)) }
                    } else {
                        null
                    },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedBikeId, distanceKm) },
                enabled = selectedBikeId != null && isDistanceValid && !isSaving,
            ) {
                Text(stringResource(R.string.trip_manual_mileage_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun HealthConnectImportReviewDialog(
    reviews: List<HealthConnectImportReview>,
    bikes: List<com.clintoncochrane.bikecompanion.data.bike.BikeEntity>,
    isSaving: Boolean,
    onAssignBike: (String, Long) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = { if (!isSaving) onCancel() },
        title = { Text(stringResource(R.string.trip_import_review_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.trip_import_review_message))
                reviews.forEach { review ->
                    val session = review.session
                    val recordId = requireNotNull(session.healthConnectRecordId)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(
                                R.string.trip_import_review_ride,
                                dateFormat.format(Date(session.startTimeMs)),
                                requireNotNull(session.distanceKm),
                            ),
                        )
                        bikes.forEach { bike ->
                            FilterChip(
                                selected = review.bikeId == bike.id,
                                onClick = { onAssignBike(recordId, bike.id) },
                                label = { Text(stringResource(R.string.trip_import_review_assign_bike, bike.name)) },
                                enabled = !isSaving,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !isSaving) {
                Text(stringResource(R.string.trip_import_review_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isSaving) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun RideCard(
    row: RideHistoryRow,
    dismissedRideFlagIds: Set<Long>,
    dismissedPlaceholderReminderIds: Set<Long>,
    snoozedPlaceholderReminderUntilMs: Long?,
    onEditRide: () -> Unit,
    onDismissAlert: () -> Unit,
    onDeleteRide: () -> Unit,
    onEditBike: () -> Unit,
    onDismissPlaceholderReminder: () -> Unit,
    onSnoozePlaceholderReminder: () -> Unit,
) {
    val ride = row.ride
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val flagReason = RideDisplayHelper.getRideFlagReason(ride)
    var showReviewDialog by remember { mutableStateOf(false) }
    var showPlaceholderReminderDialog by remember { mutableStateOf(false) }

    if (showReviewDialog && flagReason != null) {
        RideReviewDialog(
            ride = ride,
            flagReason = flagReason,
            onEditTrip = onEditRide,
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditRide),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (row.hasAssignedBike) {
                    stringResource(
                        R.string.ride_history_summary_assigned,
                        dateFormat.format(Date(ride.endedAt)),
                        ride.distanceKm,
                        requireNotNull(row.bikeName),
                        DurationFormatHelper.formatDurationBreakdownMs(
                            ride.durationMs,
                            over24hPlaceholder = stringResource(R.string.ride_duration_over_24h),
                        ),
                    )
                } else {
                    stringResource(
                        R.string.ride_history_summary_unassigned,
                        dateFormat.format(Date(ride.endedAt)),
                        ride.distanceKm,
                        DurationFormatHelper.formatDurationBreakdownMs(
                            ride.durationMs,
                            over24hPlaceholder = stringResource(R.string.ride_duration_over_24h),
                        ),
                    )
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
