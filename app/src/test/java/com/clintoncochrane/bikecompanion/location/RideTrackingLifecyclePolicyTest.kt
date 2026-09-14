package com.clintoncochrane.bikecompanion.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RideTrackingLifecyclePolicyTest {

    @Test
    fun assignBike_whenRideStartsWithBike_isRejected() {
        val state = RideState(bikeId = 1L, isTracking = true)

        assertFalse(RideAssignmentPolicy.canAssignBike(state, 2L))
        assertNull(RideAssignmentPolicy.assignBike(state, 2L))
    }

    @Test
    fun assignBike_whenRideStartsWithoutBike_assignsAndLocksBike() {
        val state = RideState(bikeId = -1L, isTracking = true)

        val assigned = RideAssignmentPolicy.assignBike(state, 2L)

        assertEquals(2L, assigned?.bikeId)
        assertFalse(RideAssignmentPolicy.canAssignBike(requireNotNull(assigned), 3L))
        assertNull(RideAssignmentPolicy.assignBike(requireNotNull(assigned), 3L))
    }

    @Test
    fun assignBike_whenRideIsNotActive_isRejected() {
        assertFalse(RideAssignmentPolicy.canAssignBike(RideState(), 1L))
    }

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
