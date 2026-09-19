codpackage com.clintoncochrane.bikecompanion.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.clintoncochrane.bikecompanion.R
import com.clintoncochrane.bikecompanion.data.bike.BikeRepository
import com.clintoncochrane.bikecompanion.data.ride.ActiveRideCheckpoint
import com.clintoncochrane.bikecompanion.location.RideTrackingService
import com.clintoncochrane.bikecompanion.ui.ride.ActiveRideActivity
import com.clintoncochrane.bikecompanion.ui.ride.RideRecoveryCoordinator
import com.clintoncochrane.bikecompanion.ui.navigation.BikeCompanionNavGraph
import com.clintoncochrane.bikecompanion.ui.navigation.Screen
import com.clintoncochrane.bikecompanion.ui.theme.BikeCompanionTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var rideRecoveryCoordinator: RideRecoveryCoordinator
    @Inject lateinit var bikeRepository: BikeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BikeCompanionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScaffold(
                        startDestination = intent.getStringExtra(START_DESTINATION_EXTRA),
                    )
                    if (!RideTrackingService.rideIsActive.collectAsState().value) {
                        RideRecoveryDialog(
                            coordinator = rideRecoveryCoordinator,
                            bikeRepository = bikeRepository,
                            onResume = { checkpoint ->
                                startService(Intent(this@MainActivity, RideTrackingService::class.java).apply {
                                    putExtra(RideTrackingService.ACTION_KEY, RideTrackingService.ACTION_RESTORE)
                                })
                                ActiveRideActivity.start(
                                    this@MainActivity,
                                    checkpoint.bikeId ?: -1L,
                                    checkpoint.hadPlaceholdersAtStart,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val START_DESTINATION_EXTRA = "start_destination"
        val GARAGE_DESTINATION = Screen.Garage.route
    }
}

@Composable
private fun RideRecoveryDialog(
    coordinator: RideRecoveryCoordinator,
    bikeRepository: BikeRepository,
    onResume: (ActiveRideCheckpoint) -> Unit,
) {
    var checkpoint by remember { mutableStateOf<ActiveRideCheckpoint?>(null) }
    var checked by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    val bikes by bikeRepository.getAllBikes().collectAsState(initial = emptyList())
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        checkpoint = coordinator.load()
        checked = true
    }
    val currentCheckpoint = checkpoint ?: return
    if (!checked) return

    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.ride_recovery_title)) },
        text = {
            Text(
                if (currentCheckpoint.bikeId == null) {
                    stringResource(R.string.ride_recovery_select_bike)
                } else if (saveFailed) {
                    stringResource(R.string.ride_recovery_save_failed)
                } else {
                    stringResource(R.string.ride_recovery_message)
                },
            )
        },
        confirmButton = {
            if (currentCheckpoint.bikeId == null) {
                bikes.forEach { bike ->
                    TextButton(onClick = {
                        (context as ComponentActivity).lifecycleScope.launch {
                            if (coordinator.save(currentCheckpoint.copy(bikeId = bike.id))) checkpoint = null
                            else saveFailed = true
                        }
                    }) { Text(stringResource(R.string.ride_recovery_save_with_bike, bike.name)) }
                }
            } else {
                TextButton(onClick = {
                    (context as ComponentActivity).lifecycleScope.launch {
                        if (coordinator.save(currentCheckpoint)) checkpoint = null else saveFailed = true
                    }
                }) { Text(stringResource(R.string.ride_recovery_save)) }
            }
        },
        dismissButton = {
            androidx.compose.foundation.layout.Row {
                TextButton(onClick = { onResume(currentCheckpoint); checkpoint = null }) {
                    Text(stringResource(R.string.ride_recovery_resume))
                }
                TextButton(onClick = {
                    (context as ComponentActivity).lifecycleScope.launch {
                        coordinator.discard()
                        checkpoint = null
                    }
                }) { Text(stringResource(R.string.ride_recovery_discard)) }
            }
        },
    )
}
