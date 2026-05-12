package com.you.bikecompanion.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ImperialUnitsTest {

    @Test
    fun kmToMiles_roundTrip() {
        val km = 100.0
        val miles = ImperialUnits.kmToMiles(km)
        assertEquals(km, ImperialUnits.milesToKm(miles), 0.0001)
    }

    @Test
    fun kmhToMph_knownValue() {
        // 100 km/h ≈ 62.14 mph
        assertEquals(62.137, ImperialUnits.kmhToMph(100.0), 0.01)
    }

    @Test
    fun metersToFeet_knownValue() {
        assertEquals(328.084, ImperialUnits.metersToFeet(100.0), 0.01)
    }

    @Test
    fun milesFromKmToInputString_exactMile() {
        assertEquals("5", ImperialUnits.milesFromKmToInputString(ImperialUnits.milesToKm(5.0)))
    }
}
