package com.clintoncochrane.bikecompanion.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.bike.BikeRepository
import com.clintoncochrane.bikecompanion.data.ride.RideEntity
import com.clintoncochrane.bikecompanion.data.ride.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsSummary(
    val totalDistanceKm: Double = 0.0,
    val totalRideDurationMs: Long = 0L,
    val rideCount: Int = 0,
    val completedServiceCount: Int = 0,
)

data class BikeWithStats(
    val bike: BikeEntity,
    val stats: StatsSummary,
)

data class StatsUiState(
    val allBikesStats: StatsSummary = StatsSummary(),
    val bikesWithStats: List<BikeWithStats> = emptyList(),
    val selectedBikeIndex: Int = 0,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val bikeRepository: BikeRepository,
    private val rideRepository: RideRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                bikeRepository.getAllBikes(),
                rideRepository.getAllRides(),
            ) { bikes, rides ->
                StatsUiState(
                    allBikesStats = computeStats(rides),
                    bikesWithStats = bikes.map { bike ->
                        BikeWithStats(
                            bike = bike,
                            stats = computeStats(rides.filter { it.bikeId == bike.id }),
                        )
                    },
                )
            }.collect { stats ->
                _uiState.update { currentState ->
                    stats.copy(
                        selectedBikeIndex = currentState.selectedBikeIndex
                            .coerceAtMost(stats.bikesWithStats.lastIndex)
                            .coerceAtLeast(0),
                    )
                }
            }
        }
    }

    fun selectPreviousBike() {
        _uiState.update { state ->
            state.copy(selectedBikeIndex = (state.selectedBikeIndex - 1).coerceAtLeast(0))
        }
    }

    fun selectNextBike() {
        _uiState.update { state ->
            if (state.bikesWithStats.isEmpty()) {
                state
            } else {
                state.copy(
                    selectedBikeIndex = (state.selectedBikeIndex + 1)
                        .coerceAtMost(state.bikesWithStats.lastIndex),
                )
            }
        }
    }

    fun selectBike(index: Int) {
        _uiState.update { state ->
            state.copy(
                selectedBikeIndex = index.coerceIn(0, state.bikesWithStats.lastIndex.coerceAtLeast(0)),
            )
        }
    }

    private fun computeStats(rides: List<RideEntity>): StatsSummary = StatsSummary(
        totalDistanceKm = rides.sumOf { it.distanceKm },
        totalRideDurationMs = rides.sumOf { it.durationMs.coerceAtLeast(0L) },
        rideCount = rides.size,
        // Service interval completion only resets an interval; it does not persist a service record.
        completedServiceCount = 0,
    )
}
