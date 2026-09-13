package com.clintoncochrane.bikecompanion.location

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RideLocationManifestTest {

    @Test
    fun manifest_requestsFineLocationWithoutBackgroundLocation() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("android.permission.ACCESS_COARSE_LOCATION"))
        assertTrue(manifest.contains("android.permission.ACCESS_FINE_LOCATION"))
        assertFalse(manifest.contains("android.permission.ACCESS_BACKGROUND_LOCATION"))
    }
}
