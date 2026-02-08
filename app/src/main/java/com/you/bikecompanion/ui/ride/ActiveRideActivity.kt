package com.you.bikecompanion.ui.ride

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActiveRideActivity : ComponentActivity() {

    @Inject
    lateinit var rideRepository: RideRepository

    private val rideStateFlow = MutableStateFlow(RideState())
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
        setContent {
            BikeCompanionTheme {
                ActiveRideScreen(
                    bikeId = bikeId,
                    rideRepository = rideRepository,
                    onStopRide = { state -> stopRideAndSave(state) },
                    rideStateFlow = rideStateFlow.asStateFlow(),
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

    private fun stopRideAndSave(state: RideState) {
        val bikeId = intent.getLongExtra(BIKE_ID_EXTRA, -1L)
        val endTime = System.currentTimeMillis()
        val ride = RideEntity(
            bikeId = if (bikeId >= 0) bikeId else null,
            distanceKm = state.distanceKm,
            durationMs = (endTime - state.startTimeMs).coerceAtLeast(0L),
            avgSpeedKmh = state.avgSpeedKmh,
            maxSpeedKmh = state.maxSpeedKmh,
            elevGainM = state.elevGainM,
            elevLossM = state.elevLossM,
            startedAt = state.startTimeMs,
            endedAt = endTime,
            source = RideSource.APP,
        )
        CoroutineScope(Dispatchers.Main).launch {
            rideRepository.saveRideAndUpdateBikeAndComponents(ride)
        }
        stopService(Intent(this, RideTrackingService::class.java).apply {
            putExtra(RideTrackingService.ACTION_KEY, RideTrackingService.ACTION_STOP)
        })
        finish()
    }

    companion object {
        const val BIKE_ID_EXTRA = "bike_id"
        fun start(context: Context, bikeId: Long) {
            context.startActivity(Intent(context, ActiveRideActivity::class.java).apply {
                putExtra(BIKE_ID_EXTRA, bikeId)
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
) {
    val state by rideStateFlow.collectAsState()
    val tick by produceState(initialValue = 0) {
        while (true) {
            delay(1000)
            value += 1
        }
    }
    val context = LocalContext.current
    val pauseLabel = stringResource(R.string.ride_pause)
    val resumeLabel = stringResource(R.string.ride_resume)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.ride_active_title)) },
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
            // key(tick) forces recomposition every second so elapsed duration updates
            key(tick) {
                Text(
                    text = stringResource(R.string.ride_duration, com.you.bikecompanion.util.DurationFormatHelper.formatElapsedFromStart(state.startTimeMs)),
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
                text = stringResource(R.string.ride_elevation_gain, state.elevGainM),
                style = MaterialTheme.typography.bodySmall,
            )
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
                    onClick = { onStopRide(state) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.ride_stop))
                }
            }
        }
    }
}
