package com.you.bikecompanion.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.component.ComponentRepository
import com.you.bikecompanion.data.component.ServiceIntervalEntity
import com.you.bikecompanion.data.component.ServiceIntervalRepository
import com.you.bikecompanion.data.preferences.AppPreferencesRepository
import com.you.bikecompanion.util.ComponentSortOrder
import com.you.bikecompanion.util.ServiceIntervalHelper
import com.you.bikecompanion.util.componentHealthPercent
import com.you.bikecompanion.util.sortComponents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A component due for service, with bike name, health, and next due interval text.
 */
data class DueServiceItem(
    val component: ComponentEntity,
    val bikeName: String,
    val healthPercent: Int,
    val nextDueText: String,
)

data class ServiceListUiState(
    val bikes: List<BikeEntity> = emptyList(),
    val allDueItems: List<DueServiceItem> = emptyList(),
    val dueItems: List<DueServiceItem> = emptyList(),
    val closeToServiceThreshold: Int = AppPreferencesRepository.DEFAULT_CLOSE_TO_SERVICE_THRESHOLD,
    val bikeFilterId: Long? = null,
    val componentTypeFilter: String? = null,
    val sortOrder: ComponentSortOrder = ComponentSortOrder.NEXT_SERVICE,
    val searchQuery: String = "",
    val selectedIds: Set<Long> = emptySet(),
)

@HiltViewModel
class ServiceListViewModel @Inject constructor(
    private val bikeRepository: BikeRepository,
    private val componentRepository: ComponentRepository,
    private val serviceIntervalRepository: ServiceIntervalRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServiceListUiState())
    val uiState: StateFlow<ServiceListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferencesRepository.closeToServiceHealthThreshold.collect { threshold ->
                _uiState.update { it.copy(closeToServiceThreshold = threshold) }
            }
        }
        viewModelScope.launch {
            bikeRepository.getAllBikes().collect { bikes ->
                _uiState.update { it.copy(bikes = bikes) }
            }
        }
        viewModelScope.launch {
            componentRepository.getAllComponentsFlow().collect { components ->
                val ids = components.map { it.id }
                val intervals = if (ids.isEmpty()) emptyList()
                else serviceIntervalRepository.getIntervalsByComponentIdsOnce(ids)
                val intervalsByComponent = intervals.groupBy { it.componentId }
                val due = computeDueItems(components, intervalsByComponent)
                val state = _uiState.value.copy(allDueItems = due)
                val filtered = applyFiltersAndSort(due, state)
                _uiState.update { it.copy(allDueItems = due, dueItems = filtered) }
            }
        }
    }

    private fun computeDueItems(
        components: List<ComponentEntity>,
        intervalsByComponentId: Map<Long, List<ServiceIntervalEntity>>,
    ): List<DueServiceItem> {
        val bikes = _uiState.value.bikes
        val threshold = _uiState.value.closeToServiceThreshold
        return components
            .filter { component ->
                val componentHealth = componentHealthPercent(component)
                val intervalHealth = intervalsByComponentId[component.id]
                    ?.minOfOrNull { ServiceIntervalHelper.healthPercent(it) }
                    ?: 100
                val minHealth = minOf(componentHealth, intervalHealth)
                minHealth <= threshold
            }
            .map { component ->
                val bikeName = component.bikeId?.let { bid ->
                    bikes.find { it.id == bid }?.name ?: ""
                } ?: ""
                val componentHealth = componentHealthPercent(component)
                val intervals = intervalsByComponentId[component.id] ?: emptyList()
                val intervalHealth = intervals.minOfOrNull { ServiceIntervalHelper.healthPercent(it) } ?: 100
                val health = minOf(componentHealth, intervalHealth)
                val nextDue = intervals.minByOrNull { ServiceIntervalHelper.healthPercent(it) }
                val nextDueText = nextDue?.let { ServiceIntervalHelper.description(it) }
                    ?.let { desc ->
                        listOfNotNull(desc.kmText, desc.timeText).joinToString(" · ")
                    } ?: ""
                DueServiceItem(
                    component = component,
                    bikeName = bikeName,
                    healthPercent = health,
                    nextDueText = nextDueText,
                )
            }
    }

    private fun applyFiltersAndSort(
        allDue: List<DueServiceItem>,
        state: ServiceListUiState,
    ): List<DueServiceItem> {
        var filtered = allDue
        if (state.bikeFilterId != null) {
            filtered = filtered.filter { it.component.bikeId == state.bikeFilterId }
        }
        if (state.componentTypeFilter != null) {
            filtered = filtered.filter { it.component.type == state.componentTypeFilter }
        }
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase().trim()
            filtered = filtered.filter {
                it.component.name.lowercase().contains(q) ||
                    it.component.type.lowercase().contains(q) ||
                    it.bikeName.lowercase().contains(q)
            }
        }
        return when (state.sortOrder) {
            ComponentSortOrder.NEXT_SERVICE -> filtered.sortedBy { it.healthPercent }
            ComponentSortOrder.HEALTH -> filtered.sortedBy { it.healthPercent }
            ComponentSortOrder.TYPE_AZ -> filtered.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { item: DueServiceItem -> item.component.type }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { item: DueServiceItem -> item.component.name },
            )
        }
    }

    fun setBikeFilter(bikeId: Long?) {
        _uiState.update { state ->
            val next = state.copy(bikeFilterId = bikeId)
            next.copy(dueItems = applyFiltersAndSort(next.allDueItems, next))
        }
    }

    fun setComponentTypeFilter(type: String?) {
        _uiState.update { state ->
            val next = state.copy(componentTypeFilter = type)
            next.copy(dueItems = applyFiltersAndSort(next.allDueItems, next))
        }
    }

    fun setSortOrder(order: ComponentSortOrder) {
        _uiState.update { state ->
            val next = state.copy(sortOrder = order)
            next.copy(dueItems = applyFiltersAndSort(next.allDueItems, next))
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val next = state.copy(searchQuery = query)
            next.copy(dueItems = applyFiltersAndSort(next.allDueItems, next))
        }
    }

    fun toggleSelection(componentId: Long) {
        _uiState.update { state ->
            val next = if (componentId in state.selectedIds) {
                state.selectedIds - componentId
            } else {
                state.selectedIds + componentId
            }
            state.copy(selectedIds = next)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedIds = state.dueItems.map { it.component.id }.toSet())
        }
    }

    fun selectNone() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun replaceComponent(componentId: Long) {
        viewModelScope.launch {
            val component = componentRepository.getComponentById(componentId) ?: return@launch
            componentRepository.markComponentReplaced(component)
            _uiState.update { state ->
                state.copy(selectedIds = state.selectedIds - componentId)
            }
        }
    }

    fun replaceSelected() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toList()
            ids.forEach { id ->
                val component = componentRepository.getComponentById(id) ?: return@forEach
                componentRepository.markComponentReplaced(component)
            }
            _uiState.update { it.copy(selectedIds = emptySet()) }
        }
    }

    fun completeInspection(componentId: Long) {
        viewModelScope.launch {
            componentRepository.markInspectionComplete(componentId)
            _uiState.update { state ->
                state.copy(selectedIds = state.selectedIds - componentId)
            }
        }
    }

    fun completeInspectionSelected() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toList()
            ids.forEach { componentRepository.markInspectionComplete(it) }
            _uiState.update { it.copy(selectedIds = emptySet()) }
        }
    }
}
