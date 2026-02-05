package com.you.bikecompanion.ui.garage

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

data class GarageUiState(
    val bikes: List<BikeEntity> = emptyList(),
)

@HiltViewModel
class GarageViewModel @Inject constructor(
    private val bikeRepository: BikeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GarageUiState())
    val uiState: StateFlow<GarageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bikeRepository.getAllBikes().collect { bikes ->
                _uiState.update { it.copy(bikes = bikes) }
            }
        }
    }
}
