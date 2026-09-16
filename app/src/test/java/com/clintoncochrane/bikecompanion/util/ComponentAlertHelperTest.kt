package com.clintoncochrane.bikecompanion.util

import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentAlertHelperTest {

    @Test
    fun isComponentAlertActionable_healthAboveThreshold_returnsFalse() {
        assertFalse(isComponentAlertActionable(component(distanceUsedKm = 890.0), nowMillis = 1_000L))
    }

    @Test
    fun isComponentAlertActionable_healthAtOrBelowThreshold_returnsTrue() {
        assertTrue(isComponentAlertActionable(component(distanceUsedKm = 900.0), nowMillis = 1_000L))
        assertTrue(isComponentAlertActionable(component(distanceUsedKm = 950.0), nowMillis = 1_000L))
    }

    @Test
    fun isComponentAlertActionable_alertsDisabledOrSnoozed_returnsFalse() {
        assertFalse(isComponentAlertActionable(component(alertsEnabled = false), nowMillis = 1_000L))
        assertFalse(isComponentAlertActionable(component(alertSnoozeUntilKm = 1_000.0), nowMillis = 1_000L))
        assertFalse(isComponentAlertActionable(component(alertSnoozeUntilTime = 2_000L), nowMillis = 1_000L))
    }

    @Test
    fun isComponentAlertActionable_localAlertsValueUpdatesAvailabilityImmediately() {
        val component = component()

        assertFalse(
            isComponentAlertActionable(component, nowMillis = 1_000L, alertsEnabled = false),
        )
        assertTrue(
            isComponentAlertActionable(component, nowMillis = 1_000L, alertsEnabled = true),
        )
    }

    @Test
    fun isComponentAlertActionable_expiredSnoozes_returnsTrue() {
        assertTrue(isComponentAlertActionable(component(alertSnoozeUntilKm = 900.0), nowMillis = 1_000L))
        assertTrue(isComponentAlertActionable(component(alertSnoozeUntilTime = 1_000L), nowMillis = 1_000L))
    }

    @Test
    fun isComponentAlertActionable_unknownPriorUsage_returnsFalse() {
        assertFalse(
            isComponentAlertActionable(
                component(priorUsageCertainty = PriorUsageCertainty.UNKNOWN),
                nowMillis = 1_000L,
            ),
        )
    }

    private fun component(
        distanceUsedKm: Double = 950.0,
        alertsEnabled: Boolean = true,
        alertSnoozeUntilKm: Double? = null,
        alertSnoozeUntilTime: Long? = null,
        priorUsageCertainty: PriorUsageCertainty = PriorUsageCertainty.KNOWN,
    ) = ComponentEntity(
        bikeId = 1L,
        type = "chain",
        lifespanKm = 1_000.0,
        distanceUsedKm = distanceUsedKm,
        priorUsageCertainty = priorUsageCertainty,
        alertThresholdPercent = 10,
        alertsEnabled = alertsEnabled,
        alertSnoozeUntilKm = alertSnoozeUntilKm,
        alertSnoozeUntilTime = alertSnoozeUntilTime,
        installedAt = 0L,
    )
}
