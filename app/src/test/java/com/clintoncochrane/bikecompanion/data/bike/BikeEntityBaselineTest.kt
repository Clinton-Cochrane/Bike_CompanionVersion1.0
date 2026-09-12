package com.clintoncochrane.bikecompanion.data.bike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BikeEntityBaselineTest {

    @Test
    fun withBaselineDistance_newBike_setsBaselineAndOdometerTotal() {
        val bike = BikeEntity(name = "Used bike", createdAt = 0L)

        val updated = bike.withBaselineDistanceKm(1_250.5)

        assertEquals(1_250.5, updated.baselineDistanceKm, 0.0)
        assertEquals(1_250.5, updated.totalDistanceKm, 0.0)
        assertEquals(0.0, updated.recordedDistanceKm, 0.0)
    }

    @Test
    fun withBaselineDistance_existingBike_preservesRecordedDistanceWhenCorrected() {
        val bike = BikeEntity(
            name = "Used bike",
            baselineDistanceKm = 1_000.0,
            totalDistanceKm = 1_125.0,
            createdAt = 0L,
        )

        val increased = bike.withBaselineDistanceKm(1_200.0)
        val decreased = bike.withBaselineDistanceKm(800.0)

        assertEquals(1_325.0, increased.totalDistanceKm, 0.0)
        assertEquals(925.0, decreased.totalDistanceKm, 0.0)
        assertEquals(125.0, increased.recordedDistanceKm, 0.0)
        assertEquals(125.0, decreased.recordedDistanceKm, 0.0)
    }

    @Test
    fun withBaselineDistance_invalidValue_isRejected() {
        listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { value ->
            assertThrows(IllegalArgumentException::class.java) {
                BikeEntity(name = "Bike", createdAt = 0L).withBaselineDistanceKm(value)
            }
        }
    }
}
