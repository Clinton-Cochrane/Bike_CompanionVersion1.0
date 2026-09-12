package com.clintoncochrane.bikecompanion

import org.junit.Assert.assertEquals
import org.junit.Test

class ApplicationIdentityTest {

    @Test
    fun applicationClass_usesProductionPackageIdentity() {
        val applicationClass = Class.forName(
            "com.clintoncochrane.bikecompanion.BikeCompanionApplication"
        )

        assertEquals(
            "com.clintoncochrane.bikecompanion",
            applicationClass.packageName
        )
    }
}
