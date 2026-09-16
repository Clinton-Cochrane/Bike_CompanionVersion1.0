package com.clintoncochrane.bikecompanion.util

import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NextServiceInboxTest {

    @Test
    fun nextServiceInbox_excludesComponentsWithoutQualifyingIntervals() {
        val noIntervals = component(id = 1, type = "Brakes", name = "Pads")
        val healthy = component(id = 2, type = "Chain", name = "Chain")

        val inbox = nextServiceInbox(
            components = listOf(noIntervals, healthy),
            intervalsByComponentId = mapOf(healthy.id to listOf(interval(healthy.id, trackedKm = 70.0))),
            thresholdPercent = 20,
        )

        assertTrue(inbox.isEmpty())
    }

    @Test
    fun nextServiceInbox_includesIntervalsAtOrBelowThreshold_sortedByUrgency() {
        val due = component(id = 1, type = "Wheels", name = "Tire")
        val atThreshold = component(id = 2, type = "Brakes", name = "Pads")
        val belowThreshold = component(id = 3, type = "Chain", name = "Chain")

        val inbox = nextServiceInbox(
            components = listOf(atThreshold, belowThreshold, due),
            intervalsByComponentId = mapOf(
                due.id to listOf(interval(due.id, trackedKm = 110.0)),
                atThreshold.id to listOf(interval(atThreshold.id, trackedKm = 80.0)),
                belowThreshold.id to listOf(interval(belowThreshold.id, trackedKm = 85.0)),
            ),
            thresholdPercent = 20,
        )

        assertEquals(listOf(due.id, belowThreshold.id, atThreshold.id), inbox.map(ComponentEntity::id))
    }

    @Test
    fun nextServiceInbox_usesMostUrgentDistanceOrTimeAndInterval() {
        val timeUrgent = component(id = 1, type = "Wheels", name = "Sealant")
        val multipleIntervals = component(id = 2, type = "Drivetrain", name = "Chain")

        val inbox = nextServiceInbox(
            components = listOf(multipleIntervals, timeUrgent),
            intervalsByComponentId = mapOf(
                timeUrgent.id to listOf(
                    interval(
                        timeUrgent.id,
                        trackedKm = 10.0,
                        intervalTimeSeconds = 100L,
                        trackedTimeSeconds = 90L,
                    ),
                ),
                multipleIntervals.id to listOf(
                    interval(multipleIntervals.id, trackedKm = 10.0),
                    interval(multipleIntervals.id, trackedKm = 95.0),
                ),
            ),
            thresholdPercent = 20,
        )

        assertEquals(listOf(multipleIntervals.id, timeUrgent.id), inbox.map(ComponentEntity::id))
    }

    @Test
    fun nextServiceInbox_equalUrgency_usesTypeThenNameForStableOrdering() {
        val brakes = component(id = 2, type = "Brakes", name = "Pads")
        val chain = component(id = 1, type = "Chain", name = "Chain")

        val inbox = nextServiceInbox(
            components = listOf(chain, brakes),
            intervalsByComponentId = mapOf(
                brakes.id to listOf(interval(brakes.id, trackedKm = 90.0)),
                chain.id to listOf(interval(chain.id, trackedKm = 90.0)),
            ),
            thresholdPercent = 20,
        )

        assertEquals(listOf(brakes.id, chain.id), inbox.map(ComponentEntity::id))
    }

    @Test
    fun nextServiceInbox_thresholdControlsMembership_independentOfAlertSettings() {
        val component = component(id = 1, type = "Chain", name = "Chain", alertsEnabled = false)
            .copy(alertSnoozeUntilKm = 1_000.0)
        val intervals = mapOf(component.id to listOf(interval(component.id, trackedKm = 75.0)))

        assertTrue(nextServiceInbox(listOf(component), intervals, thresholdPercent = 25).isNotEmpty())
        assertTrue(nextServiceInbox(listOf(component), intervals, thresholdPercent = 20).isEmpty())
        assertFalse(nextServiceInbox(emptyList(), emptyMap(), thresholdPercent = 20).isNotEmpty())
    }

    @Test
    fun availableComponentSortOrder_emptyInboxWhileNextServiceSelected_returnsType() {
        assertEquals(
            ComponentSortOrder.TYPE_AZ,
            availableComponentSortOrder(ComponentSortOrder.NEXT_SERVICE, hasNextServiceItems = false),
        )
    }

    @Test
    fun componentSortOrder_doesNotContainHealthMode() {
        assertEquals(
            setOf(ComponentSortOrder.TYPE_AZ, ComponentSortOrder.NEXT_SERVICE),
            ComponentSortOrder.entries.toSet(),
        )
    }

    private fun component(id: Long, type: String, name: String, alertsEnabled: Boolean = true) = ComponentEntity(
        id = id,
        bikeId = 1L,
        type = type,
        name = name,
        lifespanKm = 1_000.0,
        alertsEnabled = alertsEnabled,
        installedAt = 0L,
    )

    private fun interval(
        componentId: Long,
        trackedKm: Double,
        intervalTimeSeconds: Long? = null,
        trackedTimeSeconds: Long? = null,
    ) = ServiceIntervalEntity(
        componentId = componentId,
        name = "Inspection",
        intervalKm = 100.0,
        trackedKm = trackedKm,
        intervalTimeSeconds = intervalTimeSeconds,
        trackedTimeSeconds = trackedTimeSeconds,
    )
}
