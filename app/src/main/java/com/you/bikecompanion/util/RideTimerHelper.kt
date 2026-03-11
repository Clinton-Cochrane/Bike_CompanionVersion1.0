package com.you.bikecompanion.util

import com.you.bikecompanion.location.RideState

/**
 * Computes elapsed moving time for ride display.
 * When paused, returns frozen duration; when active, returns live duration.
 * Accepts [currentTimeMs] for testability (use System.currentTimeMillis() in production).
 */
fun computeElapsedMovingMs(state: RideState, currentTimeMs: Long): Long = when {
    state.startTimeMs <= 0 -> 0L
    state.isPaused && state.pausedAtMs > 0 ->
        (state.pausedAtMs - state.startTimeMs - state.totalPausedDurationMs).coerceAtLeast(0L)
    else ->
        (currentTimeMs - state.startTimeMs - state.totalPausedDurationMs).coerceAtLeast(0L)
}
