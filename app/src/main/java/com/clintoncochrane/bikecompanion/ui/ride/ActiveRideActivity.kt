package com.clintoncochrane.bikecompanion.ui.ride

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
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
import com.clintoncochrane.bikecompanion.R
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.bike.BikeRepository
import com.clintoncochrane.bikecompanion.data.ride.RideEntity
import com.clintoncochrane.bikecompanion.data.ride.RideRepository
import com.clintoncochrane.bikecompanion.data.ride.RideSaveResult
import com.clintoncochrane.bikecompanion.data.ride.RideSource
import com.clintoncochrane.bikecompanion.location.RideState
import com.clintoncochrane.bikecompanion.location.RideTrackingService
import com.clintoncochrane.bikecompanion.ui.theme.BikeCompanionTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class PendingStopRide(
    val state: RideState,
    val hadPlaceholdersAtStart: Boolean,
    val endedAtMs: Long,
)

@AndroidEntryPoint
class ActiveRideActivity : ComponentActivity() {

    @Inject
    lateinit var rideRepository: RideRepository

    @Inject
    lateinit var bikeRepository: BikeRepository

    private val rideStateFlow = MutableStateFlow(RideState())
    private val bikesFlow = MutableStateFlow<List<BikeEntity>>(emptyList())
    private val pendingStopRideFlow = MutableStateFlow<PendingStopRide?>(null)
    private val saveFailedEvents = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    private val assignmentFailedEvents = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    private val saveInProgressFlow = MutableStateFlow(false)
    private var notificationStopRequested = false
    private val completionRequestGuard = RideCompletionRequestGuard()
    private var boundService: RideTrackingService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            boundService = (service as RideTrackingService.LocalBinder).getService()
            rideStateFlow.value = boundService?.rideState?.value ?: RideState()
            handleNotificationStopRequest()
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
        notificationStopRequested = intent.getBooleanExtra(REQUEST_STOP_EXTRA, false)
        val hadPlaceholdersFromIntent = intent.getBooleanExtra(HAD_PLACEHOLDERS_EXTRA, false)
        lifecycleScope.launch {
            bikeRepository.getAllBikes().collect { bikesFlow.value = it }
        }
        setContent {
            BikeCompanionTheme {
                ActiveRideScreen(
                    onStopRide = { state ->
                        val hadPlaceholders = boundService?.rideState?.value?.hadPlaceholdersAtStart
                            ?: hadPlaceholdersFromIntent
                        requestStopRide(state, hadPlaceholders)
                    },
                    bikesFlow = bikesFlow.asStateFlow(),
                    pendingStopRideFlow = pendingStopRideFlow.asStateFlow(),
                    isSavingFlow = saveInProgressFlow.asStateFlow(),
                    onAssignBike = ::assignBikeDuringRide,
                    onAssignBikeAndSave = ::assignBikeAndSave,
                    onCancelPendingStop = {
                        pendingStopRideFlow.value = null
                        completionRequestGuard.reset()
                    },
                    rideStateFlow = rideStateFlow.asStateFlow(),
                    saveFailedEvents = saveFailedEvents.asSharedFlow(),
                    assignmentFailedEvents = assignmentFailedEvents.asSharedFlow(),
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(REQUEST_STOP_EXTRA, false)) {
            notificationStopRequested = true
            handleNotificationStopRequest()
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

    private fun requestStopRide(state: RideState, hadPlaceholdersAtStart: Boolean) {
        if (state.startTimeMs <= 0 || !state.isTracking) {
            lifecycleScope.launch {
                saveFailedEvents.emit(Unit)
            }
            return
        }
        if (!completionRequestGuard.tryStart()) return
        val pendingRide = PendingStopRide(
            state = state,
            hadPlaceholdersAtStart = hadPlaceholdersAtStart,
            endedAtMs = System.currentTimeMillis(),
        )
        when (RideCompletionPolicy.destinationFor(state)) {
            RideCompletionDestination.ASSIGN_BIKE -> pendingStopRideFlow.value = pendingRide
            RideCompletionDestination.SAVE -> saveRide(pendingRide, state.bikeId)
        }
    }

    private fun handleNotificationStopRequest() {
        val service = boundService ?: return
        if (!notificationStopRequested) return
        notificationStopRequested = false
        intent.removeExtra(REQUEST_STOP_EXTRA)
        requestStopRide(service.rideState.value, service.rideState.value.hadPlaceholdersAtStart)
    }

    private fun assignBikeDuringRide(bikeId: Long) {
        if (boundService?.assignBike(bikeId) != true) {
            assignmentFailedEvents.tryEmit(Unit)
        }
    }

    private fun assignBikeAndSave(bikeId: Long) {
        val pendingRide = pendingStopRideFlow.value ?: return
        val service = boundService
        val assignmentAccepted = service?.rideState?.value?.bikeId == bikeId || service?.assignBike(bikeId) == true
        if (!assignmentAccepted) {
            assignmentFailedEvents.tryEmit(Unit)
            return
        }
        saveRide(pendingRide, bikeId)
    }

    private fun saveRide(pendingRide: PendingStopRide, bikeId: Long) {
        if (saveInProgressFlow.value) return
        saveInProgressFlow.value = true
        val state = pendingRide.state
        val endTime = pendingRide.endedAtMs
        val currentPauseMs = if (state.isPaused && state.pausedAtMs > 0) endTime - state.pausedAtMs else 0L
        val totalPausedMs = state.totalPausedDurationMs + currentPauseMs
        val movingDurationMs = (endTime - state.startTimeMs - totalPausedMs).coerceAtLeast(0L)
        val ride = RideEntity(
            bikeId = bikeId,
            distanceKm = state.distanceKm,
            durationMs = movingDurationMs,
            avgSpeedKmh = state.avgSpeedKmh,
            maxSpeedKmh = state.maxSpeedKmh,
            elevGainM = state.elevGainM,
            elevLossM = state.elevLossM,
            startedAt = state.startTimeMs,
            endedAt = endTime,
            source = RideSource.APP,
            hadPlaceholdersAtStart = pendingRide.hadPlaceholdersAtStart,
        )
        lifecycleScope.launch {
            try {
                when (rideRepository.saveRideAndUpdateBikeAndComponents(ride)) {
                    RideSaveResult.SAVED -> {
                        pendingStopRideFlow.value = null
                        stopTrackingAndFinish()
                    }
                    RideSaveResult.DISCARDED_EMPTY -> {
                        pendingStopRideFlow.value = null
                        Toast.makeText(
                            this@ActiveRideActivity,
                            getString(R.string.ride_discarded_zero_distance),
                            Toast.LENGTH_LONG,
                        ).show()
                        stopTrackingAndFinish()
                    }
                    RideSaveResult.REJECTED_INVALID -> {
                        pendingStopRideFlow.value = null
                        Toast.makeText(
                            this@ActiveRideActivity,
                            getString(R.string.ride_save_rejected_invalid),
                            Toast.LENGTH_LONG,
                        ).show()
                        stopTrackingAndFinish()
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                completionRequestGuard.reset()
                saveFailedEvents.tryEmit(Unit)
            } finally {
                saveInProgressFlow.value = false
            }
        }
    }

    private fun stopTrackingAndFinish() {
        startService(Intent(this, RideTrackingService::class.java).apply {
            putExtra(RideTrackingService.ACTION_KEY, RideTrackingService.ACTION_STOP)
        })
        finish()
    }

    companion object {
        const val BIKE_ID_EXTRA = "bike_id"
        const val HAD_PLACEHOLDERS_EXTRA = "had_placeholders_at_start"
        const val REQUEST_STOP_EXTRA = "request_stop"

        fun start(context: Context, bikeId: Long, hadPlaceholdersAtStart: Boolean = false) {
            context.startActivity(Intent(context, ActiveRideActivity::class.java).apply {
                putExtra(BIKE_ID_EXTRA, bikeId)
                putExtra(HAD_PLACEHOLDERS_EXTRA, hadPlaceholdersAtStart)
            })
        }
    }
}

internal enum class RideCompletionDestination {
    ASSIGN_BIKE,
    SAVE,
}

internal object RideCompletionPolicy {
    fun destinationFor(state: RideState): RideCompletionDestination =
        if (state.bikeId < 0L) {
            RideCompletionDestination.ASSIGN_BIKE
        } else {
            RideCompletionDestination.SAVE
        }
}

internal class RideCompletionRequestGuard {
    private var requestInProgress = false

    fun tryStart(): Boolean {
        if (requestInProgress) return false
        requestInProgress = true
        return true
    }

    fun reset() {
        requestInProgress = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveRideScreen(
    onStopRide: (RideState) -> Unit,
    bikesFlow: StateFlow<List<BikeEntity>>,
    pendingStopRideFlow: StateFlow<PendingStopRide?>,
    isSavingFlow: StateFlow<Boolean>,
    onAssignBike: (Long) -> Unit,
    onAssignBikeAndSave: (Long) -> Unit,
    onCancelPendingStop: () -> Unit,
    rideStateFlow: kotlinx.coroutines.flow.StateFlow<RideState>,
    saveFailedEvents: kotlinx.coroutines.flow.SharedFlow<Unit>,
    assignmentFailedEvents: kotlinx.coroutines.flow.SharedFlow<Unit>,
) {
    val context = LocalContext.current
    val state by rideStateFlow.collectAsState()
    val bikes by bikesFlow.collectAsState()
    val pendingStopRide by pendingStopRideFlow.collectAsState()
    val isSaving by isSavingFlow.collectAsState()
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
    val elapsedMovingMs = com.clintoncochrane.bikecompanion.util.computeElapsedMovingMs(state, System.currentTimeMillis())
    val snackbarHostState = remember { SnackbarHostState() }
    var showStopConfirm by remember { mutableStateOf(false) }
    val pauseLabel = stringResource(R.string.ride_pause)
    val resumeLabel = stringResource(R.string.ride_resume)

    LaunchedEffect(Unit) {
        saveFailedEvents.collect {
            snackbarHostState.showSnackbar(context.getString(R.string.ride_save_failed_tracking))
        }
    }
    LaunchedEffect(Unit) {
        assignmentFailedEvents.collect {
            snackbarHostState.showSnackbar(context.getString(R.string.ride_assign_bike_failed))
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
            if (state.bikeId > 0L) {
                bikes.find { it.id == state.bikeId }?.let { bike ->
                    Text(
                        text = stringResource(R.string.trip_ride_bike, bike.name),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else if (state.isTracking) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.ride_no_bike_assigned),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    bikes.forEach { bike ->
                        TextButton(
                            onClick = { onAssignBike(bike.id) },
                            enabled = !isSaving,
                        ) {
                            Text(stringResource(R.string.ride_assign_bike, bike.name))
                        }
                    }
                }
            }
            // key(tick) forces recomposition every second when active; when paused, elapsedMovingMs is static
            key(tick) {
                Text(
                    text = stringResource(
                        R.string.ride_duration,
                        com.clintoncochrane.bikecompanion.util.DurationFormatHelper.formatDurationBreakdownMs(elapsedMovingMs, capAt24h = false),
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
                    enabled = state.startTimeMs > 0 && state.isTracking && !isSaving && pendingStopRide == null,
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
            if (pendingStopRide != null) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(stringResource(R.string.ride_assign_before_save_title)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.ride_assign_before_save_message))
                            if (bikes.isEmpty()) {
                                Text(stringResource(R.string.ride_assign_before_save_no_bikes))
                            }
                            bikes.forEach { bike ->
                                TextButton(
                                    onClick = { onAssignBikeAndSave(bike.id) },
                                    enabled = !isSaving,
                                ) {
                                    Text(stringResource(R.string.ride_save_with_bike, bike.name))
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = onCancelPendingStop, enabled = !isSaving) {
                            Text(stringResource(R.string.ride_continue_tracking))
                        }
                    },
                )
            }
        }
    }
}
