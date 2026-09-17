package com.clintoncochrane.bikecompanion.ui.stats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.ui.theme.BikeCompanionTheme
import org.junit.Rule
import org.junit.Test

class StatsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun statsContent_showsAllBikesMetricsAndAnEmptyBikeState() {
        composeRule.setContent {
            BikeCompanionTheme {
                StatsContent(
                    uiState = StatsUiState(allBikesStats = StatsSummary()),
                    onPreviousBike = {},
                    onNextBike = {},
                    onBikeSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("All bikes").assertIsDisplayed()
        composeRule.onNodeWithText("Distance: 0.0 km").assertIsDisplayed()
        composeRule.onNodeWithText("Ride time: 0:00:00").assertIsDisplayed()
        composeRule.onNodeWithText("Rides: 0").assertIsDisplayed()
        composeRule.onNodeWithText("Services: 0").assertIsDisplayed()
        composeRule.onNodeWithText("Add a bike to see per-bike lifetime totals.").assertIsDisplayed()
    }

    @Test
    fun statsContent_keepsArrowAndSwipeNavigationSynchronized() {
        composeRule.setContent {
            var selectedBikeIndex by remember { mutableStateOf(0) }
            val bikes = listOf(
                bike(id = 1L, name = "Bike 1"),
                bike(id = 2L, name = "Bike 2"),
                bike(id = 3L, name = "Bike 3"),
            ).map { BikeWithStats(it, StatsSummary()) }
            BikeCompanionTheme {
                StatsContent(
                    uiState = StatsUiState(
                        bikesWithStats = bikes,
                        selectedBikeIndex = selectedBikeIndex,
                    ),
                    onPreviousBike = { selectedBikeIndex = (selectedBikeIndex - 1).coerceAtLeast(0) },
                    onNextBike = { selectedBikeIndex = (selectedBikeIndex + 1).coerceAtMost(bikes.lastIndex) },
                    onBikeSelected = { selectedBikeIndex = it },
                )
            }
        }

        composeRule.onNodeWithText("Bike 1 of 3").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Show next bike").performClick()
        composeRule.onNodeWithText("Bike 2 of 3").assertIsDisplayed()
        composeRule.onNodeWithText("Bike 2").assertIsDisplayed()

        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitUntil(timeoutMillis = 15_000) {
            val bike3Visible = try {
                composeRule.onNodeWithText("Bike 3 of 3").isDisplayed()
            } catch (e: AssertionError) {
                false
            }
            if (!bike3Visible) {
                composeRule.waitForIdle()
                composeRule.onRoot().performTouchInput { swipeLeft() }
            }
            bike3Visible
        }
        composeRule.onNodeWithText("Bike 3 of 3").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Show previous bike").performClick()
        composeRule.onNodeWithText("Bike 2 of 3").assertIsDisplayed()
    }

    private fun bike(id: Long, name: String) = BikeEntity(
        id = id,
        name = name,
        createdAt = 0L,
    )
}
