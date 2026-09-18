package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GarageServiceSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun serviceDueCard_actionOpensChecklistWithNothingSelected() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                GarageBikeStatusCard(
                    status = GarageBikeStatusSummary(
                        GarageBikeStatus.ServiceDue,
                        listOf("Chain"),
                    ),
                    isActionable = true,
                    onClick = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Service due. Affected: Chain").performClick()

        composeRule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun checklistAndConfirmation_renderInSameSheetAndLiftBottomNavigation() {
        val requirement = DueServiceRequirement(1L, 10L, "Inspect", "inspection", "Chain")
        var state by mutableStateOf(GarageServiceSheetState.open(listOf(requirement)))
        var liftPx = 0f
        composeRule.setContent {
            MaterialTheme {
                GarageServiceSheetHost(
                    state = state,
                    onDismiss = { state = state.copy(isVisible = false) },
                    onToggle = { state = state.toggle(it) },
                    onShowConfirmation = { state = state.showConfirmation() },
                    onShowChecklist = { state = state.showChecklist() },
                    onConfirm = {},
                    onRetry = {},
                    onLiftChanged = { liftPx = it },
                )
            }
        }

        composeRule.onNodeWithTag(GARAGE_SERVICE_SHEET_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Complete selected").assertIsNotEnabled()
        val completeButtonBottom = composeRule.onNodeWithText("Complete selected")
            .getUnclippedBoundsInRoot()
            .bottom
        val firstRequirementTop = composeRule.onNodeWithText("Inspect — Chain")
            .getUnclippedBoundsInRoot()
            .top
        assertTrue(completeButtonBottom <= firstRequirementTop)
        composeRule.onNodeWithText("Inspect — Chain").performClick()
        composeRule.onNodeWithText("Complete selected").performClick()

        composeRule.onNodeWithTag(GARAGE_SERVICE_SHEET_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Complete these services?").assertIsDisplayed()
        val confirmationDescriptionBottom = composeRule.onNodeWithText("Restart this service interval.")
            .getUnclippedBoundsInRoot()
            .bottom
        val confirmationActionsTop = composeRule.onNodeWithTag("garage_service_confirmation_actions")
            .getUnclippedBoundsInRoot()
            .top
        assertTrue(confirmationActionsTop - confirmationDescriptionBottom >= 20.dp)
        composeRule.runOnIdle { assertTrue(liftPx > 0f) }
    }

    @Test
    fun sheetContent_startsBelowElevatedBottomNavigationForEveryStep() {
        val requirement = DueServiceRequirement(1L, 10L, "Inspect", "inspection", "Chain")
        val navigationBarClearance = 80.dp
        var state by mutableStateOf(GarageServiceSheetState.open(listOf(requirement)))
        composeRule.setContent {
            MaterialTheme {
                GarageServiceSheetHost(
                    state = state,
                    onDismiss = {},
                    onToggle = {},
                    onShowConfirmation = {},
                    onShowChecklist = {},
                    onConfirm = {},
                    onRetry = {},
                    onLiftChanged = {},
                    navigationBarClearance = navigationBarClearance,
                )
            }
        }

        assertContentClearsNavigation("Service due", navigationBarClearance)

        composeRule.runOnIdle {
            state = state.copy(
                step = GarageServiceSheetStep.CONFIRMATION,
                selectedIntervalIds = setOf(requirement.intervalId),
            )
        }
        assertContentClearsNavigation("Complete these services?", navigationBarClearance)

        composeRule.runOnIdle {
            state = state.showResult(
                successfulIntervalIds = emptySet(),
                failedIntervalIds = setOf(requirement.intervalId),
            )
        }
        assertContentClearsNavigation("0 services completed", navigationBarClearance)
    }

    @Test
    fun completeSelectedButton_isFullyVisibleWhenSheetFirstOpens() {
        val requirement = DueServiceRequirement(1L, 10L, "Inspect", "inspection", "Chain")
        val navigationBarClearance = 80.dp
        composeRule.setContent {
            MaterialTheme {
                GarageServiceSheetHost(
                    state = GarageServiceSheetState.open(listOf(requirement)),
                    onDismiss = {},
                    onToggle = {},
                    onShowConfirmation = {},
                    onShowChecklist = {},
                    onConfirm = {},
                    onRetry = {},
                    onLiftChanged = {},
                    navigationBarClearance = navigationBarClearance,
                )
            }
        }

        val sheetBounds = composeRule.onNodeWithTag(GARAGE_SERVICE_SHEET_TAG)
            .getUnclippedBoundsInRoot()
        val buttonBounds = composeRule.onNodeWithText("Complete selected")
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()

        assertTrue(buttonBounds.top - sheetBounds.top >= navigationBarClearance)
        assertTrue(buttonBounds.bottom <= sheetBounds.bottom)
    }

    @Test
    fun swipeDown_dismissesSheet() {
        val requirement = DueServiceRequirement(1L, 10L, "Inspect", "inspection", "Chain")
        var state by mutableStateOf(GarageServiceSheetState.open(listOf(requirement)))
        composeRule.setContent {
            MaterialTheme {
                GarageServiceSheetHost(
                    state = state,
                    onDismiss = { state = state.copy(isVisible = false) },
                    onToggle = {},
                    onShowConfirmation = {},
                    onShowChecklist = {},
                    onConfirm = {},
                    onRetry = {},
                    onLiftChanged = {},
                )
            }
        }

        composeRule.onNodeWithTag(GARAGE_SERVICE_SHEET_TAG).performTouchInput { swipeDown() }

        composeRule.runOnIdle { assertFalse(state.isVisible) }
    }

    private fun assertContentClearsNavigation(text: String, navigationBarClearance: Dp) {
        val sheetTop = composeRule.onNodeWithTag(GARAGE_SERVICE_SHEET_TAG)
            .getUnclippedBoundsInRoot()
            .top
        val contentTop = composeRule.onNodeWithText(text)
            .getUnclippedBoundsInRoot()
            .top

        assertTrue(contentTop - sheetTop >= navigationBarClearance)
    }
}
