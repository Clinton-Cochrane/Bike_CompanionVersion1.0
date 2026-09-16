package com.clintoncochrane.bikecompanion.ui.garage

import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import com.clintoncochrane.bikecompanion.data.component.SERVICE_INTERVAL_TYPE_INSPECTION
import com.clintoncochrane.bikecompanion.data.component.SERVICE_INTERVAL_TYPE_REPLACE
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceCompletionCoordinatorTest {

    private val intervals = mockk<ServiceIntervalRepository>()
    private val components = mockk<ComponentRepository>()
    private val coordinator = ServiceCompletionCoordinator(intervals, components)

    @Test
    fun complete_normalServiceRunsBeforeReplacementForSameComponent() = runTest {
        val clean = requirement(1, 10, SERVICE_INTERVAL_TYPE_INSPECTION)
        val replace = requirement(2, 10, SERVICE_INTERVAL_TYPE_REPLACE)
        coEvery { intervals.completeServiceRequirement(1, "session", 100) } returns true
        coEvery { components.replaceComponentForService(2, "session", 100) } returns 20L

        val result = coordinator.complete(listOf(replace, clean), "session", 100)

        assertEquals(setOf(1L, 2L), result.successfulIntervalIds)
        assertEquals(emptySet<Long>(), result.failedIntervalIds)
        coVerifyOrder {
            intervals.completeServiceRequirement(1, "session", 100)
            components.replaceComponentForService(2, "session", 100)
        }
    }

    @Test
    fun complete_failedPrerequisiteSkipsReplacementButContinuesOtherComponents() = runTest {
        val failedClean = requirement(1, 10, SERVICE_INTERVAL_TYPE_INSPECTION)
        val skippedReplace = requirement(2, 10, SERVICE_INTERVAL_TYPE_REPLACE)
        val successfulInspect = requirement(3, 11, SERVICE_INTERVAL_TYPE_INSPECTION)
        coEvery { intervals.completeServiceRequirement(1, "session", 100) } throws IllegalStateException("fail")
        coEvery { intervals.completeServiceRequirement(3, "session", 100) } returns true

        val result = coordinator.complete(listOf(failedClean, skippedReplace, successfulInspect), "session", 100)

        assertEquals(setOf(3L), result.successfulIntervalIds)
        assertEquals(setOf(1L, 2L), result.failedIntervalIds)
        coVerify(exactly = 0) { components.replaceComponentForService(2, any(), any()) }
    }

    @Test
    fun retry_receivesOnlyFailedRequirementsAndDoesNotRepeatSuccess() = runTest {
        val clean = requirement(1, 10, SERVICE_INTERVAL_TYPE_INSPECTION)
        val inspect = requirement(3, 11, SERVICE_INTERVAL_TYPE_INSPECTION)
        coEvery { intervals.completeServiceRequirement(1, "session", 100) } returns true
        coEvery { intervals.completeServiceRequirement(3, "session", 100) } throws IllegalStateException("fail")
        coEvery { intervals.completeServiceRequirement(3, "session", 200) } returns true

        val first = coordinator.complete(listOf(clean, inspect), "session", 100)
        val retryItems = listOf(clean, inspect).filter { it.intervalId in first.failedIntervalIds }
        val retry = coordinator.complete(retryItems, "session", 200)

        assertEquals(setOf(3L), retry.successfulIntervalIds)
        coVerify(exactly = 1) { intervals.completeServiceRequirement(1, "session", any()) }
    }

    private fun requirement(id: Long, componentId: Long, type: String) = DueServiceRequirement(
        intervalId = id,
        componentId = componentId,
        serviceName = if (type == SERVICE_INTERVAL_TYPE_REPLACE) "Replace" else "Inspect",
        serviceType = type,
        componentLabel = "Component $componentId",
    )
}
