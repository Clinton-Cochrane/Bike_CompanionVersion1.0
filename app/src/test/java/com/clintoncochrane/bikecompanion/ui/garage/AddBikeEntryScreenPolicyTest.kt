package com.clintoncochrane.bikecompanion.ui.garage

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddBikeEntryScreenPolicyTest {

    @Test
    fun addBikeEntryScreen_offersFullSetupWithoutQuickAddNavigation() {
        val screenSource = File("src/main/java/com/clintoncochrane/bikecompanion/ui/garage/AddBikeEntryScreen.kt")
            .readText()

        assertTrue(screenSource.contains("Screen.AddBikeAdvanced.route"))
        assertFalse(screenSource.contains("Screen.AddBikeSimple.route"))
    }
}
