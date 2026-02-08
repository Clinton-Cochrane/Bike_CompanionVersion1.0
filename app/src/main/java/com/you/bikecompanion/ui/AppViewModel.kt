package com.you.bikecompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.bike.BikeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-level state used to choose the initial screen: Garage when no bikes (bike creation CTA),
 * Trip when at least one bike exists.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val bikeRepository: BikeRepository,
) : ViewModel() {

    private val _hasAnyBike = MutableStateFlow(false)
    val hasAnyBike: StateFlow<Boolean> = _hasAnyBike.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    init {
        viewModelScope.launch {
            _hasAnyBike.value = bikeRepository.hasAnyBike().first()
            _isInitialized.value = true
            bikeRepository.hasAnyBike().collect { _hasAnyBike.value = it }
        }
    }
}
