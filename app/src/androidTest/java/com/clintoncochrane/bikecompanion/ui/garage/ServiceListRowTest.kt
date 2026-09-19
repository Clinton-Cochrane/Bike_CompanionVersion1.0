package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.util.ServiceIntervalHelper
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ServiceListRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun longComponentName_narrowWidth_keepsIdentityAndActionsReadable() {
        val longName = "Front Hydraulic Brake Caliper Assembly"
        composeRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.width(320.dp)) {
                    ServiceListRow(
                        item = DueServiceItem(
                            component = ComponentEntity(
                                id = 1L,
                                bikeId = 1L,
                                type = "brake_caliper",
                                name = longName,
                                lifespanKm = 1_000.0,
                                installedAt = 0L,
                            ),
                            bikeName = "Bike A",
                            healthPercent = 10,
                            nextDueDescription = ServiceIntervalHelper.IntervalDescription(
                                remainingKm = 10,
                                remainingTimeSeconds = null,
                                expectedIntervalReached = false,
                            ),
                            nextServiceIntervalId = 10L,
                        ),
                        isSelected = false,
                        onToggleSelect = {},
                        onReplace = {},
                        onInspect = {},
                        onRowClick = {},
                    )
                }
            }
        }

        val identityBounds = composeRule.onNodeWithTag(
            testTag = SERVICE_LIST_IDENTITY_TAG,
            useUnmergedTree = true,
        )
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val identityWidth = identityBounds.right - identityBounds.left
        assertTrue("Identity content was squeezed to $identityWidth", identityWidth >= 120.dp)
        composeRule.onNodeWithTag(SERVICE_LIST_CHECKBOX_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(SERVICE_LIST_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(longName).assertIsDisplayed()
        composeRule.onNodeWithText("Brake Caliper").assertIsDisplayed()
        composeRule.onNodeWithText("Bike A").assertIsDisplayed()
        composeRule.onNodeWithText("10 km until expected interval").assertIsDisplayed()
        composeRule.onNodeWithText("Health: 10%").assertIsDisplayed()
        composeRule.onNodeWithText("Replace").assertIsDisplayed()
        composeRule.onNodeWithText("Inspect").assertIsDisplayed()
    }
}
