package com.you.bikecompanion.ui.garage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.bike.BikeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditBikeUiState(
    val bike: BikeEntity? = null,
)

@HiltViewModel
class AddEditBikeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bikeRepository: BikeRepository,
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
            } else {
                bikeRepository.insertBike(bike)
            }
        }
    }
}
