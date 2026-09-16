package com.clintoncochrane.bikecompanion.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.bike.BikeRepository
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import com.clintoncochrane.bikecompanion.data.component.ComponentSwapEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentSwapRepository
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalRepository
import com.clintoncochrane.bikecompanion.data.preferences.AppPreferencesRepository
import com.clintoncochrane.bikecompanion.data.ride.RideEntity
import com.clintoncochrane.bikecompanion.data.ride.RideRepository
import com.clintoncochrane.bikecompanion.util.ComponentSortOrder
import com.clintoncochrane.bikecompanion.util.GarageSpecHelper
import com.clintoncochrane.bikecompanion.util.componentHealthPercent
import com.clintoncochrane.bikecompanion.util.minimumComponentHealthPercent
import com.clintoncochrane.bikecompanion.util.sortComponents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Tab selection for the garage view. */
enum class GarageTab {
    Bikes,
    Components,
}

data class GarageUiState(
    val selectedTab: GarageTab = GarageTab.Bikes,
    val bikes: List<BikeEntity> = emptyList(),
    val garageComponents: List<ComponentEntity> = emptyList(),
    /** Per-bike health from components; null when any installed component has unknown prior usage. */
    val bikeHealth: Map<Long, Int?> = emptyMap(),
    /** Bike IDs that have an alert (health at or below close-to-service threshold). */
    val bikeHasAlert: Set<Long> = emptySet(),
    /** Health % threshold below which to show alert; from [AppPreferencesRepository]. */
    val closeToServiceThreshold: Int = AppPreferencesRepository.DEFAULT_CLOSE_TO_SERVICE_THRESHOLD,
    /** Null means show all types. Otherwise filter by this component type. */
    val componentTypeFilter: String? = null,
    /** Null means show all bikes. Otherwise filter to components assigned to this bike id. */
    val componentBikeFilter: Long? = null,
    val componentSortOrder: ComponentSortOrder = ComponentSortOrder.TYPE_AZ,
    /** Total distance across all bikes (rider total). */
    val totalDistanceKm: Double = 0.0,
    /** Bike ID that was ridden most recently; null when no bike has been ridden. */
    val lastRiddenBikeId: Long? = null,
    /** State for the one-bike-at-a-time Bikes overview. */
    val bikesOverview: GarageBikesUiState = GarageBikesUiState(),
    /** State for the complete active, unassigned, and retired Parts directory. */
    val partsDirectory: PartsDirectoryUiState = PartsDirectoryUiState(),
)

@HiltViewModel
class GarageViewModel @Inject constructor(
    private val bikeRepository: BikeRepository,
    private val componentRepository: ComponentRepository,
    private val componentSwapRepository: ComponentSwapRepository,
    private val serviceIntervalRepository: ServiceIntervalRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val rideRepository: RideRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GarageUiState())
    val uiState: StateFlow<GarageUiState> = _uiState.asStateFlow()
    private var rides: List<RideEntity> = emptyList()
    private var allComponents: List<ComponentEntity> = emptyList()
    private var componentSwaps: List<ComponentSwapEntity> = emptyList()

    init {
        viewModelScope.launch {
            bikeRepository.getAllBikes().collect { bikes ->
                val sorted = GarageSpecHelper.sortBikesByLastRidden(bikes)
                _uiState.update { state ->
                    val health = computeBikeHealth(state.garageComponents, sorted)
                    state.copy(
                        bikes = sorted,
                        bikeHealth = health,
                        bikeHasAlert = computeBikeAlerts(state.garageComponents, sorted, state.closeToServiceThreshold),
                        totalDistanceKm = GarageSpecHelper.computeTotalDistanceKm(sorted),
                        lastRiddenBikeId = GarageSpecHelper.getLastRiddenBikeId(sorted),
                    ).withBikesOverview().withPartsDirectory()
                }
            }
        }
        viewModelScope.launch {
            combine(
                componentRepository.getAllComponentsFlow(),
                componentSwapRepository.getAllSwaps(),
            ) { components, swaps -> components to swaps }
                .collect { (components, swaps) ->
                    allComponents = components
                    componentSwaps = swaps
                    _uiState.update { it.withPartsDirectory() }
                }
        }
        viewModelScope.launch {
            componentRepository.getNonRetiredComponents().collect { list ->
                val order = _uiState.value.componentSortOrder
                val intervalsByComponentId = if (order == ComponentSortOrder.NEXT_SERVICE && list.isNotEmpty()) {
                    val ids = list.map { it.id }
                    val intervals = serviceIntervalRepository.getIntervalsByComponentIdsOnce(ids)
                    intervals.groupBy { it.componentId }
                } else emptyMap()
                val sorted = sortComponents(list, order, intervalsByComponentId)
                val bikes = _uiState.value.bikes
                val health = computeBikeHealth(sorted, bikes)
                val threshold = _uiState.value.closeToServiceThreshold
                _uiState.update {
                    it.copy(
                        garageComponents = sorted,
                        bikeHealth = health,
                        bikeHasAlert = computeBikeAlerts(sorted, bikes, threshold),
                    ).withBikesOverview()
                }
            }
        }
        viewModelScope.launch {
            appPreferencesRepository.closeToServiceHealthThreshold.collect { threshold ->
                _uiState.update { state ->
                    state.copy(
                        closeToServiceThreshold = threshold,
                        bikeHasAlert = computeBikeAlerts(
                            state.garageComponents,
                            state.bikes,
                            threshold,
                        ),
                    ).withBikesOverview()
                }
            }
        }
        viewModelScope.launch {
            rideRepository.getAllRides().collect { updatedRides ->
                rides = updatedRides
                _uiState.update { it.withBikesOverview() }
            }
        }
    }

    private fun computeBikeHealth(components: List<ComponentEntity>, bikes: List<BikeEntity>): Map<Long, Int?> {
        val byBike = components.filter { it.bikeId != null }.groupBy { it.bikeId!! }
        return bikes.associate { bike ->
            val comps = byBike[bike.id].orEmpty()
            bike.id to minimumComponentHealthPercent(comps)
        }
    }

    private fun computeBikeAlerts(
        components: List<ComponentEntity>,
        bikes: List<BikeEntity>,
        threshold: Int,
    ): Set<Long> {
        val byBike = components.filter { it.bikeId != null }.groupBy { it.bikeId!! }
        return bikes.filter { bike ->
            val comps = byBike[bike.id].orEmpty()
            if (comps.isEmpty()) return@filter false
            comps.any { component ->
                componentHealthPercent(component)?.let { it <= threshold } == true
            }
        }.map { it.id }.toSet()
    }

    fun setComponentSortOrder(order: ComponentSortOrder) {
        viewModelScope.launch {
            val current = _uiState.value.garageComponents
            val intervalsByComponentId = if (order == ComponentSortOrder.NEXT_SERVICE && current.isNotEmpty()) {
                val ids = current.map { it.id }
                val intervals = serviceIntervalRepository.getIntervalsByComponentIdsOnce(ids)
                intervals.groupBy { it.componentId }
            } else emptyMap()
            val sorted = sortComponents(current, order, intervalsByComponentId)
            _uiState.update {
                it.copy(componentSortOrder = order, garageComponents = sorted)
            }
        }
    }

    fun setSelectedTab(tab: GarageTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setPartsDirectoryFilter(filter: PartsDirectoryFilter) {
        _uiState.update { state ->
            state.copy(partsDirectory = state.partsDirectory.copy(filter = filter))
                .withPartsDirectory()
        }
    }

    fun selectBike(index: Int) {
        _uiState.update { state ->
            val selectedBikeId = state.bikes.getOrNull(index)?.id ?: return@update state
            state.withBikesOverview(selectedBikeId)
        }
    }

    fun setComponentTypeFilter(type: String?) {
        _uiState.update { it.copy(componentTypeFilter = type) }
    }

    fun setComponentBikeFilter(bikeId: Long?) {
        _uiState.update { it.copy(componentBikeFilter = bikeId) }
    }

    fun addComponentToGarage(
        type: String,
        name: String,
        lifespanKm: Double,
    ) {
        viewModelScope.launch {
            componentRepository.insertComponent(
                ComponentEntity(
                    bikeId = null,
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

    private fun GarageUiState.withBikesOverview(selectedBikeId: Long? = bikesOverview.selectedBikeId): GarageUiState =
        copy(
            bikesOverview = GarageBikesPresenter.build(
                bikes = bikes,
                components = garageComponents,
                rides = rides,
                closeToServiceThreshold = closeToServiceThreshold,
                selectedBikeId = selectedBikeId,
            ),
        )

    private fun GarageUiState.withPartsDirectory(): GarageUiState = copy(
        partsDirectory = partsDirectory.copy(
            sections = PartsDirectoryPresenter.build(
                components = allComponents,
                bikes = bikes,
                swaps = componentSwaps,
                filter = partsDirectory.filter,
            ),
        ),
    )
}
