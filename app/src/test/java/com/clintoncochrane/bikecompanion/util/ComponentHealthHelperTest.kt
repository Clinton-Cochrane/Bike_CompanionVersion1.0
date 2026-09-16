package com.clintoncochrane.bikecompanion.util

import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComponentHealthHelperTest {

    @Test
    fun componentHealthPercent_unknownPriorUsage_returnsNoPercentage() {
        val component = component(
            certainty = PriorUsageCertainty.UNKNOWN,
            baselineKm = 0.0,
            trackedKm = 500.0,
        )

        assertNull(componentHealthPercent(component))
    }

    @Test
    fun componentHealthPercent_knownPriorUsage_preservesPercentage() {
        val component = component(
            certainty = PriorUsageCertainty.KNOWN,
            baselineKm = 250.0,
            trackedKm = 250.0,
        )

        assertEquals(50, componentHealthPercent(component))
    }

    @Test
    fun componentHealthPercent_approximatePriorUsage_preservesPercentage() {
        val component = component(
            certainty = PriorUsageCertainty.APPROXIMATE,
            baselineKm = 250.0,
            trackedKm = 250.0,
        )

        assertEquals(50, componentHealthPercent(component))
    }

    @Test
    fun minimumComponentHealthPercent_anyUnknownPriorUsage_returnsNoPercentage() {
        val known = component(PriorUsageCertainty.KNOWN, baselineKm = 250.0, trackedKm = 250.0)
        val unknown = component(PriorUsageCertainty.UNKNOWN, baselineKm = 0.0, trackedKm = 100.0)

        assertNull(minimumComponentHealthPercent(listOf(known, unknown)))
    }

    private fun component(
        certainty: PriorUsageCertainty,
        baselineKm: Double,
        trackedKm: Double,
    ) = ComponentEntity(
        bikeId = 1L,
        type = "chain",
        name = "Chain",
        lifespanKm = 1_000.0,
        distanceUsedKm = trackedKm,
        baselineKm = baselineKm,
        priorUsageCertainty = certainty,
        installedAt = 0L,
    )
}
