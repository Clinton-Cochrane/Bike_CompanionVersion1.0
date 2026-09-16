package com.clintoncochrane.bikecompanion.ui.trip

data class CountdownTickResult(
    val state: SplashState,
    val shouldStartRide: Boolean,
)

/** Pure countdown state transitions, kept separate from the ViewModel clock for JVM coverage. */
object TripStartCountdown {
    const val INITIAL_COUNTDOWN = 10

    fun start(state: SplashState): SplashState =
        if (state.isCancelled || state.hasStarted) state else state.copy(
            countdown = INITIAL_COUNTDOWN,
            isCountdownAuthorized = true,
        )

    fun extend(state: SplashState): SplashState =
        if (state.isCancelled || state.hasStarted) state else state.copy(countdown = state.countdown + 10)

    fun tick(state: SplashState): CountdownTickResult {
        if (!state.isCountdownAuthorized || state.isCancelled || state.hasStarted) {
            return CountdownTickResult(state = state, shouldStartRide = false)
        }

        val remainingSeconds = (state.countdown - 1).coerceAtLeast(0)
        val shouldStartRide = remainingSeconds == 0
        return CountdownTickResult(
            state = state.copy(countdown = remainingSeconds, hasStarted = shouldStartRide),
            shouldStartRide = shouldStartRide,
        )
    }
}
