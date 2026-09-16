package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AddComponentDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cancel_closesDialogWithoutAdding() {
        var addCount = 0
        composeRule.setContent {
            var visible by remember { mutableStateOf(true) }
            if (visible) {
                AddComponentDialog(
                    onDismiss = { visible = false },
                    onAdd = { addCount++ },
                )
            }
        }

        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Add component").assertDoesNotExist()
        assertEquals(0, addCount)
    }

    @Test
    fun add_usesTypeDropdownAndClosesSingleModalAfterOneSubmission() {
        var addCount = 0
        var submittedType = ""
        composeRule.setContent {
            var visible by remember { mutableStateOf(true) }
            if (visible) {
                AddComponentDialog(
                    onDismiss = { visible = false },
                    onAdd = { request ->
                        addCount++
                        submittedType = request.type
                        visible = false
                    },
                )
            }
        }

        composeRule.onNodeWithText("Type").assertIsDisplayed()
        composeRule.onNodeWithText("Chain").performClick()
        composeRule.onNodeWithText("Cassette").performClick()
        composeRule.onNodeWithText("Add").performClick()

        composeRule.onNodeWithText("Add component").assertDoesNotExist()
        composeRule.onNodeWithText("Prior usage for Cassette").assertDoesNotExist()
        assertEquals(1, addCount)
        assertEquals("cassette", submittedType)
    }
}
