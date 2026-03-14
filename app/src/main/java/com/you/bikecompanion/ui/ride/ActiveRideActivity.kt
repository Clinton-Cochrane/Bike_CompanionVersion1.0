package com.you.bikecompanion.ui.ride

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.you.bikecompanion.R
import com.you.bikecompanion.data.ride.RideEntity
import com.you.bikecompanion.data.ride.RideRepository
import com.you.bikecompanion.data.ride.RideSource
import com.you.bikecompanion.location.RideState
import com.you.bikecompanion.location.RideTrackingService
import com.you.bikecompanion.ui.theme.BikeCompanionTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActiveRideActivity : ComponentActivity() {

    @Inject
    lateinit var rideRepository: RideRepository

    private val rideStateFlow = MutableStateFlow(RideState())
    private val saveFailedEvents = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    private var boundService: RideTrackingService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            boundService = (service as RideTrackingService.LocalBinder).getService()
            lifecycleScope.launch {
                boundService?.rideState?.collect { rideStateFlow.value = it }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val bikeId = intent.getLongExtra(BIKE_ID_EXTRA, -1L)
        val hadPlaceholdersFromIntent = intent.getBooleanExtra(HAD_PLACEHOLDERS_EXTRA, false)
        setContent {
            BikeCompanionTheme {
                ActiveRideScreen(
                    bikeId = bikeId,
                    rideRepository = rideRepository,
                    onStopRide = { state ->
                        val hadPlaceholders = boundService?.rideState?.value?.hadPlaceholdersAtStart
                            ?: hadPlaceholdersFromIntent
                        stopRideAndSave(state, hadPlaceholders)
                    },
                    rideStateFlow = rideStateFlow.asStateFlow(),
                    saveFailedEvents = saveFailedEvents.asSharedFlow(),
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, RideTrackingService::class.java),
            connection,
            Context.BIND_AUTO_CREATE,
        )
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    private fun stopRideAndSave(state: RideState, hadPlaceholdersAtStart: Boolean) {
        if (state.startTimeMs <= 0 || !state.isTracking) {
            lifecycleScope.launch {
                saveFailedEvents.emit(Unit)
            }
            return
        }
        val bikeId = intent.getLongExtra(BIKE_ID_EXTRA, -1L)
        val endTime = System.currentTimeMillis()
        val currentPauseMs = if (state.isPaused && state.pausedAtMs > 0) endTime - state.pausedAtMs else 0L
        val totalPausedMs = state.totalPausedDurationMs + currentPauseMs
        val movingDurationMs = (endTime - state.startTimeMs - totalPausedMs).coerceAtLeast(0L)
        val ride = RideEntity(
            bikeId = if (bikeId >= 0) bikeId else null,
            distanceKm = state.distanceKm,
            durationMs = movingDurationMs,
            avgSpeedKmh = state.avgSpeedKmh,
            maxSpeedKmh = state.maxSpeedKmh,
            elevGainM = state.elevGainM,
            elevLossM = state.elevLossM,
            startedAt = state.startTimeMs,
            endedAt = endTime,
            source = RideSource.APP,
            hadPlaceholdersAtStart = hadPlaceholdersAtStart,
        )
        lifecycleScope.launch {
            rideRepository.saveRideAndUpdateBikeAndComponents(ride)
            startService(Intent(this@ActiveRideActivity, RideTrackingService::class.java).apply {
                putExtra(RideTrackingService.ACTION_KEY, RideTrackingService.ACTION_STOP)
            })
            finish()
        }
    }

    companion object {
        const val BIKE_ID_EXTRA = "bike_id"
        const val HAD_PLACEHOLDERS_EXTRA = "had_placeholders_at_start"

        fun start(context: Context, bikeId: Long, hadPlaceholdersAtStart: Boolean = false) {
            context.startActivity(Intent(context, ActiveRideActivity::class.java).apply {
                putExtra(BIKE_ID_EXTRA, bikeId)
                putExtra(HAD_PLACEHOLDERS_EXTRA, hadPlaceholdersAtStart)
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveRideScreen(
    bikeId: Long,
    rideRepository: RideRepository,
    onStopRide: (RideState) -> Unit,
    rideStateFlow: kotlinx.coroutines.flow.StateFlow<RideState>,
    saveFailedEvents: kotlinx.coroutines.flow.SharedFlow<Unit>,
) {
    val context = LocalContext.current
    val state by rideStateFlow.collectAsState()
    val tickState = remember { mutableStateOf(0) }
    DisposableEffect(Unit) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                tickState.value = tickState.value + 1
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(runnable, 1000)
        onDispose { handler.removeCallbacks(runnable) }
    }
    val tick = tickState.value
    val elapsedMovingMs = com.you.bikecompanion.util.computeElapsedMovingMs(state, System.currentTimeMillis())
    val snackbarHostState = remember { SnackbarHostState() }
    var showStopConfirm by remember { mutableStateOf(false) }
    val pauseLabel = stringResource(R.string.ride_pause)
    val resumeLabel = stringResource(R.string.ride_resume)

    LaunchedEffect(Unit) {
        saveFailedEvents.collect {
            snackbarHostState.showSnackbar(context.getString(R.string.ride_save_failed_tracking))
        }
    }
    LaunchedEffect(state.isPaused, state.wasAutoPausedDueToNoMovement) {
        if (state.isPaused && state.wasAutoPausedDueToNoMovement) {
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.ride_auto_paused_no_movement),
                withDismissAction = true,
            )
            context.startService(
                Intent(context, RideTrackingService::class.java).apply {
                    putExtra(RideTrackingService.ACTION_KEY, RideTrackingService.ACTION_CLEAR_AUTO_PAUSE_FLAG)
                },
            )
        }
    }

    val activity = context as? ComponentActivity
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.ride_active_title)) },
                navigationIcon = {
                    if (state.isPaused) {
                        IconButton(onClick = { activity?.finish() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back),
                            )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.ride_distance, state.distanceKm),
                style = MaterialTheme.typography.headlineMedium,
            )
            // key(tick) forces recomposition every second when active; when paused, elapsedMovingMs is static
            key(tick) {
                Text(
                    text = stringResource(
                        R.string.ride_duration,
                        com.you.bikecompanion.util.DurationFormatHelper.formatDurationBreakdownMs(elapsedMovingMs, capAt24h = false),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Text(
                text = stringResource(R.string.ride_speed_current, state.currentSpeedKmh),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.ride_speed_avg, state.avgSpeedKmh),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.ride_speed_max, "%.1f km/h".format(state.maxSpeedKmh)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.ride_elevation_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val elevNet = state.elevGainM - state.elevLossM
                val elevColor = when {
                    elevNet > 0 -> Color(0xFF2E7D32)
                    elevNet < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = "+%.0f / -%.0f m".format(state.elevGainM, state.elevLossM),
                    style = MaterialTheme.typography.bodySmall,
                    color = elevColor,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        val intent = Intent(context, RideTrackingService::class.java).apply {
                            putExtra(RideTrackingService.ACTION_KEY, if (state.isPaused) RideTrackingService.ACTION_RESUME else RideTrackingService.ACTION_PAUSE)
                        }
                        context.startService(intent)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.isPaused) resumeLabel else pauseLabel)
                }
                Button(
                    onClick = { showStopConfirm = true },
                    modifier = Modifier.weight(1f),
                    enabled = state.startTimeMs > 0 && state.isTracking,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.ride_stop))
                }
            }
            if (showStopConfirm) {
                AlertDialog(
                    onDismissRequest = { showStopConfirm = false },
                    title = { Text(stringResource(R.string.ride_stop_confirm_title)) },
                    text = { Text(stringResource(R.string.ride_stop_confirm_message)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showStopConfirm = false
                                onStopRide(state)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(stringResource(R.string.ride_stop))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStopConfirm = false }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    },
                )
            }
        }
    }
}
