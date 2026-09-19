package com.clintoncochrane.bikecompanion.ui.garage

import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.bike.BikeRepository
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentLifecycleStatus
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import com.clintoncochrane.bikecompanion.data.component.SERVICE_INTERVAL_TYPE_INSPECTION
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalEntity
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalRepository
import com.clintoncochrane.bikecompanion.data.preferences.AppPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ServiceListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val bikeRepository = mockk<BikeRepository>()
    private val componentRepository = mockk<ComponentRepository>()
    private val serviceIntervalRepository = mockk<ServiceIntervalRepository>()
    private val appPreferencesRepository = mockk<AppPreferencesRepository>()
    private val bikes = MutableStateFlow(listOf(BikeEntity(id = 1L, name = "Bike A", createdAt = 0L)))
    private val components = MutableStateFlow(listOf(installedComponent()))
    private val intervals = MutableStateFlow(listOf(dueInterval()))
    private val threshold = MutableStateFlow(20)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { bikeRepository.getAllBikes() } returns bikes
        every { componentRepository.getNonRetiredComponents() } returns components
        every { serviceIntervalRepository.getAllIntervals() } returns intervals
        every { appPreferencesRepository.closeToServiceHealthThreshold } returns threshold
    }

    @Test
    fun componentAssignmentAndRetirementChanges_updateServiceListMembership() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(listOf(1L), viewModel.uiState.value.dueItems.map { it.component.id })

        components.value = listOf(
            installedComponent().copy(
                bikeId = null,
                lifecycleStatus = ComponentLifecycleStatus.IN_GARAGE,
            ),
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.dueItems.isEmpty())

        components.value = listOf(installedComponent())
        advanceUntilIdle()
        assertEquals(listOf(1L), viewModel.uiState.value.dueItems.map { it.component.id })

        components.value = emptyList()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.dueItems.isEmpty())
    }

    @Test
    fun intervalCompletion_updateRemovesComponentFromServiceList() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(listOf(1L), viewModel.uiState.value.dueItems.map { it.component.id })

        intervals.value = listOf(dueInterval().copy(trackedKm = 0.0))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.dueItems.isEmpty())
    }

    private fun viewModel() = ServiceListViewModel(
        bikeRepository = bikeRepository,
        componentRepository = componentRepository,
        serviceIntervalRepository = serviceIntervalRepository,
        appPreferencesRepository = appPreferencesRepository,
    )

    private fun installedComponent() = ComponentEntity(
        id = 1L,
        bikeId = 1L,
        lifecycleStatus = ComponentLifecycleStatus.INSTALLED,
        type = "chain",
        name = "Chain",
        lifespanKm = 1_000.0,
        installedAt = 0L,
    )

    private fun dueInterval() = ServiceIntervalEntity(
        id = 10L,
        componentId = 1L,
        name = "Inspect",
        intervalKm = 100.0,
        trackedKm = 90.0,
        type = SERVICE_INTERVAL_TYPE_INSPECTION,
    )
}
