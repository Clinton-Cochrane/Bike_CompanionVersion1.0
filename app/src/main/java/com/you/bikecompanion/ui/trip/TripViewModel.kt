package com.you.bikecompanion.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.ride.RideEntity
import com.you.bikecompanion.data.ride.RideRepository
import com.you.bikecompanion.data.ride.RideSource
import com.you.bikecompanion.healthconnect.HealthConnectImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TripUiState(
    val bikes: List<BikeEntity> = emptyList(),
    val rides: List<RideEntity> = emptyList(),
    val selectedBike: BikeEntity? = null,
    val lastRiddenBike: BikeEntity? = null,
)

@HiltViewModel
class TripViewModel @Inject constructor(
    private val bikeRepository: BikeRepository,
    private val rideRepository: RideRepository,
    private val healthConnectImporter: HealthConnectImporter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripUiState())
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                bikeRepository.getAllBikes(),
                rideRepository.getAllRides(),
            ) { bikes, rides ->
                Pair(bikes, rides)
            }.collect { (bikes, rides) ->
                val lastRidden = bikeRepository.getMostRecentlyRiddenBike()
                _uiState.value = TripUiState(
                    bikes = bikes,
                    rides = rides.sortedByDescending { it.endedAt },
                    selectedBike = _uiState.value.selectedBike?.let { selected ->
                        bikes.find { it.id == selected.id }
                    } ?: lastRidden,
                    lastRiddenBike = lastRidden,
                )
            }
        }
    }

    fun selectBike(bike: BikeEntity?) {
        _uiState.value = _uiState.value.copy(selectedBike = bike)
    }

    fun importFromHealthConnect(onResult: (message: String) -> Unit) {
        val bikeId = _uiState.value.selectedBike?.id ?: -1L
        if (bikeId < 0) {
            onResult("Select a bike first")
            return
        }
        viewModelScope.launch {
            val sessions = healthConnectImporter.readCyclingSessions()
            if (sessions.isEmpty()) {
                onResult("No cycling sessions found in Health Connect")
                return@launch
            }
            var count = 0
            sessions.forEach { session ->
                val ride = RideEntity(
                    bikeId = bikeId,
                    distanceKm = session.distanceKm,
                    durationMs = session.durationMs,
                    startedAt = session.startTimeMs,
                    endedAt = session.endTimeMs,
                    source = RideSource.HEALTH_CONNECT,
                )
                rideRepository.saveRideAndUpdateBikeAndComponents(ride)
                count++
            }
            onResult("Imported $count ride(s)")
        }
    }
}
