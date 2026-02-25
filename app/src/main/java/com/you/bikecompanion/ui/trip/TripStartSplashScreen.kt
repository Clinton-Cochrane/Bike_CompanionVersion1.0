package com.you.bikecompanion.ui.trip

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.you.bikecompanion.R
import com.you.bikecompanion.location.RideTrackingService
import com.you.bikecompanion.ui.navigation.Screen
import com.you.bikecompanion.ui.ride.ActiveRideActivity
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripStartSplashScreen(
    navController: NavController,
    bikeId: Long,
) {
    val viewModel: TripStartSplashViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    if (bikeId < 0) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    LaunchedEffect(viewModel.startTripEvents) {
        viewModel.startTripEvents.collectLatest {
            context.startService(
                Intent(context, RideTrackingService::class.java).apply {
                    putExtra(RideTrackingService.ACTION_KEY, RideTrackingService.ACTION_START)
                    putExtra(RideTrackingService.BIKE_ID_KEY, bikeId)
                }
            )
            ActiveRideActivity.start(context, bikeId)
            navController.popBackStack()
        }
    }

    val cancelContentDesc = stringResource(R.string.trip_splash_cancel_content_description)
    val countdownDesc = stringResource(R.string.trip_splash_countdown_content_description, state.countdown)
    val goNowDesc = stringResource(R.string.trip_splash_go_now_content_description)
    val tripSettingsDesc = stringResource(R.string.trip_settings_title)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.trip_splash_title)) },
                actions = {
                    IconButton(
                        onClick = { navController.navigate(Screen.TripSettings.route) },
                        modifier = Modifier.semantics { contentDescription = tripSettingsDesc },
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
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
                    text = if (state.countdown > 0) state.countdown.toString() else "Go!",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics {
                        contentDescription = countdownDesc
                    },
                )
                if (state.countdown > 0 && !state.isCancelled) {
                    Button(
                        onClick = { viewModel.goNow() },
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .semantics { contentDescription = goNowDesc },
                    ) {
                        Text(stringResource(R.string.trip_splash_go_now))
                    }
                }
            }

            val bikeModeDesc = stringResource(R.string.trip_splash_action_bike_mode)
            val autoPauseDesc = stringResource(R.string.trip_splash_action_auto_pause)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                ) {
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.semantics {
                            contentDescription = bikeModeDesc
                        },
                    ) {
                        Text(bikeModeDesc)
                    }
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.semantics {
                            contentDescription = autoPauseDesc
                        },
                    ) {
                        Text(autoPauseDesc)
                    }
                }
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
