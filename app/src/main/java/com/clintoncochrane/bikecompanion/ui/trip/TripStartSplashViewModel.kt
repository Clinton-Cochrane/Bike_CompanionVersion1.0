package com.clintoncochrane.bikecompanion.ui.trip

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clintoncochrane.bikecompanion.data.bike.BikeRepository
import com.clintoncochrane.bikecompanion.util.DisplayFormatHelper
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
 * If the app is backgrounded during the countdown, on resume the countdown resets to 10.
 */
data class SplashState(
    val countdown: Int = TripStartCountdown.INITIAL_COUNTDOWN,
    val isCancelled: Boolean = false,
    val hasStarted: Boolean = false,
    val isCountdownAuthorized: Boolean = false,
)

/** One-shot event: trip should start (countdown reached 0 and not cancelled). */
object StartTripEvent

@HiltViewModel
class TripStartSplashViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    application: Application,
    private val bikeRepository: BikeRepository,
) : ViewModel() {

    val bikeId: Long = savedStateHandle.get<String>("bikeId")?.toLongOrNull() ?: -1L

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    private val _assignedBikeName = MutableStateFlow<String?>(null)
    val assignedBikeName: StateFlow<String?> = _assignedBikeName.asStateFlow()

    private val _startTripEvents = MutableSharedFlow<StartTripEvent>(replay = 0, extraBufferCapacity = 1)
    val startTripEvents: SharedFlow<StartTripEvent> = _startTripEvents.asSharedFlow()

    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            if (bikeId > 0L) {
                _assignedBikeName.value = bikeRepository.getBikeById(bikeId)?.let {
                    DisplayFormatHelper.bikeLabels(it.name, it.make, it.model).primary
                }
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START && _state.value.isCountdownAuthorized) {
                    startCountdown()
                }
            }
        )
    }

    /**
     * Starts the countdown from [TripStartCountdown.INITIAL_COUNTDOWN] down to 0.
     * When the app is resumed from background, the lifecycle observer resets and calls this again.
     */
    fun beginCountdown() {
        startCountdown()
    }

    private fun startCountdown() {
        if (_state.value.isCancelled || _state.value.hasStarted) return
        countdownJob?.cancel()
        _state.update(TripStartCountdown::start)
        countdownJob = viewModelScope.launch {
            while (!_state.value.isCancelled && !_state.value.hasStarted) {
                delay(1000)
                var shouldStartRide = false
                _state.update { state ->
                    TripStartCountdown.tick(state).also { shouldStartRide = it.shouldStartRide }.state
                }
                if (shouldStartRide) {
                    _startTripEvents.emit(StartTripEvent)
                }
            }
        }
    }

    fun cancel() {
        _state.update { it.copy(isCancelled = true) }
        countdownJob?.cancel()
        countdownJob = null
    }

    fun addTenSeconds() {
        _state.update(TripStartCountdown::extend)
    }
}
