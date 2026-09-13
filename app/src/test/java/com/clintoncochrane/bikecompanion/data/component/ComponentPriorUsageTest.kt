package com.clintoncochrane.bikecompanion.data.component

import org.junit.Assert.assertEquals
import org.junit.Test

class ComponentPriorUsageTest {

    @Test
    fun lifetimeDistanceKm_knownPriorUsage_includesBaselineAndTrackedDistance() {
        val component = component(
            priorUsageCertainty = PriorUsageCertainty.KNOWN,
            baselineKm = 125.0,
            distanceUsedKm = 25.0,
        )

        assertEquals(150.0, component.lifetimeDistanceKm, 0.0)
    }

    @Test
    fun lifetimeDistanceKm_approximatePriorUsage_includesBaselineAndTrackedDistance() {
        val component = component(
            priorUsageCertainty = PriorUsageCertainty.APPROXIMATE,
            baselineKm = 125.0,
            distanceUsedKm = 25.0,
        )

        assertEquals(150.0, component.lifetimeDistanceKm, 0.0)
    }

    @Test
    fun lifetimeDistanceKm_unknownPriorUsage_reportsOnlyTrackedDistance() {
        val component = component(
            priorUsageCertainty = PriorUsageCertainty.UNKNOWN,
            baselineKm = 0.0,
            distanceUsedKm = 25.0,
        )

        assertEquals(25.0, component.lifetimeDistanceKm, 0.0)
    }

    private fun component(
        priorUsageCertainty: PriorUsageCertainty,
        baselineKm: Double,
        distanceUsedKm: Double,
    ) = ComponentEntity(
        bikeId = 1L,
        type = "chain",
        name = "Chain",
        lifespanKm = 3_000.0,
        baselineKm = baselineKm,
        distanceUsedKm = distanceUsedKm,
        priorUsageCertainty = priorUsageCertainty,
        installedAt = 0L,
    )
}
