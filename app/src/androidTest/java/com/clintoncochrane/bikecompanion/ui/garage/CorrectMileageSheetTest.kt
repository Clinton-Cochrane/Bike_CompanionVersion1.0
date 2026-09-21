package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CorrectMileageSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bikeActionsMenu_containsAddComponentAndCorrectMileage() {
        var addClicked = false
        var correctionClicked = false
        composeRule.setContent {
            MaterialTheme {
                BikeActionsMenu(
                    onAddComponent = { addClicked = true },
                    onCorrectMileage = { correctionClicked = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Bike actions").performClick()
        composeRule.onNodeWithText("Add component").performClick()
        assertTrue(addClicked)

        composeRule.onNodeWithContentDescription("Bike actions").performClick()
        composeRule.onNodeWithText("Correct mileage").performClick()
        assertTrue(correctionClicked)
    }

    @Test
    fun correctMileageSheet_componentsStartUncheckedAndPreviewSameDelta() {
        var appliedMileage = 0.0
        var appliedComponentIds = emptySet<Long>()
        composeRule.setContent {
            MaterialTheme {
                CorrectMileageSheet(
                    bike = bike(totalDistanceKm = 500.0),
                    components = listOf(component(id = 10L, mileageKm = 300.0)),
                    saving = false,
                    onDismiss = {},
                    onApply = { mileage, ids ->
                        appliedMileage = mileage
                        appliedComponentIds = ids
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Apply correction to Chain").assertIsOff()
        composeRule.onNodeWithText("New mileage (km)").performTextReplacement("1000")
        composeRule.onNodeWithText("Difference: +500 km").assertExists()
        composeRule.onNodeWithText("300 → 800 km", substring = true).assertExists()
        composeRule.onNodeWithContentDescription("Apply correction to Chain").performClick().assertIsOn()
        composeRule.onNodeWithText("Apply").performClick()

        composeRule.runOnIdle {
            assertEquals(1_000.0, appliedMileage, 0.0)
            assertEquals(setOf(10L), appliedComponentIds)
        }
    }

    @Test
    fun correctMileageSheet_selectedNegativeComponentBlocksApply() {
        composeRule.setContent {
            MaterialTheme {
                CorrectMileageSheet(
                    bike = bike(totalDistanceKm = 1_000.0),
                    components = listOf(component(id = 10L, mileageKm = 50.0)),
                    saving = false,
                    onDismiss = {},
                    onApply = { _, _ -> error("Apply must remain blocked") },
                )
            }
        }

        composeRule.onNodeWithText("New mileage (km)").performTextReplacement("900")
        composeRule.onNodeWithContentDescription("Apply correction to Chain").performClick()

        composeRule.onNodeWithText("A selected component would be below 0 km.").assertExists()
        composeRule.onNodeWithText("Apply").assertIsNotEnabled()
    }

    private fun bike(totalDistanceKm: Double) = BikeEntity(
        id = 42L,
        name = "Test bike",
        totalDistanceKm = totalDistanceKm,
        createdAt = 1L,
    )

    private fun component(id: Long, mileageKm: Double) = ComponentEntity(
        id = id,
        bikeId = 42L,
        type = "chain",
        name = "Chain",
        lifespanKm = 2_000.0,
        baselineKm = mileageKm,
        installedAt = 1L,
    )
}
