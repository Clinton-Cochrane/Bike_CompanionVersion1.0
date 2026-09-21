package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GarageTopBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dueServiceIndicator_isVisibleAndOpensServiceList() {
        var serviceListOpened = false
        composeRule.setContent {
            MaterialTheme {
                GarageTopBar(
                    hasDueServiceItems = true,
                    onStartRide = {},
                    onServiceListClick = { serviceListOpened = true },
                    onSettingsClick = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Service due. Open service list").performClick()

        composeRule.runOnIdle { assertTrue(serviceListOpened) }
    }

    @Test
    fun dueServiceIndicator_isAbsentWithoutDueServiceItems() {
        composeRule.setContent {
            MaterialTheme {
                GarageTopBar(
                    hasDueServiceItems = false,
                    onStartRide = {},
                    onServiceListClick = {},
                    onSettingsClick = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Service due. Open service list").assertDoesNotExist()
    }
}
