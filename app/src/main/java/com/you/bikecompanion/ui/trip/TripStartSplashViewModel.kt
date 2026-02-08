package com.you.bikecompanion.ui.trip

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for the trip-start splash countdown screen.
 *
 * If the app is backgrounded during the countdown, on resume the countdown resets to 5.
 */
data class SplashState(
    val countdown: Int = INITIAL_COUNTDOWN,
    val isCancelled: Boolean = false,
)

/** One-shot event: trip should start (countdown reached 0 and not cancelled). */
object StartTripEvent

private const val INITIAL_COUNTDOWN = 5

@HiltViewModel
class TripStartSplashViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    application: Application,
) : ViewModel() {

    val bikeId: Long = savedStateHandle.get<String>("bikeId")?.toLongOrNull() ?: -1L

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    private val _startTripEvents = MutableSharedFlow<StartTripEvent>(replay = 0, extraBufferCapacity = 1)
    val startTripEvents: SharedFlow<StartTripEvent> = _startTripEvents.asSharedFlow()

    private var countdownJob: Job? = null

    init {
        startCountdown()
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) startCountdown()
            }
        )
    }

    /**
     * Starts the countdown from [INITIAL_COUNTDOWN] down to 0.
     * When the app is resumed from background, the lifecycle observer resets and calls this again.
     */
    private fun startCountdown() {
        countdownJob?.cancel()
        _state.update { it.copy(countdown = INITIAL_COUNTDOWN, isCancelled = it.isCancelled) }
        countdownJob = viewModelScope.launch {
            var current = INITIAL_COUNTDOWN
            while (current > 0) {
                delay(1000)
                if (_state.value.isCancelled) return@launch
                current--
                _state.update { it.copy(countdown = current) }
            }
            if (!_state.value.isCancelled && bikeId >= 0) {
                _startTripEvents.emit(StartTripEvent)
            }
        }
    }

    fun cancel() {
        _state.update { it.copy(isCancelled = true) }
    }
}
