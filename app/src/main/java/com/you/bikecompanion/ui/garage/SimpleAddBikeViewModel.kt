package com.you.bikecompanion.ui.garage

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

data class SimpleAddBikeUiState(
    val saveOutcome: SaveOutcome? = null,
)

@HiltViewModel
class SimpleAddBikeViewModel @Inject constructor(
    private val bikeRepository: BikeRepository,
    private val componentRepository: ComponentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SimpleAddBikeUiState())
    val uiState: StateFlow<SimpleAddBikeUiState> = _uiState.asStateFlow()

    fun saveBike(name: String, drivetrainType: String, brakeType: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            val bike = BikeEntity(
                name = trimmedName,
                drivetrainType = drivetrainType,
                brakeType = brakeType,
                createdAt = System.currentTimeMillis(),
            )
            val newId = bikeRepository.insertBike(bike)
            componentRepository.seedComponentsForBikeType(newId, drivetrainType, brakeType)
            _uiState.update { it.copy(saveOutcome = SaveOutcome.NewBike(newId)) }
        }
    }

    fun clearSaveOutcome() {
        _uiState.update { it.copy(saveOutcome = null) }
    }
}
