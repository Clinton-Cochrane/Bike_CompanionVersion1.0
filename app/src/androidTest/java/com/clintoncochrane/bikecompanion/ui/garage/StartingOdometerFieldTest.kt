package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StartingOdometerFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun newBike_typingMileageReplacesPlaceholder() {
        var input by mutableStateOf(INITIAL_STARTING_ODOMETER_INPUT)
        composeRule.setContent {
            MaterialTheme {
                StartingOdometerField(
                    value = input,
                    onValueChange = { input = it },
                )
            }
        }

        val field = composeRule.onNodeWithTag(STARTING_ODOMETER_FIELD_TAG)
        composeRule.runOnIdle { assertEquals("", input) }
        field.performClick()
        field.performTextInput("1000")
        composeRule.runOnIdle { assertEquals("1000", input) }
    }
}
