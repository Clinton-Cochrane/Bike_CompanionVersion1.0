package com.you.bikecompanion.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.component.ComponentRepository
import com.you.bikecompanion.data.component.ServiceIntervalRepository
import com.you.bikecompanion.data.preferences.AppPreferencesRepository
import com.you.bikecompanion.util.ComponentSortOrder
import com.you.bikecompanion.util.componentHealthPercent
import com.you.bikecompanion.util.sortComponents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    /** Per-bike health 0–100 from components; default 100 when bike has no components. */
    val bikeHealth: Map<Long, Int> = emptyMap(),
    /** Bike IDs that have an alert (health at or below close-to-service threshold). */
    val bikeHasAlert: Set<Long> = emptySet(),
    /** Health % threshold below which to show alert; from [AppPreferencesRepository]. */
    val closeToServiceThreshold: Int = AppPreferencesRepository.DEFAULT_CLOSE_TO_SERVICE_THRESHOLD,
    /** Null means show all types. Otherwise filter by this component type. */
    val componentTypeFilter: String? = null,
    /** Null means show all bikes. Otherwise filter to components assigned to this bike id. */
    val componentBikeFilter: Long? = null,
    val componentSortOrder: ComponentSortOrder = ComponentSortOrder.TYPE_AZ,
)

@HiltViewModel
class GarageViewModel @Inject constructor(
    private val bikeRepository: BikeRepository,
    private val componentRepository: ComponentRepository,
    private val serviceIntervalRepository: ServiceIntervalRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GarageUiState())
    val uiState: StateFlow<GarageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bikeRepository.getAllBikes().collect { bikes ->
                _uiState.update { state ->
                    val health = computeBikeHealth(state.garageComponents, bikes)
                    state.copy(
                        bikes = bikes,
                        bikeHealth = health,
                        bikeHasAlert = computeBikeAlerts(state.garageComponents, bikes, health, state.closeToServiceThreshold),
                    )
                }
            }
        }
        viewModelScope.launch {
            componentRepository.getAllComponentsFlow().collect { list ->
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
                        bikeHasAlert = computeBikeAlerts(sorted, bikes, health, threshold),
                    )
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
                            state.bikeHealth,
                            threshold,
                        ),
                    )
                }
            }
        }
    }

    private fun computeBikeHealth(components: List<ComponentEntity>, bikes: List<BikeEntity>): Map<Long, Int> {
        val byBike = components.filter { it.bikeId != null }.groupBy { it.bikeId!! }
        return bikes.associate { bike ->
            val comps = byBike[bike.id].orEmpty()
            val health = if (comps.isEmpty()) 100
            else comps.minOf { componentHealthPercent(it) }
            bike.id to health
        }
    }

    private fun computeBikeAlerts(
        components: List<ComponentEntity>,
        bikes: List<BikeEntity>,
        bikeHealth: Map<Long, Int>,
        threshold: Int,
    ): Set<Long> {
        val byBike = components.filter { it.bikeId != null }.groupBy { it.bikeId!! }
        return bikes.filter { bike ->
            val comps = byBike[bike.id].orEmpty()
            if (comps.isEmpty()) return@filter false
            val health = bikeHealth[bike.id] ?: 100
            health <= threshold
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

    fun setComponentTypeFilter(type: String?) {
        _uiState.update { it.copy(componentTypeFilter = type) }
    }

    fun setComponentBikeFilter(bikeId: Long?) {
        _uiState.update { it.copy(componentBikeFilter = bikeId) }
    }

    fun addComponentToGarage(type: String, name: String, lifespanKm: Double) {
        viewModelScope.launch {
            componentRepository.insertComponent(
                ComponentEntity(
                    bikeId = null,
                    type = type,
                    name = name,
                    lifespanKm = lifespanKm,
                    installedAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}
