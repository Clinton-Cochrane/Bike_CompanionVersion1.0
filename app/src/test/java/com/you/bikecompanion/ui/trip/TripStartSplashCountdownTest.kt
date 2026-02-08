package com.you.bikecompanion.ui.trip

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the trip-start splash countdown behavior.
 * Validates that countdown completion triggers start and that cancel prevents it.
 *
 * Tests a state machine that mirrors [TripStartSplashViewModel] countdown logic
 * so we can run on JVM without Android (ViewModel uses ProcessLifecycleOwner and Application).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TripStartSplashCountdownTest {

    @Test
    fun countdownReachingZero_withoutCancel_emitsStartTripEvent() = runTest {
        val events = MutableSharedFlow<StartTripEvent>(replay = 0, extraBufferCapacity = 1)
        var emitted = false
        val collectJob = launch {
            events.collect { emitted = true }
        }
        val countdownJob = launch {
            runCountdownLogic(
                initialCountdown = 5,
                isCancelled = false,
                onEmitStart = { events.tryEmit(StartTripEvent) },
                tickMs = 1000,
            )
        }
        advanceTimeBy(6000)
        countdownJob.cancel()
        collectJob.cancel()
        assertTrue(
            "Start trip event should be emitted when countdown reaches 0 without cancel",
            emitted,
        )
    }

    @Test
    fun countdownReachingZero_afterCancel_doesNotEmitStartTripEvent() = runTest {
        val events = MutableSharedFlow<StartTripEvent>(replay = 0, extraBufferCapacity = 1)
        var emitCount = 0
        val collectJob = launch {
            events.collect { emitCount++ }
        }
        val countdownJob = launch {
            runCountdownLogic(
                initialCountdown = 5,
                isCancelled = true,
                onEmitStart = { events.tryEmit(StartTripEvent) },
                tickMs = 1000,
            )
        }
        advanceTimeBy(6000)
        countdownJob.cancel()
        collectJob.cancel()
        assertEquals(
            "Start trip event must not be emitted when cancelled",
            0,
            emitCount,
        )
    }

    /**
     * Minimal state machine that mirrors the ViewModel's countdown + event logic:
     * count down from initialCountdown to 0; at 0, if not cancelled, emit once.
     */
    private suspend fun runCountdownLogic(
        initialCountdown: Int,
        isCancelled: Boolean,
        onEmitStart: () -> Unit,
        tickMs: Long,
    ) {
        var current = initialCountdown
        var cancelled = isCancelled
        while (current > 0) {
            kotlinx.coroutines.delay(tickMs)
            if (cancelled) return
            current--
        }
        if (!cancelled) onEmitStart()
    }
}
