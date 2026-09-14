package com.clintoncochrane.bikecompanion.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WallOfHonorUiState(
    val components: List<ComponentEntity> = emptyList(),
)

@HiltViewModel
class WallOfHonorViewModel @Inject constructor(
    private val componentRepository: ComponentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WallOfHonorUiState())
    val uiState: StateFlow<WallOfHonorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            componentRepository.getRetiredComponents().collect { components ->
                _uiState.update { it.copy(components = components) }
            }
        }
    }
}
