package com.you.bikecompanion.ui.garage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.component.ComponentRepository
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

data class BikeDetailUiState(
    val bike: BikeEntity? = null,
    val components: List<ComponentEntity> = emptyList(),
    val rides: List<RideEntity> = emptyList(),
    val loading: Boolean = true,
)

@HiltViewModel
class BikeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bikeRepository: BikeRepository,
    private val rideRepository: RideRepository,
    private val componentRepository: ComponentRepository,
) : ViewModel() {

    private val bikeId: Long = savedStateHandle.get<String>("bikeId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(BikeDetailUiState())
    val uiState: StateFlow<BikeDetailUiState> = _uiState.asStateFlow()

    init {
        if (bikeId <= 0) {
            _uiState.update { it.copy(loading = false) }
            return
        }
        viewModelScope.launch {
            val bike = bikeRepository.getBikeById(bikeId)
            _uiState.update { it.copy(bike = bike, loading = false) }
        }
        viewModelScope.launch {
            componentRepository.getComponentsByBikeId(bikeId).collect { list ->
                _uiState.update { it.copy(components = list) }
            }
        }
        viewModelScope.launch {
            rideRepository.getRidesByBikeId(bikeId).collect { list ->
                _uiState.update { it.copy(rides = list.sortedByDescending { r -> r.endedAt }) }
            }
        }
    }

    fun addComponent(type: String, name: String, lifespanKm: Double) {
        if (bikeId <= 0) return
        viewModelScope.launch {
            componentRepository.insertComponent(
                com.you.bikecompanion.data.component.ComponentEntity(
                    bikeId = bikeId,
                    type = type,
                    name = name,
                    lifespanKm = lifespanKm,
                ),
            )
        }
    }

    fun markComponentReplaced(component: com.you.bikecompanion.data.component.ComponentEntity) {
        viewModelScope.launch {
            componentRepository.updateComponent(
                component.copy(
                    distanceUsedKm = 0.0,
                    installedAt = System.currentTimeMillis(),
                    alertSnoozeUntilKm = null,
                    alertSnoozeUntilTime = null,
                ),
            )
        }
    }

    fun snoozeComponent(component: com.you.bikecompanion.data.component.ComponentEntity, snoozeKm: Double) {
        viewModelScope.launch {
            componentRepository.updateComponent(
                component.copy(alertSnoozeUntilKm = component.distanceUsedKm + snoozeKm),
            )
        }
    }

    fun turnOffAlerts(component: com.you.bikecompanion.data.component.ComponentEntity) {
        viewModelScope.launch {
            componentRepository.updateComponent(component.copy(alertsEnabled = false))
        }
    }
}
