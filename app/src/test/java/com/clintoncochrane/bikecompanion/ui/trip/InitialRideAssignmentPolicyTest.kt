package com.clintoncochrane.bikecompanion.ui.trip

import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class InitialRideAssignmentPolicyTest {

    @Test
    fun initialBikeId_whenThereAreNoRideableBikes_returnsNoBike() {
        assertEquals(NO_BIKE_ASSIGNED, InitialRideAssignmentPolicy.initialBikeId(emptyList()))
    }

    @Test
    fun initialBikeId_whenThereIsOneRideableBike_returnsThatBike() {
        val bike = bike(id = 42L, name = "Commuter")

        assertEquals(bike.id, InitialRideAssignmentPolicy.initialBikeId(listOf(bike)))
    }

    @Test
    fun initialBikeId_whenThereAreMultipleRideableBikes_returnsNoBike() {
        assertEquals(
            NO_BIKE_ASSIGNED,
            InitialRideAssignmentPolicy.initialBikeId(
                listOf(
                    bike(id = 1L, name = "Road"),
                    bike(id = 2L, name = "Mountain"),
                ),
            ),
        )
    }

    private fun bike(id: Long, name: String) = BikeEntity(
        id = id,
        name = name,
        createdAt = 1L,
    )
}
