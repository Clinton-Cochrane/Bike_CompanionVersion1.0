package com.clintoncochrane.bikecompanion.ui.trip

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.core.content.ContextCompat
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clintoncochrane.bikecompanion.R
import com.clintoncochrane.bikecompanion.location.RideTrackingService
import com.clintoncochrane.bikecompanion.location.RideLocationPermission
import com.clintoncochrane.bikecompanion.ui.ride.ActiveRideActivity
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TripStartSplashScreen(
    navController: NavController,
    bikeId: Long,
    hadPlaceholdersAtStart: Boolean = false,
) {
    val viewModel: TripStartSplashViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val assignedBikeName by viewModel.assignedBikeName.collectAsState()
    val context = LocalContext.current
    var showPermissionLostDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.startTripEvents) {
        viewModel.startTripEvents.collectLatest {
            if (!RideLocationPermission.isGranted(context)) {
                showPermissionLostDialog = true
                return@collectLatest
            }
            ContextCompat.startForegroundService(
                context,
                Intent(context, RideTrackingService::class.java).apply {
                    putExtra(RideTrackingService.ACTION_KEY, RideTrackingService.ACTION_START)
                    putExtra(RideTrackingService.BIKE_ID_KEY, bikeId)
                    putExtra(RideTrackingService.HAD_PLACEHOLDERS_KEY, hadPlaceholdersAtStart)
                }
            )
            ActiveRideActivity.start(context, bikeId, hadPlaceholdersAtStart)
            navController.popBackStack()
        }
    }

    if (showPermissionLostDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.trip_location_permission_title)) },
            text = { Text(stringResource(R.string.trip_location_permission_lost)) },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionLostDialog = false
                        navController.popBackStack()
                    },
                ) {
                    Text(stringResource(R.string.common_back))
                }
            },
        )
    }

    val cancelContentDesc = stringResource(R.string.trip_splash_cancel_content_description)
    val countdownDesc = stringResource(R.string.trip_splash_countdown_content_description, state.countdown)
    val addTimeDesc = stringResource(R.string.trip_splash_add_ten_seconds_content_description)

    BackHandler {
        viewModel.cancel()
        navController.popBackStack()
    }

    Scaffold(
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.countdown.toString(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics {
                        contentDescription = countdownDesc
                    },
                )
                Text(
                    text = assignedBikeName?.let { stringResource(R.string.trip_splash_riding_bike, it) }
                        ?: stringResource(R.string.trip_splash_no_bike_assigned),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
                if (state.countdown > 0 && !state.isCancelled && !state.hasStarted) {
                    Button(
                        onClick = viewModel::addTenSeconds,
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .semantics { contentDescription = addTimeDesc },
                    ) {
                        Text(stringResource(R.string.trip_splash_add_ten_seconds))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        viewModel.cancel()
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = cancelContentDesc },
                ) {
                    Text(stringResource(R.string.trip_splash_cancel))
                }
            }
        }
    }
}
