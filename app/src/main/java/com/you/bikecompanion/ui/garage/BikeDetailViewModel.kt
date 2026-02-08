package com.you.bikecompanion.ui.garage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.component.ComponentRepository
import com.you.bikecompanion.data.component.DefaultSeedComponents
import com.you.bikecompanion.data.component.ServiceIntervalRepository
import com.you.bikecompanion.data.preferences.AppPreferencesRepository
import com.you.bikecompanion.data.ride.RideEntity
import com.you.bikecompanion.data.ride.RideRepository
import com.you.bikecompanion.util.ComponentSortOrder
import com.you.bikecompanion.util.sortComponents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BikeDetailUiState(
    val bike: BikeEntity? = null,
    val components: List<ComponentEntity> = emptyList(),
    val rides: List<RideEntity> = emptyList(),
    val bikes: List<BikeEntity> = emptyList(),
    val componentSortOrder: ComponentSortOrder = ComponentSortOrder.TYPE_AZ,
    /** User-configured threshold below which mild alert is shown (default from [AppPreferencesRepository]). */
    val closeToServiceHealthThreshold: Int = AppPreferencesRepository.DEFAULT_CLOSE_TO_SERVICE_THRESHOLD,
    val loading: Boolean = true,
)

@HiltViewModel
class BikeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bikeRepository: BikeRepository,
    private val rideRepository: RideRepository,
    private val componentRepository: ComponentRepository,
    private val serviceIntervalRepository: ServiceIntervalRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    private val bikeId: Long = savedStateHandle.get<String>("bikeId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(BikeDetailUiState())
    val uiState: StateFlow<BikeDetailUiState> = _uiState.asStateFlow()

    init {
        if (bikeId > 0) {
            viewModelScope.launch {
                val bike = bikeRepository.getBikeById(bikeId)
                _uiState.update { it.copy(bike = bike, loading = false) }
            }
            viewModelScope.launch {
                componentRepository.getComponentsByBikeId(bikeId).collect { list ->
                    if (list.isNotEmpty() && list.size < DefaultSeedComponents.LIST.size) {
                        componentRepository.seedMissingDefaultComponents(bikeId)
                    }
                    val order = _uiState.value.componentSortOrder
                    val intervalsByComponentId = if (order == ComponentSortOrder.NEXT_SERVICE && list.isNotEmpty()) {
                        val ids = list.map { it.id }
                        val intervals = serviceIntervalRepository.getIntervalsByComponentIdsOnce(ids)
                        intervals.groupBy { it.componentId }
                    } else emptyMap()
                    val sorted = sortComponents(list, order, intervalsByComponentId)
                    _uiState.update { it.copy(components = sorted) }
                }
            }
            viewModelScope.launch {
                rideRepository.getRidesByBikeId(bikeId).collect { list ->
                    _uiState.update { it.copy(rides = list.sortedByDescending { r -> r.endedAt }) }
                }
            }
            viewModelScope.launch {
                bikeRepository.getAllBikes().collect { list ->
                    _uiState.update { it.copy(bikes = list) }
                }
            }
        } else {
            _uiState.update { it.copy(loading = false) }
        }
        viewModelScope.launch {
            appPreferencesRepository.closeToServiceHealthThreshold.collect { threshold ->
                _uiState.update { it.copy(closeToServiceHealthThreshold = threshold) }
            }
        }
    }

    fun setComponentSortOrder(order: ComponentSortOrder) {
        viewModelScope.launch {
            val current = _uiState.value.components
            val intervalsByComponentId = if (order == ComponentSortOrder.NEXT_SERVICE && current.isNotEmpty()) {
                val ids = current.map { it.id }
                val intervals = serviceIntervalRepository.getIntervalsByComponentIdsOnce(ids)
                intervals.groupBy { it.componentId }
            } else emptyMap()
            val sorted = sortComponents(current, order, intervalsByComponentId)
            _uiState.update {
                it.copy(componentSortOrder = order, components = sorted)
            }
        }
    }

    fun addComponent(type: String, name: String, lifespanKm: Double) {
        if (bikeId <= 0) return
        viewModelScope.launch {
            componentRepository.insertComponent(
                ComponentEntity(
                    bikeId = bikeId,
                    type = type,
                    name = name,
                    lifespanKm = lifespanKm,
                    installedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun markComponentReplaced(component: com.you.bikecompanion.data.component.ComponentEntity) {
        viewModelScope.launch {
            componentRepository.markComponentReplaced(component)
            val bike = bikeRepository.getBikeById(bikeId)
            _uiState.update { it.copy(bike = bike) }
        }
    }

    fun resetChainReplacementCount() {
        if (bikeId <= 0) return
        viewModelScope.launch {
            bikeRepository.resetChainReplacementCount(bikeId)
            val bike = bikeRepository.getBikeById(bikeId)
            _uiState.update { it.copy(bike = bike) }
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

    fun installComponent(component: ComponentEntity, targetBikeId: Long) {
        viewModelScope.launch {
            componentRepository.installComponent(component, targetBikeId)
        }
    }

    fun uninstallComponent(component: ComponentEntity) {
        viewModelScope.launch {
            componentRepository.uninstallComponent(component)
        }
    }

    fun deleteComponent(component: ComponentEntity) {
        viewModelScope.launch {
            componentRepository.deleteComponent(component)
        }
    }
}
