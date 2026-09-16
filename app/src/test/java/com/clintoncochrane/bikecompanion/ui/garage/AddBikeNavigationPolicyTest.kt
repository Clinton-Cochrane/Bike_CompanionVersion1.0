package com.clintoncochrane.bikecompanion.ui.garage

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddBikeNavigationPolicyTest {

    @Test
    fun addBikeRoute_opensFullBikeFormDirectly() {
        val navigationSource = File("src/main/java/com/clintoncochrane/bikecompanion/ui/navigation/BikeCompanionNavGraph.kt")
            .readText()

        assertTrue(
            navigationSource.contains(
                "composable(Screen.AddBike.route) { backStackEntry ->\n" +
                    "            AddEditBikeScreen(navController = navController, backStackEntry = backStackEntry, bikeId = null)",
            ),
        )
        assertFalse(navigationSource.contains("AddBikeEntryScreen"))
    }
}
