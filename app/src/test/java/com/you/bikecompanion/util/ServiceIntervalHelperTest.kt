package com.you.bikecompanion.util

import com.you.bikecompanion.data.component.ServiceIntervalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServiceIntervalHelperTest {

    @Test
    fun description_distanceInterval_formatsMilesRemaining() {
        val interval = ServiceIntervalEntity(
            id = 1L,
            componentId = 1L,
            name = "Inspect",
            intervalKm = 1609.344,
            trackedKm = 804.672,
        )
        val desc = ServiceIntervalHelper.description(interval)
        assertEquals("500 mi of 1000 mi left", desc.kmText)
        assertNull(desc.timeText)
    }
}
