package com.you.bikecompanion.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.ride.RideEntity
import com.you.bikecompanion.data.ride.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BikeStats(
    val totalDistanceKm: Double,
    val rideCount: Int,
    val avgDistanceKm: Double,
    val avgSpeedKmh: Double,
    val totalElevGainM: Double,
)

/**
 * A bike with its computed ride stats. [stats] is null when the bike has no rides.
 */
data class BikeWithStats(
    val bike: BikeEntity,
    val stats: BikeStats?,
)

data class StatsUiState(
    val bikesWithStats: List<BikeWithStats> = emptyList(),
    val filterBikeId: Long? = null,
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
                bikes.map { bike ->
                    val bikeRides = rides.filter { it.bikeId == bike.id }
                    val stats = computeStats(bikeRides)
                    BikeWithStats(bike = bike, stats = stats)
                }
            }.collect { bikesWithStats ->
                _uiState.update { it.copy(bikesWithStats = bikesWithStats) }
            }
        }
    }

    /**
     * Filters the stats list to a single bike when non-null, or shows all when null.
     */
    fun setFilterBikeId(bikeId: Long?) {
        _uiState.update { it.copy(filterBikeId = bikeId) }
    }

    private fun computeStats(rides: List<RideEntity>): BikeStats? {
        if (rides.isEmpty()) return null
        val totalKm = rides.sumOf { it.distanceKm }
        val count = rides.size
        val sumSpeed = rides.sumOf { it.avgSpeedKmh }
        val totalElev = rides.sumOf { it.elevGainM }
        return BikeStats(
            totalDistanceKm = totalKm,
            rideCount = count,
            avgDistanceKm = totalKm / count,
            avgSpeedKmh = sumSpeed / count,
            totalElevGainM = totalElev,
        )
    }
}
