package com.clintoncochrane.bikecompanion.ui.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.clintoncochrane.bikecompanion.R
import com.clintoncochrane.bikecompanion.data.ride.RideEntity
import com.clintoncochrane.bikecompanion.data.ride.RideSource
import com.clintoncochrane.bikecompanion.ui.navigation.Screen
import com.clintoncochrane.bikecompanion.util.DurationFormatHelper
import com.clintoncochrane.bikecompanion.util.RideDisplayHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
) {
    val viewModel: RideDetailViewModel = hiltViewModel(viewModelStoreOwner = backStackEntry)
    val uiState by viewModel.uiState.collectAsState()
    val backContentDesc = stringResource(R.string.common_back_content_description)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.ride_detail_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.semantics { contentDescription = backContentDesc },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { paddingValues ->
        when {
            uiState.loading -> Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }

            uiState.ride == null -> Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.ride_detail_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                val ride = requireNotNull(uiState.ride)
                RideDetails(
                    ride = ride,
                    bikeName = uiState.bike?.name,
                    onEditRide = {
                        navController.navigate(Screen.EditRide.withId(ride.id))
                    },
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun RideDetails(
    ride: RideEntity,
    bikeName: String?,
    onEditRide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())
    val unavailable = stringResource(R.string.ride_stat_unavailable)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(dateFormat.format(Date(ride.endedAt)), style = MaterialTheme.typography.titleLarge)
        RideDetailRow(R.string.ride_detail_bike, bikeName ?: stringResource(R.string.ride_no_bike_assigned_short))
        RideDetailRow(R.string.ride_detail_distance, stringResource(R.string.trip_ride_distance, ride.distanceKm))
        RideDetailRow(
            R.string.ride_detail_duration,
            DurationFormatHelper.formatDurationBreakdownMs(
                ride.durationMs,
                over24hPlaceholder = stringResource(R.string.ride_duration_over_24h),
            ),
        )
        RideDetailRow(R.string.ride_detail_average_speed, stringResource(R.string.ride_speed_avg, ride.avgSpeedKmh))
        RideDetailRow(
            R.string.ride_detail_max_speed,
            RideDisplayHelper.formatMaxSpeedKmh(ride.maxSpeedKmh, ride.source, unavailable),
        )
        RideDetailRow(
            R.string.ride_detail_elevation_gain,
            RideDisplayHelper.formatElevationGainLoss(ride.elevGainM, ride.elevLossM, ride.source, unavailable),
        )
        RideDetailRow(R.string.ride_detail_source, stringResource(ride.source.labelRes))
        Button(onClick = onEditRide, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ride_detail_edit_action))
        }
    }
}

@Composable
private fun RideDetailRow(labelRes: Int, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

private val RideSource.labelRes: Int
    get() = when (this) {
        RideSource.APP -> R.string.ride_detail_source_app
        RideSource.HEALTH_CONNECT -> R.string.ride_detail_source_health_connect
        RideSource.MANUAL -> R.string.ride_detail_source_manual
    }
