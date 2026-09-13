package com.clintoncochrane.bikecompanion.util

import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceIntervalHelperTest {

    @Test
    fun description_beforeInterval_returnsRemainingDistanceAndTime() {
        val interval = serviceInterval(
            intervalKm = 250.0,
            trackedKm = 70.0,
            intervalTimeSeconds = 1_209_600L,
            trackedTimeSeconds = 604_800L,
        )

        val description = ServiceIntervalHelper.description(interval)

        assertFalse(description.expectedIntervalReached)
        assertEquals(180, description.remainingKm)
        assertEquals(604_800L, description.remainingTimeSeconds)
    }

    @Test
    fun description_atDistanceInterval_marksExpectedIntervalReached() {
        val interval = serviceInterval(intervalKm = 250.0, trackedKm = 250.0)

        val description = ServiceIntervalHelper.description(interval)

        assertTrue(description.expectedIntervalReached)
        assertEquals(0, description.remainingKm)
    }

    @Test
    fun description_pastTimeInterval_marksExpectedIntervalReached() {
        val interval = serviceInterval(
            intervalKm = 0.0,
            intervalTimeSeconds = 604_800L,
            trackedTimeSeconds = 604_801L,
        )

        val description = ServiceIntervalHelper.description(interval)

        assertTrue(description.expectedIntervalReached)
        assertEquals(0L, description.remainingTimeSeconds)
    }

    private fun serviceInterval(
        intervalKm: Double,
        trackedKm: Double = 0.0,
        intervalTimeSeconds: Long? = null,
        trackedTimeSeconds: Long? = null,
    ) = ServiceIntervalEntity(
        componentId = 1L,
        name = "Inspection",
        intervalKm = intervalKm,
        trackedKm = trackedKm,
        intervalTimeSeconds = intervalTimeSeconds,
        trackedTimeSeconds = trackedTimeSeconds,
    )
}
