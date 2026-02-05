package com.you.bikecompanion.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.ride.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

data class StatsUiState(
    val bikes: List<BikeEntity> = emptyList(),
    val selectedBike: BikeEntity? = null,
    val stats: BikeStats? = null,
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
            bikeRepository.getAllBikes().collect { bikes ->
                _uiState.update { it.copy(bikes = bikes) }
            }
        }
    }

    fun selectBike(bike: BikeEntity?) {
        _uiState.update { it.copy(selectedBike = bike, stats = null) }
        bike ?: return
        viewModelScope.launch {
            rideRepository.getRidesByBikeId(bike.id).collect { rides ->
                val totalKm = rides.sumOf { it.distanceKm }
                val count = rides.size
                val sumSpeed = rides.sumOf { it.avgSpeedKmh }
                val totalElev = rides.sumOf { it.elevGainM }
                _uiState.update {
                    it.copy(
                        stats = BikeStats(
                            totalDistanceKm = totalKm,
                            rideCount = count,
                            avgDistanceKm = if (count > 0) totalKm / count else 0.0,
                            avgSpeedKmh = if (count > 0) sumSpeed / count else 0.0,
                            totalElevGainM = totalElev,
                        ),
                    )
                }
            }
        }
    }
}
