package com.you.bikecompanion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.preferences.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val closeToServiceHealthThreshold: Int = AppPreferencesRepository.DEFAULT_CLOSE_TO_SERVICE_THRESHOLD,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferencesRepository.closeToServiceHealthThreshold.collect { threshold ->
                _uiState.update { it.copy(closeToServiceHealthThreshold = threshold) }
            }
        }
    }

    fun setCloseToServiceHealthThreshold(value: Int) {
        val clamped = value.coerceIn(
            AppPreferencesRepository.MIN_THRESHOLD,
            AppPreferencesRepository.MAX_THRESHOLD,
        )
        _uiState.update { it.copy(closeToServiceHealthThreshold = clamped) }
        viewModelScope.launch {
            appPreferencesRepository.setCloseToServiceHealthThreshold(clamped)
        }
    }
}
