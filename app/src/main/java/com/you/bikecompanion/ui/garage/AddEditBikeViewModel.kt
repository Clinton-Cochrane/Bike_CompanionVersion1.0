package com.you.bikecompanion.ui.garage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.component.ComponentRepository
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

data class AddEditBikeUiState(
    val bike: BikeEntity? = null,
    val saveOutcome: SaveOutcome? = null,
)

@HiltViewModel
class AddEditBikeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bikeRepository: BikeRepository,
    private val componentRepository: ComponentRepository,
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

    fun saveBike(bike: BikeEntity) {
        viewModelScope.launch {
            if (bike.id > 0) {
                bikeRepository.updateBike(bike)
                _uiState.update { it.copy(saveOutcome = SaveOutcome.Updated) }
            } else {
                val newId = bikeRepository.insertBike(bike)
                componentRepository.seedDefaultComponentsIfEmpty(newId)
                _uiState.update { it.copy(saveOutcome = SaveOutcome.NewBike(newId)) }
            }
        }
    }

    fun clearSaveOutcome() {
        _uiState.update { it.copy(saveOutcome = null) }
    }
}
