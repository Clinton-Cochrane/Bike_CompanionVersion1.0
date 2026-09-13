package com.clintoncochrane.bikecompanion.notifications

import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentAlertNotifierTest {

    @Test
    fun shouldNotifyForComponent_unknownPriorUsage_doesNotUseTrackedDistanceAsLifetime() {
        val component = dueComponent(PriorUsageCertainty.UNKNOWN)

        assertFalse(shouldNotifyForComponent(component, nowMillis = 1_000L))
    }

    @Test
    fun shouldNotifyForComponent_knownPriorUsage_preservesThresholdAlert() {
        val component = dueComponent(PriorUsageCertainty.KNOWN)

        assertTrue(shouldNotifyForComponent(component, nowMillis = 1_000L))
    }

    private fun dueComponent(certainty: PriorUsageCertainty) = ComponentEntity(
        bikeId = 1L,
        type = "chain",
        name = "Chain",
        lifespanKm = 1_000.0,
        distanceUsedKm = 950.0,
        priorUsageCertainty = certainty,
        alertThresholdPercent = 10,
        installedAt = 0L,
    )
}
