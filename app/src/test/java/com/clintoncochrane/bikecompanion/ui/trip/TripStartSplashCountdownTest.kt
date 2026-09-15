package com.clintoncochrane.bikecompanion.ui.trip

import org.junit.Assert.assertEquals
import org.junit.Test

class TripStartSplashCountdownTest {

    @Test
    fun initialState_startsAtTenSeconds() {
        assertEquals(10, SplashState().countdown)
    }

    @Test
    fun extend_repeatedlyAddsTenSeconds() {
        val afterFirstExtension = TripStartCountdown.extend(SplashState(countdown = 3))
        val afterSecondExtension = TripStartCountdown.extend(afterFirstExtension)

        assertEquals(23, afterSecondExtension.countdown)
    }

    @Test
    fun tick_afterCancel_doesNotStartTheRide() {
        val result = TripStartCountdown.tick(SplashState(countdown = 1, isCancelled = true))

        assertEquals(false, result.shouldStartRide)
        assertEquals(1, result.state.countdown)
    }

    @Test
    fun tick_atZero_startsTheRideOnlyOnce() {
        val firstTick = TripStartCountdown.tick(SplashState(countdown = 1))
        val repeatedTick = TripStartCountdown.tick(firstTick.state)

        assertEquals(true, firstTick.shouldStartRide)
        assertEquals(0, firstTick.state.countdown)
        assertEquals(false, repeatedTick.shouldStartRide)
    }
}
