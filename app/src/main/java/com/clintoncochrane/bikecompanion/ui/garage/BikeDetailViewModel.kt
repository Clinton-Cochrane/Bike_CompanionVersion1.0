package com.clintoncochrane.bikecompanion.ui.garage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.bike.BikeRepository
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalRepository
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import com.clintoncochrane.bikecompanion.data.preferences.AppPreferencesRepository
import com.clintoncochrane.bikecompanion.data.ride.RideEntity
import com.clintoncochrane.bikecompanion.data.ride.RideRepository
import com.clintoncochrane.bikecompanion.util.ComponentSortOrder
import com.clintoncochrane.bikecompanion.util.availableComponentSortOrder
import com.clintoncochrane.bikecompanion.util.nextServiceInbox
import com.clintoncochrane.bikecompanion.util.sortComponents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BikeDetailUiState(
    val bike: BikeEntity? = null,
    val components: List<ComponentEntity> = emptyList(),
    val nextServiceComponents: List<ComponentEntity> = emptyList(),
    val rides: List<RideEntity> = emptyList(),
    val bikes: List<BikeEntity> = emptyList(),
    val componentSortOrder: ComponentSortOrder = ComponentSortOrder.TYPE_AZ,
    /** User-configured threshold below which mild alert is shown (default from [AppPreferencesRepository]). */
    val closeToServiceHealthThreshold: Int = AppPreferencesRepository.DEFAULT_CLOSE_TO_SERVICE_THRESHOLD,
    val hasNextServiceItems: Boolean = false,
    val loading: Boolean = true,
    val installOutcome: BikeDetailViewModel.InstallOutcome? = null,
    /** Ride IDs whose review flags have been dismissed. */
    val dismissedRideFlagIds: Set<Long> = emptySet(),
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
                combine(
                    componentRepository.getComponentsByBikeId(bikeId),
                    appPreferencesRepository.closeToServiceHealthThreshold,
                ) { components, threshold -> components to threshold }
                    .flatMapLatest { (components, threshold) ->
                        serviceIntervalRepository.getIntervalsByComponentIds(components.map { it.id })
                            .map { intervals -> Triple(components, intervals, threshold) }
                    }
                    .collect { (components, intervals, threshold) ->
                        val intervalsByComponentId = intervals.groupBy { it.componentId }
                        val inbox = nextServiceInbox(components, intervalsByComponentId, threshold)
                        _uiState.update { state ->
                            state.copy(
                                components = sortComponents(components, ComponentSortOrder.TYPE_AZ),
                                nextServiceComponents = inbox,
                                closeToServiceHealthThreshold = threshold,
                                hasNextServiceItems = inbox.isNotEmpty(),
                                componentSortOrder = availableComponentSortOrder(
                                    state.componentSortOrder,
                                    inbox.isNotEmpty(),
                                ),
                            )
                        }
                    }
            }
            viewModelScope.launch {
                combine(
                    rideRepository.getRidesByBikeId(bikeId),
                    appPreferencesRepository.dismissedRideFlagIds,
                ) { rides, dismissedIds ->
                    Pair(rides.sortedByDescending { r -> r.endedAt }, dismissedIds)
                }.collect { (rides, dismissedIds) ->
                    _uiState.update {
                        it.copy(rides = rides, dismissedRideFlagIds = dismissedIds)
                    }
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
        _uiState.update { state ->
            state.copy(
                componentSortOrder = availableComponentSortOrder(order, state.hasNextServiceItems),
            )
        }
    }

    fun addComponent(
        type: String,
        name: String,
        lifespanKm: Double,
    ) {
        if (bikeId <= 0) return
        viewModelScope.launch {
            componentRepository.insertComponent(
                ComponentEntity(
                    bikeId = bikeId,
                    type = type,
                    name = name,
                    lifespanKm = lifespanKm,
                    baselineKm = 0.0,
                    priorUsageCertainty = PriorUsageCertainty.KNOWN,
                    installedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun replaceComponent(component: ComponentEntity, replacement: ComponentEntity) {
        viewModelScope.launch {
            componentRepository.replaceComponent(component, replacement)
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

    fun snoozeComponent(component: com.clintoncochrane.bikecompanion.data.component.ComponentEntity, snoozeKm: Double) {
        viewModelScope.launch {
            componentRepository.updateComponent(
                component.copy(alertSnoozeUntilKm = component.lifetimeDistanceKm + snoozeKm),
            )
        }
    }

    fun setAlertsEnabled(
        component: com.clintoncochrane.bikecompanion.data.component.ComponentEntity,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            componentRepository.updateComponent(component.copy(alertsEnabled = enabled))
        }
    }

    /** Outcome of an install attempt; UI shows snackbar for Duplicate and closes picker on Success. */
    sealed class InstallOutcome {
        data object Success : InstallOutcome()
        data class Duplicate(val bikeName: String) : InstallOutcome()
    }

    fun installComponent(component: ComponentEntity, targetBikeId: Long) {
        viewModelScope.launch {
            if (componentRepository.wouldBeDuplicatePart(component, targetBikeId)) {
                val bikeName = _uiState.value.bikes.find { it.id == targetBikeId }?.name ?: ""
                _uiState.update { it.copy(installOutcome = InstallOutcome.Duplicate(bikeName)) }
                return@launch
            }
            componentRepository.installComponent(component, targetBikeId)
            _uiState.update { it.copy(installOutcome = InstallOutcome.Success) }
        }
    }

    fun clearInstallOutcome() {
        _uiState.update { it.copy(installOutcome = null) }
    }

    fun uninstallComponent(component: ComponentEntity) {
        viewModelScope.launch {
            componentRepository.uninstallComponent(component)
        }
    }

    fun retireComponent(component: ComponentEntity) {
        viewModelScope.launch {
            componentRepository.retireComponent(component)
        }
    }

    fun deleteComponent(component: ComponentEntity) {
        viewModelScope.launch {
            componentRepository.deleteComponent(component)
        }
    }

    fun dismissRideFlag(rideId: Long) {
        viewModelScope.launch {
            appPreferencesRepository.addDismissedRideFlagId(rideId)
        }
    }

    fun deleteRide(ride: RideEntity) {
        viewModelScope.launch {
            rideRepository.deleteRide(ride)
        }
    }
}
