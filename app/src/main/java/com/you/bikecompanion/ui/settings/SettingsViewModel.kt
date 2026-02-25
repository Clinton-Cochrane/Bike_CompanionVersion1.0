package com.you.bikecompanion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.preferences.AppPreferencesRepository
import com.you.bikecompanion.data.preferences.SecurePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val closeToServiceHealthThreshold: Int = AppPreferencesRepository.DEFAULT_CLOSE_TO_SERVICE_THRESHOLD,
    /** True when a Gemini API key is stored (show masked field and Clear). */
    val hasStoredApiKey: Boolean = false,
    /** Current text in the API key field (when adding/editing). */
    val apiKeyInput: String = "",
    /** Snackbar to show once; UI maps to string resource and then calls consumeSnackbarMessage(). */
    val snackbarMessage: SettingsSnackbar? = null,
)

enum class SettingsSnackbar { ApiKeySaved, ApiKeyCleared }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val securePreferences: SecurePreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferencesRepository.closeToServiceHealthThreshold.collect { threshold ->
                _uiState.update { it.copy(closeToServiceHealthThreshold = threshold) }
            }
        }
        viewModelScope.launch {
            securePreferences.apiKey.collect { key ->
                _uiState.update { it.copy(hasStoredApiKey = !key.isNullOrBlank()) }
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

    fun updateApiKeyInput(text: String) {
        _uiState.update { it.copy(apiKeyInput = text) }
    }

    fun saveApiKey() {
        val trimmed = _uiState.value.apiKeyInput.trim()
        if (trimmed.isEmpty()) return
        securePreferences.setApiKey(trimmed)
        _uiState.update {
            it.copy(apiKeyInput = "", snackbarMessage = SettingsSnackbar.ApiKeySaved)
        }
    }

    fun clearApiKey() {
        securePreferences.clearApiKey()
        _uiState.update { it.copy(apiKeyInput = "", snackbarMessage = SettingsSnackbar.ApiKeyCleared) }
    }

    fun consumeSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
