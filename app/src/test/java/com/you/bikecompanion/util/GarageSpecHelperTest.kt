package com.you.bikecompanion.util

import com.you.bikecompanion.data.bike.BikeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GarageSpecHelperTest {

    @Test
    fun sortBikesByLastRidden_mostRecentFirst() {
        val bikes = listOf(
            BikeEntity(id = 1, name = "A", lastRideAt = 1000L, createdAt = 0L),
            BikeEntity(id = 2, name = "B", lastRideAt = 3000L, createdAt = 0L),
            BikeEntity(id = 3, name = "C", lastRideAt = 2000L, createdAt = 0L),
        )
        val sorted = GarageSpecHelper.sortBikesByLastRidden(bikes)
        assertEquals(listOf(2L, 3L, 1L), sorted.map { it.id })
    }

    @Test
    fun sortBikesByLastRidden_nullsLast() {
        val bikes = listOf(
            BikeEntity(id = 1, name = "A", lastRideAt = null, createdAt = 0L),
            BikeEntity(id = 2, name = "B", lastRideAt = 1000L, createdAt = 0L),
        )
        val sorted = GarageSpecHelper.sortBikesByLastRidden(bikes)
        assertEquals(listOf(2L, 1L), sorted.map { it.id })
    }

    @Test
    fun sortBikesByLastRidden_tiesSortedByName() {
        val bikes = listOf(
            BikeEntity(id = 1, name = "Zebra", lastRideAt = 1000L, createdAt = 0L),
            BikeEntity(id = 2, name = "Alpha", lastRideAt = 1000L, createdAt = 0L),
        )
        val sorted = GarageSpecHelper.sortBikesByLastRidden(bikes)
        assertEquals(listOf(2L, 1L), sorted.map { it.id })
    }

    @Test
    fun computeTotalDistanceKm_sumsAllBikes() {
        val bikes = listOf(
            BikeEntity(id = 1, name = "A", totalDistanceKm = 100.0, createdAt = 0L),
            BikeEntity(id = 2, name = "B", totalDistanceKm = 50.5, createdAt = 0L),
        )
        assertEquals(150.5, GarageSpecHelper.computeTotalDistanceKm(bikes), 0.001)
    }

    @Test
    fun computeTotalDistanceKm_emptyList_returnsZero() {
        assertEquals(0.0, GarageSpecHelper.computeTotalDistanceKm(emptyList()), 0.001)
    }

    @Test
    fun getLastRiddenBikeId_returnsMostRecent() {
        val bikes = listOf(
            BikeEntity(id = 1, name = "A", lastRideAt = 1000L, createdAt = 0L),
            BikeEntity(id = 2, name = "B", lastRideAt = 3000L, createdAt = 0L),
        )
        assertEquals(2L, GarageSpecHelper.getLastRiddenBikeId(bikes))
    }

    @Test
    fun getLastRiddenBikeId_allNull_returnsNull() {
        val bikes = listOf(
            BikeEntity(id = 1, name = "A", lastRideAt = null, createdAt = 0L),
        )
        assertNull(GarageSpecHelper.getLastRiddenBikeId(bikes))
    }

    @Test
    fun getLastRiddenBikeId_allZero_returnsNull() {
        val bikes = listOf(
            BikeEntity(id = 1, name = "A", lastRideAt = 0L, createdAt = 0L),
        )
        assertNull(GarageSpecHelper.getLastRiddenBikeId(bikes))
    }

    @Test
    fun getLastRiddenBikeId_emptyList_returnsNull() {
        assertNull(GarageSpecHelper.getLastRiddenBikeId(emptyList()))
    }
}
