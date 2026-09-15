package com.clintoncochrane.bikecompanion.ui.garage

import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import com.clintoncochrane.bikecompanion.data.ride.RideEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class GarageBikesPresenterTest {

    @Test
    fun build_preservesSelectedBikeByStableIdWhenOrderChanges() {
        val selectedBikeId = 2L

        val state = GarageBikesPresenter.build(
            bikes = listOf(bike(1), bike(2)),
            components = emptyList(),
            rides = emptyList(),
            closeToServiceThreshold = 20,
            selectedBikeId = selectedBikeId,
        )

        assertEquals(2L, state.selectedBikeId)
        assertEquals(1, state.selectedBikeIndex)
    }

    @Test
    fun build_selectsFirstBikeWhenSelectedBikeIsRemoved() {
        val state = GarageBikesPresenter.build(
            bikes = listOf(bike(3), bike(4)),
            components = emptyList(),
            rides = emptyList(),
            closeToServiceThreshold = 20,
            selectedBikeId = 2L,
        )

        assertEquals(3L, state.selectedBikeId)
        assertEquals(0, state.selectedBikeIndex)
    }

    @Test
    fun build_mapsReadyInspectAndServiceStatusesFromComponentHealth() {
        val bikes = listOf(bike(1), bike(2), bike(3))
        val components = listOf(
            component(id = 11, bikeId = 1, name = "Chain", distanceUsedKm = 50.0),
            component(id = 12, bikeId = 2, name = "Brake pads", distanceUsedKm = 850.0),
            component(id = 13, bikeId = 3, name = "Cassette", distanceUsedKm = 1_000.0),
        )

        val ready = GarageBikesPresenter.build(bikes, components, emptyList(), 20, 1).status
        val inspect = GarageBikesPresenter.build(bikes, components, emptyList(), 20, 2).status
        val due = GarageBikesPresenter.build(bikes, components, emptyList(), 20, 3).status

        assertEquals(GarageBikeStatus.ReadyToRide, ready.level)
        assertEquals(GarageBikeStatus.InspectSoon, inspect.level)
        assertEquals(listOf("Brake pads"), inspect.affectedComponentNames)
        assertEquals(GarageBikeStatus.ServiceDue, due.level)
        assertEquals(listOf("Cassette"), due.affectedComponentNames)
    }

    @Test
    fun build_keepsOnlyFiveNewestRidesForSelectedBike() {
        val rides = (1L..6L).map { id -> ride(id, bikeId = 1, endedAt = id) } + ride(7, bikeId = 2, endedAt = 100)

        val state = GarageBikesPresenter.build(
            bikes = listOf(bike(1), bike(2)),
            components = emptyList(),
            rides = rides,
            closeToServiceThreshold = 20,
            selectedBikeId = 1,
        )

        assertEquals(listOf(6L, 5L, 4L, 3L, 2L), state.recentRides.map { it.id })
    }

    private fun bike(id: Long) = BikeEntity(id = id, name = "Bike $id", createdAt = 0L)

    private fun component(
        id: Long,
        bikeId: Long,
        name: String,
        distanceUsedKm: Double,
    ) = ComponentEntity(
        id = id,
        bikeId = bikeId,
        type = name,
        name = name,
        lifespanKm = 1_000.0,
        distanceUsedKm = distanceUsedKm,
        priorUsageCertainty = PriorUsageCertainty.KNOWN,
        installedAt = 0L,
    )

    private fun ride(id: Long, bikeId: Long, endedAt: Long) = RideEntity(
        id = id,
        bikeId = bikeId,
        distanceKm = 1.0,
        durationMs = 1_000L,
        startedAt = endedAt - 1,
        endedAt = endedAt,
    )
}
