package com.you.bikecompanion.util

import com.you.bikecompanion.location.RideState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [computeElapsedMovingMs] and the active ride timer display pipeline.
 *
 * ActiveRideScreen displays duration using:
 * 1. [computeElapsedMovingMs](state, System.currentTimeMillis()) for elapsed moving time
 * 2. [DurationFormatHelper.formatDurationBreakdownMs] for formatting
 * 3. A tick (Handler + DisposableEffect posting every 1s) to trigger recomposition
 *
 * The tick is critical: without it, the display would not update every second.
 * These tests verify the calculation and formatted output so we don't break the timer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RideTimerHelperTest {

    @Test
    fun timerTicks_whenActive_elapsedIncreasesWithTime() = runTest {
        val startTimeMs = 1000L
        val state = RideState(
            isTracking = true,
            isPaused = false,
            startTimeMs = startTimeMs,
        )
        assertEquals(0L, computeElapsedMovingMs(state, 1000))
        assertEquals(1000L, computeElapsedMovingMs(state, 2000))
        assertEquals(2000L, computeElapsedMovingMs(state, 3000))
        assertEquals(5000L, computeElapsedMovingMs(state, 6000))
    }

    @Test
    fun timerFrozen_whenPaused_elapsedStaysConstant() = runTest {
        val startTimeMs = 1000L
        val pausedAtMs = 3000L
        val state = RideState(
            isTracking = true,
            isPaused = true,
            pausedAtMs = pausedAtMs,
            totalPausedDurationMs = 0L,
            startTimeMs = startTimeMs,
        )
        val elapsedWhenPaused = 2000L
        assertEquals(elapsedWhenPaused, computeElapsedMovingMs(state, 3000))
        assertEquals(elapsedWhenPaused, computeElapsedMovingMs(state, 4000))
        assertEquals(elapsedWhenPaused, computeElapsedMovingMs(state, 10000))
    }

    @Test
    fun timerResumes_afterPause_elapsedIncreasesFromFrozenValue() = runTest {
        val startTimeMs = 1000L
        val totalPausedMs = 2000L
        val state = RideState(
            isTracking = true,
            isPaused = false,
            totalPausedDurationMs = totalPausedMs,
            startTimeMs = startTimeMs,
        )
        assertEquals(0L, computeElapsedMovingMs(state, 3000))
        assertEquals(1000L, computeElapsedMovingMs(state, 4000))
        assertEquals(2000L, computeElapsedMovingMs(state, 5000))
    }

    @Test
    fun timerZero_whenStartTimeNotSet() = runTest {
        val state = RideState(isTracking = false, startTimeMs = 0L)
        assertEquals(0L, computeElapsedMovingMs(state, 99999))
    }

    /**
     * When ending a ride, the saved duration should equal computeElapsedMovingMs(state, endTime).
     * This test validates the logic used by stopRideAndSave to compute moving duration.
     */
    @Test
    fun rideEnding_activeRide_savedDurationExcludesPausedTime() = runTest {
        val startTimeMs = 1000L
        val endTimeMs = 10000L
        val totalPausedMs = 2000L
        val state = RideState(
            isTracking = true,
            isPaused = false,
            startTimeMs = startTimeMs,
            totalPausedDurationMs = totalPausedMs,
        )
        val expectedMovingMs = endTimeMs - startTimeMs - totalPausedMs
        assertEquals(expectedMovingMs, computeElapsedMovingMs(state, endTimeMs))
        assertEquals(7000L, expectedMovingMs)
    }

    /**
     * Simulates the timer tick: when currentTime advances by 1 second,
     * elapsed moving time should increase by 1 second. Validates the
     * display-update logic used by the ride screen timer.
     */
    @Test
    fun timerDisplay_advancesOneSecondPerTick() = runTest {
        val startTimeMs = 1000L
        val state = RideState(
            isTracking = true,
            isPaused = false,
            startTimeMs = startTimeMs,
        )
        var currentTimeMs = 2000L
        assertEquals(1000L, computeElapsedMovingMs(state, currentTimeMs))
        currentTimeMs += 1000
        assertEquals(2000L, computeElapsedMovingMs(state, currentTimeMs))
        currentTimeMs += 1000
        assertEquals(3000L, computeElapsedMovingMs(state, currentTimeMs))
    }

    /**
     * Verifies the full active ride display pipeline: computeElapsedMovingMs +
     * formatDurationBreakdownMs. Simulates multiple timer ticks and asserts
     * the formatted output advances each second.
     *
     * ActiveRideScreen uses Handler + DisposableEffect to produce a tick every
     * second; each tick triggers recomposition with fresh System.currentTimeMillis().
     * This test would fail if the calculation or formatting were broken.
     */
    @Test
    fun activeRideDisplay_formattedOutput_advancesWithEachTick() = runTest {
        val startTimeMs = 1000L
        val state = RideState(
            isTracking = true,
            isPaused = false,
            startTimeMs = startTimeMs,
        )
        val expectedFormats = listOf("0s", "1s", "2s", "3s", "4s", "5s")
        for ((tickIndex, expected) in expectedFormats.withIndex()) {
            val currentTimeMs = startTimeMs + (tickIndex * 1000L)
            val elapsedMs = computeElapsedMovingMs(state, currentTimeMs)
            val formatted = DurationFormatHelper.formatDurationBreakdownMs(
                durationMs = elapsedMs,
                capAt24h = false,
            )
            assertEquals(
                "Tick $tickIndex: elapsed=${elapsedMs}ms should format as '$expected'",
                expected,
                formatted,
            )
        }
    }

    /**
     * When paused, the formatted display stays constant across simulated ticks.
     * Validates that paused rides show frozen duration (no tick-driven updates needed).
     */
    @Test
    fun activeRideDisplay_whenPaused_formattedOutputStaysConstant() = runTest {
        val startTimeMs = 1000L
        val pausedAtMs = 4000L
        val state = RideState(
            isTracking = true,
            isPaused = true,
            startTimeMs = startTimeMs,
            pausedAtMs = pausedAtMs,
            totalPausedDurationMs = 0L,
        )
        val expectedFormatted = "3s"
        val elapsedMs = computeElapsedMovingMs(state, pausedAtMs)
        assertEquals(3000L, elapsedMs)
        assertEquals(
            expectedFormatted,
            DurationFormatHelper.formatDurationBreakdownMs(elapsedMs, capAt24h = false),
        )
        assertEquals(
            "Frozen duration should stay 3s even when currentTime advances",
            expectedFormatted,
            DurationFormatHelper.formatDurationBreakdownMs(
                computeElapsedMovingMs(state, 10000L),
                capAt24h = false,
            ),
        )
    }

    @Test
    fun rideEnding_pausedRide_savedDurationEqualsFrozenValue() = runTest {
        val startTimeMs = 1000L
        val pausedAtMs = 5000L
        val endTimeMs = 8000L
        val totalPausedMs = 1000L
        val state = RideState(
            isTracking = true,
            isPaused = true,
            startTimeMs = startTimeMs,
            pausedAtMs = pausedAtMs,
            totalPausedDurationMs = totalPausedMs,
        )
        val expectedMovingMs = pausedAtMs - startTimeMs - totalPausedMs
        assertEquals(expectedMovingMs, computeElapsedMovingMs(state, endTimeMs))
        assertEquals(3000L, expectedMovingMs)
    }
}
