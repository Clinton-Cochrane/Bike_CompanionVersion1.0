package com.you.bikecompanion.ui.garage

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.component.ComponentContext
import com.you.bikecompanion.data.component.ComponentContextRepository
import com.you.bikecompanion.data.component.ComponentContextValidation
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.component.ComponentRepository
import com.you.bikecompanion.data.component.ComponentSwapEntity
import com.you.bikecompanion.data.component.ComponentSwapRepository
import com.you.bikecompanion.data.component.ServiceIntervalEntity
import com.you.bikecompanion.data.component.ServiceIntervalRepository
import com.you.bikecompanion.data.image.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComponentDetailUiState(
    val component: ComponentEntity? = null,
    val context: ComponentContext? = null,
    val swaps: List<ComponentSwapEntity> = emptyList(),
    val serviceIntervals: List<ServiceIntervalEntity> = emptyList(),
    val bikes: List<BikeEntity> = emptyList(),
    val loading: Boolean = true,
    /** For swap picker: bikeId -> true if install would succeed, false if duplicate. */
    val swapBikeCanInstall: Map<Long, Boolean> = emptyMap(),
)

@HiltViewModel
class ComponentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val componentRepository: ComponentRepository,
    private val componentContextRepository: ComponentContextRepository,
    private val componentSwapRepository: ComponentSwapRepository,
    private val serviceIntervalRepository: ServiceIntervalRepository,
    private val bikeRepository: BikeRepository,
    private val imageRepository: ImageRepository,
) : ViewModel() {

    private val componentId: Long = savedStateHandle.get<String>("componentId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(ComponentDetailUiState())
    val uiState: StateFlow<ComponentDetailUiState> = _uiState.asStateFlow()

    init {
        if (componentId > 0) {
            viewModelScope.launch {
                val component = componentRepository.getComponentById(componentId)
                _uiState.update { it.copy(component = component, loading = component == null) }
            }
            viewModelScope.launch {
                componentContextRepository.getComponentContext(componentId).let { ctx ->
                    _uiState.update { it.copy(context = ctx) }
                }
            }
            viewModelScope.launch {
                componentSwapRepository.getSwapsByComponentId(componentId).collect { list ->
                    _uiState.update { it.copy(swaps = list) }
                }
            }
            viewModelScope.launch {
                serviceIntervalRepository.getIntervalsByComponentId(componentId).collect { list ->
                    _uiState.update { it.copy(serviceIntervals = list) }
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
    }

    /**
     * Loads which target bikes can receive this component (no duplicate type+position).
     * Call when opening the swap/install picker.
     */
    fun loadSwapBikeStatus() {
        val component = _uiState.value.component ?: return
        viewModelScope.launch {
            val bikes = _uiState.value.bikes.filter { it.id != component.bikeId }
            val status = bikes.associate { bike ->
                bike.id to !componentRepository.wouldBeDuplicatePart(component, bike.id)
            }
            _uiState.update { it.copy(swapBikeCanInstall = status) }
        }
    }

    fun installComponent(targetBikeId: Long) {
        val component = _uiState.value.component ?: return
        viewModelScope.launch {
            if (componentRepository.wouldBeDuplicatePart(component, targetBikeId)) return@launch
            componentRepository.installComponent(component, targetBikeId)
            val refreshed = componentRepository.getComponentById(component.id)
            _uiState.update { it.copy(component = refreshed, swapBikeCanInstall = emptyMap()) }
        }
    }

    fun uninstallComponent() {
        val component = _uiState.value.component ?: return
        viewModelScope.launch {
            componentRepository.uninstallComponent(component)
            val refreshed = componentRepository.getComponentById(component.id)
            _uiState.update { it.copy(component = refreshed) }
        }
    }

    fun saveComponentContext(payload: ComponentContext, onResult: (ComponentContextValidation) -> Unit) {
        viewModelScope.launch {
            val result = componentContextRepository.upsertComponentContext(componentId, payload)
            if (result is ComponentContextValidation.Ok) {
                val updated = componentContextRepository.getComponentContext(componentId)
                _uiState.update { it.copy(context = updated) }
            }
            onResult(result)
        }
    }

    fun addServiceInterval(
        name: String,
        intervalKm: Double,
        type: String,
        intervalTimeSeconds: Long? = null,
    ) {
        viewModelScope.launch {
            val component = _uiState.value.component ?: return@launch
            serviceIntervalRepository.insertInterval(
                ServiceIntervalEntity(
                    componentId = componentId,
                    name = name,
                    intervalKm = intervalKm,
                    trackedKm = component.distanceUsedKm,
                    type = type,
                    intervalTimeSeconds = intervalTimeSeconds,
                    trackedTimeSeconds = if (intervalTimeSeconds != null) component.totalTimeSeconds else null,
                ),
            )
        }
    }

    fun updateServiceInterval(interval: ServiceIntervalEntity) {
        viewModelScope.launch {
            serviceIntervalRepository.updateInterval(interval)
        }
    }

    fun deleteServiceInterval(id: Long) {
        viewModelScope.launch {
            serviceIntervalRepository.deleteInterval(id)
        }
    }

    fun deleteComponent() {
        val component = _uiState.value.component ?: return
        viewModelScope.launch {
            componentRepository.deleteComponent(component)
        }
    }

    /**
     * Updates component display name, mileage, time, optionally resets avg/max speed,
     * and optionally updates thumbnail image.
     * Refreshes UI state after a successful update.
     */
    fun updateComponent(
        name: String,
        distanceUsedKm: Double,
        totalTimeSeconds: Long,
        resetAvgMaxSpeed: Boolean,
        pickedImageUri: Uri? = null,
        removeImage: Boolean = false,
    ) {
        val component = _uiState.value.component ?: return
        viewModelScope.launch {
            var thumbnailUri = component.thumbnailUri
            if (removeImage) {
                imageRepository.deleteImageAtPath(component.thumbnailUri)
                thumbnailUri = null
            } else if (pickedImageUri != null) {
                thumbnailUri = imageRepository.saveComponentImage(component.id, pickedImageUri)
            }

            val updated = component.copy(
                name = name.trim(),
                distanceUsedKm = distanceUsedKm.coerceAtLeast(0.0),
                totalTimeSeconds = totalTimeSeconds.coerceAtLeast(0L),
                avgSpeedKmh = if (resetAvgMaxSpeed) 0.0 else component.avgSpeedKmh,
                maxSpeedKmh = if (resetAvgMaxSpeed) 0.0 else component.maxSpeedKmh,
                maxSpeedBikeId = if (resetAvgMaxSpeed) null else component.maxSpeedBikeId,
                thumbnailUri = thumbnailUri,
            )
            componentRepository.updateComponent(updated)
            val refreshed = componentRepository.getComponentById(component.id)
            _uiState.update { it.copy(component = refreshed) }
        }
    }
}
