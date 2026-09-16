package com.clintoncochrane.bikecompanion.ui.ride

import com.clintoncochrane.bikecompanion.location.RideState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RideCompletionPolicyTest {

    @Test
    fun notificationStop_assignedRideUsesExistingSaveFlow() {
        assertEquals(
            RideCompletionDestination.SAVE,
            RideCompletionPolicy.destinationFor(RideState(bikeId = 7L, isTracking = true)),
        )
    }

    @Test
    fun notificationStop_unassignedRideRequiresBikeAssignment() {
        assertEquals(
            RideCompletionDestination.ASSIGN_BIKE,
            RideCompletionPolicy.destinationFor(RideState(bikeId = -1L, isTracking = true)),
        )
    }

    @Test
    fun repeatedNotificationStop_isIgnoredUntilCompletionCanBeRetried() {
        val guard = RideCompletionRequestGuard()

        assertTrue(guard.tryStart())
        assertFalse(guard.tryStart())

        guard.reset()

        assertTrue(guard.tryStart())
    }
}
