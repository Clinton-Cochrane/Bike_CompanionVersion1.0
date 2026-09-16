package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BikeStatsGridTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bikeStatsGrid_placesMetricsInTwoColumnsAndTwoRows() {
        composeRule.setContent {
            MaterialTheme {
                BikeStatsGrid(
                    odometerValue = "5000 km",
                    movingTimeValue = "0s",
                    averageSpeedValue = "0 km/h",
                    maxSpeedValue = "0 km/h",
                )
            }
        }

        val odometer = composeRule.onNodeWithText("ODOMETER").bounds()
        val movingTime = composeRule.onNodeWithText("MOVING TIME").bounds()
        val averageSpeed = composeRule.onNodeWithText("AVG. SPEED").bounds()
        val maxSpeed = composeRule.onNodeWithText("MAX SPEED").bounds()

        assertEquals(odometer.top, movingTime.top, POSITION_TOLERANCE_PX)
        assertEquals(averageSpeed.top, maxSpeed.top, POSITION_TOLERANCE_PX)
        assertTrue(averageSpeed.top > odometer.bottom)
        assertEquals(odometer.left, averageSpeed.left, POSITION_TOLERANCE_PX)
        assertEquals(movingTime.left, maxSpeed.left, POSITION_TOLERANCE_PX)
        assertTrue(movingTime.left > odometer.right)
    }

    private fun SemanticsNodeInteraction.bounds(): Rect = fetchSemanticsNode().boundsInRoot

    private companion object {
        const val POSITION_TOLERANCE_PX = 1f
    }
}
