package com.clintoncochrane.bikecompanion.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RideTrackingLifecyclePolicyTest {

    @Test
    fun canStartTracking_whenNoRideIsActive_returnsTrue() {
        assertTrue(RideTrackingLifecyclePolicy.canStartTracking(RideState()))
    }

    @Test
    fun canStartTracking_whenRideIsAlreadyActive_returnsFalse() {
        assertFalse(RideTrackingLifecyclePolicy.canStartTracking(RideState(isTracking = true)))
    }

    @Test
    fun canPauseTracking_onlyForAnActiveUnpausedRide() {
        assertTrue(RideTrackingLifecyclePolicy.canPauseTracking(RideState(isTracking = true)))
        assertFalse(RideTrackingLifecyclePolicy.canPauseTracking(RideState()))
        assertFalse(
            RideTrackingLifecyclePolicy.canPauseTracking(
                RideState(isTracking = true, isPaused = true),
            ),
        )
    }

    @Test
    fun canResumeTracking_onlyForAnActivePausedRide() {
        assertTrue(
            RideTrackingLifecyclePolicy.canResumeTracking(
                RideState(isTracking = true, isPaused = true),
            ),
        )
        assertFalse(RideTrackingLifecyclePolicy.canResumeTracking(RideState()))
        assertFalse(RideTrackingLifecyclePolicy.canResumeTracking(RideState(isTracking = true)))
    }
}
