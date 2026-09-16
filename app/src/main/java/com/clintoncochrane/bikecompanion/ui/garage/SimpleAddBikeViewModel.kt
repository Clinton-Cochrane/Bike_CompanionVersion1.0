package com.clintoncochrane.bikecompanion.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
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

    fun saveBike(
        name: String,
        drivetrainType: String,
        brakeType: String,
        startingOdometerInput: String = "0",
    ) {
        val trimmedName = name.trim()
        val startingOdometerKm = parseStartingOdometerKm(startingOdometerInput)
        if (trimmedName.isEmpty() || startingOdometerKm == null) return
        viewModelScope.launch {
            val bike = BikeEntity(
                name = trimmedName,
                drivetrainType = drivetrainType,
                brakeType = brakeType,
                createdAt = System.currentTimeMillis(),
            ).withBaselineDistanceKm(startingOdometerKm)
            val newId = bikeRepository.insertBike(bike)
            componentRepository.seedComponentsForBikeType(
                newId,
                drivetrainType,
                brakeType,
                startingOdometerKm,
            )
            _uiState.update { it.copy(saveOutcome = SaveOutcome.NewBike(newId)) }
        }
    }

    fun clearSaveOutcome() {
        _uiState.update { it.copy(saveOutcome = null) }
    }
}

internal fun parseStartingOdometerKm(input: String): Double? {
    val value = input.trim().ifEmpty { "0" }.toDoubleOrNull() ?: return null
    return value.takeIf { it.isFinite() && it >= 0.0 }
}
