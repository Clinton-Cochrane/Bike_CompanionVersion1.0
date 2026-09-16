package com.clintoncochrane.bikecompanion.ui.garage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.bike.BikeDeletionComponentDisposition
import com.clintoncochrane.bikecompanion.data.bike.BikeDeletionRepository
import com.clintoncochrane.bikecompanion.data.bike.BikeRepository
import com.clintoncochrane.bikecompanion.data.bike.withBaselineDistanceKm
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Result of saving a bike: navigate to new bike detail (after seeding) or back (edit). */
sealed class SaveOutcome {
    data class NewBike(val id: Long) : SaveOutcome()
    data object Updated : SaveOutcome()
}

sealed class BikeDeletionPrompt {
    data object WithoutInstalledComponents : BikeDeletionPrompt()
    data object WithInstalledComponents : BikeDeletionPrompt()
}

data class AddEditBikeUiState(
    val bike: BikeEntity? = null,
    val saveOutcome: SaveOutcome? = null,
    val bikeDeletionPrompt: BikeDeletionPrompt? = null,
    val bikeDeleted: Boolean = false,
)

@HiltViewModel
class AddEditBikeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bikeRepository: BikeRepository,
    private val componentRepository: ComponentRepository,
    private val bikeDeletionRepository: BikeDeletionRepository? = null,
) : ViewModel() {

    private val bikeId: Long? = savedStateHandle.get<String>("bikeId")?.toLongOrNull()?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(AddEditBikeUiState())
    val uiState: StateFlow<AddEditBikeUiState> = _uiState.asStateFlow()

    init {
        bikeId?.let { id ->
            viewModelScope.launch {
                val bike = bikeRepository.getBikeById(id)
                _uiState.update { it.copy(bike = bike) }
            }
        }
    }

    fun saveBike(
        bike: BikeEntity,
        startingOdometerInput: String = bike.baselineDistanceKm.toString(),
    ) {
        val startingOdometerKm = parseStartingOdometerKm(startingOdometerInput) ?: return
        viewModelScope.launch {
            val bikeToSave = bike.withBaselineDistanceKm(startingOdometerKm)

            if (bike.id > 0) {
                bikeRepository.updateBike(bikeToSave)
                _uiState.update {
                    it.copy(saveOutcome = SaveOutcome.Updated)
                }
            } else {
                val newId = bikeRepository.insertBike(bikeToSave)
                componentRepository.seedDefaultComponentsIfEmpty(newId, bikeToSave.baselineDistanceKm)
                _uiState.update {
                    it.copy(saveOutcome = SaveOutcome.NewBike(newId))
                }
            }
        }
    }

    fun clearSaveOutcome() {
        _uiState.update { it.copy(saveOutcome = null) }
    }

    fun requestBikeDeletion() {
        val bike = _uiState.value.bike ?: return
        viewModelScope.launch {
            val prompt = if (componentRepository.getComponentsByBikeIdOnce(bike.id).isEmpty()) {
                BikeDeletionPrompt.WithoutInstalledComponents
            } else {
                BikeDeletionPrompt.WithInstalledComponents
            }
            _uiState.update { it.copy(bikeDeletionPrompt = prompt) }
        }
    }

    fun confirmBikeDeletion(componentDisposition: BikeDeletionComponentDisposition) {
        val bike = _uiState.value.bike ?: return
        viewModelScope.launch {
            requireNotNull(bikeDeletionRepository).deleteBike(bike, componentDisposition)
            _uiState.update { it.copy(bikeDeletionPrompt = null, bikeDeleted = true) }
        }
    }

    fun cancelBikeDeletion() {
        _uiState.update { it.copy(bikeDeletionPrompt = null) }
    }

    fun clearBikeDeleted() {
        _uiState.update { it.copy(bikeDeleted = false) }
    }
}
