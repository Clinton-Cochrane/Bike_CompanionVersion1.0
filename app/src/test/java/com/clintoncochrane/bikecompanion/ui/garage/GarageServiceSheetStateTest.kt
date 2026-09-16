package com.clintoncochrane.bikecompanion.ui.garage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GarageServiceSheetStateTest {

    private val clean = DueServiceRequirement(1L, 10L, "Clean", "inspection", "Chain")
    private val replace = DueServiceRequirement(2L, 10L, "Replace", "replace", "Chain")

    @Test
    fun open_startsUncheckedInChecklist() {
        val state = GarageServiceSheetState.open(listOf(clean, replace))

        assertTrue(state.isVisible)
        assertEquals(GarageServiceSheetStep.CHECKLIST, state.step)
        assertTrue(state.selectedIntervalIds.isEmpty())
        assertFalse(state.canCompleteSelected)
    }

    @Test
    fun confirmationAndBack_preserveIntervalSelectionsInSameSheet() {
        val checklist = GarageServiceSheetState.open(listOf(clean, replace))
            .toggle(clean.intervalId)
            .showConfirmation()

        assertTrue(checklist.isVisible)
        assertEquals(GarageServiceSheetStep.CONFIRMATION, checklist.step)

        val restored = checklist.showChecklist()
        assertTrue(restored.isVisible)
        assertEquals(GarageServiceSheetStep.CHECKLIST, restored.step)
        assertEquals(setOf(clean.intervalId), restored.selectedIntervalIds)
    }

    @Test
    fun partialResult_retriesFailuresOnly() {
        val result = GarageServiceSheetState.open(listOf(clean, replace))
            .toggle(clean.intervalId)
            .toggle(replace.intervalId)
            .showResult(successfulIntervalIds = setOf(clean.intervalId), failedIntervalIds = setOf(replace.intervalId))

        assertEquals(GarageServiceSheetStep.RESULT, result.step)
        assertEquals(1, result.completedCount)
        assertEquals(setOf(replace.intervalId), result.selectedIntervalIds)
        assertEquals(listOf(replace), result.failedRequirements)
    }
}
