package com.you.bikecompanion.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.ai.AiApiClient
import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.component.ComponentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
)

data class AiUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    /** When true, UI should show error Snackbar (e.g. common_error) then call consumeError(). */
    val errorOccurred: Boolean = false,
)

class AiViewModel @Inject constructor(
    private val bikeRepository: BikeRepository,
    private val componentRepository: ComponentRepository,
    private val aiApiClient: AiApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private var nextId = 0L

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        _uiState.update {
            it.copy(
                inputText = "",
                messages = it.messages + ChatMessage(id = nextId++, text = text, isUser = true),
                isLoading = true,
            )
        }
        viewModelScope.launch {
            val summary = withContext(Dispatchers.IO) { buildComponentHealthSummary() }
            val result = aiApiClient.send(userMessage = text, componentHealthSummary = summary)
            result.fold(
                onSuccess = { reply ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages + ChatMessage(id = nextId++, text = reply, isUser = false),
                            isLoading = false,
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(isLoading = false, errorOccurred = true)
                    }
                },
            )
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorOccurred = false) }
    }

    private suspend fun buildComponentHealthSummary(): String {
        val bikes = bikeRepository.getAllBikes().first()
        val components = componentRepository.getAllComponents()
        if (bikes.isEmpty()) return "No bikes in garage."
        val byBike = components.groupBy { it.bikeId }
        return bikes.joinToString("\n") { bike ->
            val comps = byBike[bike.id].orEmpty()
            val bikeLine = "${bike.name}: ${bike.totalDistanceKm.toInt()} km total"
            if (comps.isEmpty()) bikeLine
            else bikeLine + "\n  " + comps.joinToString("; ") { c ->
                val health = componentHealthPercent(c)
                "${c.name}: ${health}%"
            }
        }
    }

    private fun componentHealthPercent(c: ComponentEntity): Int {
        if (c.lifespanKm <= 0) return 100
        val usedPercent = (c.distanceUsedKm / c.lifespanKm) * 100
        return (100 - usedPercent).toInt().coerceIn(0, 100)
    }
}
