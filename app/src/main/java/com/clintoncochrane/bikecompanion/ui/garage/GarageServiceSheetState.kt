package com.clintoncochrane.bikecompanion.ui.garage

enum class GarageServiceSheetStep {
    CHECKLIST,
    CONFIRMATION,
    RESULT,
}

data class GarageServiceSheetState(
    val isVisible: Boolean = false,
    val step: GarageServiceSheetStep = GarageServiceSheetStep.CHECKLIST,
    val requirements: List<DueServiceRequirement> = emptyList(),
    val selectedIntervalIds: Set<Long> = emptySet(),
    val failedIntervalIds: Set<Long> = emptySet(),
    val completedCount: Int = 0,
    val isSubmitting: Boolean = false,
    val sessionId: String? = null,
) {
    val selectedRequirements: List<DueServiceRequirement>
        get() = requirements.filter { it.intervalId in selectedIntervalIds }

    val failedRequirements: List<DueServiceRequirement>
        get() = requirements.filter { it.intervalId in failedIntervalIds }

    val canCompleteSelected: Boolean
        get() = selectedIntervalIds.isNotEmpty() && !isSubmitting

    fun toggle(intervalId: Long): GarageServiceSheetState {
        if (step != GarageServiceSheetStep.CHECKLIST || isSubmitting) return this
        val nextSelection = if (intervalId in selectedIntervalIds) {
            selectedIntervalIds - intervalId
        } else {
            selectedIntervalIds + intervalId
        }
        return copy(selectedIntervalIds = nextSelection)
    }

    fun showConfirmation(): GarageServiceSheetState = if (canCompleteSelected) {
        copy(step = GarageServiceSheetStep.CONFIRMATION)
    } else {
        this
    }

    fun showChecklist(): GarageServiceSheetState = copy(step = GarageServiceSheetStep.CHECKLIST)

    fun showResult(
        successfulIntervalIds: Set<Long>,
        failedIntervalIds: Set<Long>,
    ): GarageServiceSheetState = copy(
        step = GarageServiceSheetStep.RESULT,
        selectedIntervalIds = failedIntervalIds,
        failedIntervalIds = failedIntervalIds,
        completedCount = completedCount + successfulIntervalIds.size,
        isSubmitting = false,
    )

    companion object {
        fun open(requirements: List<DueServiceRequirement>): GarageServiceSheetState =
            GarageServiceSheetState(isVisible = true, requirements = requirements)
    }
}
