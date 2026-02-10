package com.you.bikecompanion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.preferences.SecurePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val apiKeyFieldValue: String = "",
    val hasStoredKey: Boolean = false,
    val snackbarEvent: SnackbarEvent? = null,
)

sealed class SnackbarEvent {
    data object KeySaved : SnackbarEvent()
    data object KeyCleared : SnackbarEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securePreferences: SecurePreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            securePreferences.apiKey.collect { key ->
                _uiState.update { it.copy(hasStoredKey = !key.isNullOrBlank()) }
            }
        }
    }

    fun updateApiKeyField(value: String) {
        _uiState.update { it.copy(apiKeyFieldValue = value) }
    }

    fun saveApiKey() {
        val value = _uiState.value.apiKeyFieldValue.trim()
        securePreferences.setApiKey(value)
        _uiState.update { it.copy(snackbarEvent = SnackbarEvent.KeySaved) }
    }

    fun clearApiKey() {
        securePreferences.clearApiKey()
        _uiState.update {
            it.copy(apiKeyFieldValue = "", snackbarEvent = SnackbarEvent.KeyCleared)
        }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarEvent = null) }
    }
}
