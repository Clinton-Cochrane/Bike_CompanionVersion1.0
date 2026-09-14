package com.clintoncochrane.bikecompanion.ui.garage

import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import com.clintoncochrane.bikecompanion.data.component.ComponentLifecycleStatus
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WallOfHonorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val componentRepository = mockk<ComponentRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun retiredComponents_areExposedForTheWallOfHonor() = runTest(testDispatcher) {
        val retired = component(
            id = 1L,
            name = "Retired chain",
            lifecycleStatus = ComponentLifecycleStatus.RETIRED,
        )
        every { componentRepository.getRetiredComponents() } returns flowOf(listOf(retired))

        val viewModel = WallOfHonorViewModel(componentRepository)
        advanceUntilIdle()

        assertEquals(listOf(retired), viewModel.uiState.value.components)
    }

    private fun component(
        id: Long,
        name: String,
        lifecycleStatus: ComponentLifecycleStatus,
    ) = ComponentEntity(
        id = id,
        bikeId = null,
        lifecycleStatus = lifecycleStatus,
        type = "chain",
        name = name,
        lifespanKm = 3_000.0,
        priorUsageCertainty = PriorUsageCertainty.KNOWN,
        installedAt = 0L,
    )
}
