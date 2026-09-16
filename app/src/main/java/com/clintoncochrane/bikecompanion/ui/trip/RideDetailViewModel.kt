package com.clintoncochrane.bikecompanion.ui.trip

import androidx.lifecycle.SavedStateHandle
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

data class RideDetailUiState(
    val ride: RideEntity? = null,
    val bike: BikeEntity? = null,
    val loading: Boolean = true,
)

@HiltViewModel
class RideDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    rideRepository: RideRepository,
    bikeRepository: BikeRepository,
) : ViewModel() {

    private val rideId = savedStateHandle.get<String>("rideId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(RideDetailUiState())
    val uiState: StateFlow<RideDetailUiState> = _uiState.asStateFlow()

    init {
        if (rideId <= 0L) {
            _uiState.update { it.copy(loading = false) }
        } else {
            viewModelScope.launch {
                combine(
                    rideRepository.getAllRides(),
                    bikeRepository.getAllBikes(),
                ) { rides, bikes ->
                    val ride = rides.find { it.id == rideId }
                    RideDetailUiState(
                        ride = ride,
                        bike = bikes.find { it.id == ride?.bikeId },
                        loading = false,
                    )
                }.collect { _uiState.value = it }
            }
        }
    }
}
