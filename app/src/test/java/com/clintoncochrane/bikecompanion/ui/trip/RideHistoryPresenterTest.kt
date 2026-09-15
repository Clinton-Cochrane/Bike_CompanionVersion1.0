package com.clintoncochrane.bikecompanion.ui.trip

import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.ride.RideEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RideHistoryPresenterTest {

    private val rory = BikeEntity(id = 1L, name = "Rory", createdAt = 1L)
    private val willow = BikeEntity(id = 2L, name = "Willow", createdAt = 2L)
    private val oldest = ride(id = 1L, bikeId = 1L, distanceKm = 8.0, durationMs = 20_000, endedAt = 100L)
    private val longestDistance = ride(id = 2L, bikeId = 2L, distanceKm = 24.0, durationMs = 10_000, endedAt = 200L)
    private val longestDuration = ride(id = 3L, bikeId = 1L, distanceKm = 12.0, durationMs = 30_000, endedAt = 300L)
    private val unassigned = ride(id = 4L, bikeId = null, distanceKm = 4.0, durationMs = 5_000, endedAt = 400L)

    @Test
    fun defaultState_showsAllBikesSortedNewestFirst() {
        val state = RideHistoryUiState()

        assertNull(state.bikeFilterId)
        assertEquals(RideHistorySort.NEWEST, state.sort)
        assertEquals(
            listOf(4L, 3L, 2L, 1L),
            RideHistoryPresenter.visibleRides(listOf(oldest, longestDistance, longestDuration, unassigned), state)
                .map { it.id },
        )
    }

    @Test
    fun bikeFilter_filtersAssignedRides_andAllBikesRestoresHistory() {
        val rides = listOf(oldest, longestDistance, longestDuration, unassigned)

        val roryIds = RideHistoryPresenter.visibleRides(
            rides,
            RideHistoryUiState(bikeFilterId = rory.id),
        ).map { it.id }
        val allBikeIds = RideHistoryPresenter.visibleRides(rides, RideHistoryUiState()).map { it.id }

        assertEquals(listOf(3L, 1L), roryIds)
        assertEquals(listOf(4L, 3L, 2L, 1L), allBikeIds)
    }

    @Test
    fun sortDistance_ordersVisibleRidesLongestFirst() {
        val ids = RideHistoryPresenter.visibleRides(
            listOf(oldest, longestDistance, longestDuration, unassigned),
            RideHistoryUiState(sort = RideHistorySort.DISTANCE),
        ).map { it.id }

        assertEquals(listOf(2L, 3L, 1L, 4L), ids)
    }

    @Test
    fun sortDuration_ordersVisibleRidesLongestFirst() {
        val ids = RideHistoryPresenter.visibleRides(
            listOf(oldest, longestDistance, longestDuration, unassigned),
            RideHistoryUiState(sort = RideHistorySort.DURATION),
        ).map { it.id }

        assertEquals(listOf(3L, 1L, 2L, 4L), ids)
    }

    @Test
    fun filterAndSort_applyTogetherWithoutChangingEitherSelection() {
        val state = RideHistoryUiState(bikeFilterId = rory.id, sort = RideHistorySort.DURATION)

        val ids = RideHistoryPresenter.visibleRides(
            listOf(oldest, longestDistance, longestDuration, unassigned),
            state,
        ).map { it.id }

        assertEquals(listOf(3L, 1L), ids)
        assertEquals(rory.id, state.bikeFilterId)
        assertEquals(RideHistorySort.DURATION, state.sort)
    }

    @Test
    fun toRows_mapsAssignedAndUnassignedBikesExplicitly() {
        val rows = RideHistoryPresenter.toRows(
            rides = listOf(longestDuration, unassigned),
            bikes = listOf(rory, willow),
        )

        assertEquals("Rory", rows[0].bikeName)
        assertTrue(rows[0].hasAssignedBike)
        assertNull(rows[1].bikeName)
        assertFalse(rows[1].hasAssignedBike)
    }

    private fun ride(
        id: Long,
        bikeId: Long?,
        distanceKm: Double,
        durationMs: Long,
        endedAt: Long,
    ) = RideEntity(
        id = id,
        bikeId = bikeId,
        distanceKm = distanceKm,
        durationMs = durationMs,
        startedAt = endedAt - durationMs,
        endedAt = endedAt,
    )
}
