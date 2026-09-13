package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import org.junit.Rule
import org.junit.Test

class ComponentHealthSummaryTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unknownPriorUsage_showsTrackedDistanceAndExplanation_withoutPercentageSemantics() {
        composeRule.setContent {
            MaterialTheme {
                ComponentHealthSummary(component(PriorUsageCertainty.UNKNOWN))
            }
        }

        composeRule.onNodeWithText("Tracked by Bike Companion: 250 km").assertIsDisplayed()
        composeRule.onNodeWithText("Prior usage unknown; percentage cannot be calculated reliably.").assertIsDisplayed()
        composeRule.onNodeWithText("Health: 75%").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Health: 75%").assertDoesNotExist()
    }

    @Test
    fun knownPriorUsage_keepsExpectedIntervalAndPercentage() {
        composeRule.setContent {
            MaterialTheme {
                ComponentHealthSummary(component(PriorUsageCertainty.KNOWN))
            }
        }

        composeRule.onNodeWithText("500 / 1000 km toward expected interval").assertIsDisplayed()
        composeRule.onNodeWithText("Health: 50%").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Health: 50%").assertIsDisplayed()
    }

    private fun component(certainty: PriorUsageCertainty) = ComponentEntity(
        bikeId = 1L,
        type = "chain",
        name = "Chain",
        lifespanKm = 1_000.0,
        distanceUsedKm = 250.0,
        baselineKm = if (certainty == PriorUsageCertainty.UNKNOWN) 0.0 else 250.0,
        priorUsageCertainty = certainty,
        installedAt = 0L,
    )
}
