package com.clintoncochrane.bikecompanion.ui.garage

import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.bike.BikeRepository
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentLifecycleStatus
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import com.clintoncochrane.bikecompanion.data.component.ComponentSwapRepository
import com.clintoncochrane.bikecompanion.data.component.SERVICE_INTERVAL_TYPE_INSPECTION
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalEntity
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalRepository
import com.clintoncochrane.bikecompanion.data.preferences.AppPreferencesRepository
import com.clintoncochrane.bikecompanion.data.ride.RideRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GarageViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val bikeRepository = mockk<BikeRepository>()
    private val componentRepository = mockk<ComponentRepository>()
    private val componentSwapRepository = mockk<ComponentSwapRepository>()
    private val serviceIntervalRepository = mockk<ServiceIntervalRepository>()
    private val appPreferencesRepository = mockk<AppPreferencesRepository>()
    private val rideRepository = mockk<RideRepository>()
    private val serviceCompletionCoordinator = mockk<ServiceCompletionCoordinator>()
    private val components = MutableStateFlow(listOf(installedComponent()))
    private val intervals = MutableStateFlow(listOf(dueInterval()))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { bikeRepository.getAllBikes() } returns flowOf(
            listOf(BikeEntity(id = 1L, name = "Bike", createdAt = 0L)),
        )
        every { componentRepository.getAllComponentsFlow() } returns components
        every { componentRepository.getNonRetiredComponents() } returns components
        every { componentSwapRepository.getAllSwaps() } returns flowOf(emptyList())
        every { serviceIntervalRepository.getAllIntervals() } returns intervals
        every { appPreferencesRepository.closeToServiceHealthThreshold } returns flowOf(20)
        every { rideRepository.getAllRides() } returns flowOf(emptyList())
    }

    @Test
    fun dueServiceIndicator_updatesForCompletionAndGarageOnlyComponents() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasDueServiceItems)

        intervals.value = listOf(dueInterval().copy(trackedKm = 0.0))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasDueServiceItems)

        intervals.value = listOf(dueInterval())
        components.value = listOf(
            installedComponent().copy(
                bikeId = null,
                lifecycleStatus = ComponentLifecycleStatus.IN_GARAGE,
            ),
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasDueServiceItems)
    }

    private fun viewModel() = GarageViewModel(
        bikeRepository,
        componentRepository,
        componentSwapRepository,
        serviceIntervalRepository,
        appPreferencesRepository,
        rideRepository,
        serviceCompletionCoordinator,
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
