package com.clintoncochrane.bikecompanion.ui.garage

import androidx.lifecycle.SavedStateHandle
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.bike.BikeRepository
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import com.clintoncochrane.bikecompanion.data.component.ComponentSwapRepository
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalRepository
import com.clintoncochrane.bikecompanion.data.preferences.AppPreferencesRepository
import com.clintoncochrane.bikecompanion.data.ride.RideRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddComponentViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val bikeRepository = mockk<BikeRepository>()
    private val componentRepository = mockk<ComponentRepository>()
    private val componentSwapRepository = mockk<ComponentSwapRepository>()
    private val serviceIntervalRepository = mockk<ServiceIntervalRepository>()
    private val appPreferencesRepository = mockk<AppPreferencesRepository>()
    private val rideRepository = mockk<RideRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @Test
    fun addComponent_fromBikeDetail_insertsInstalledComponentExactlyOnce() = runTest(dispatcher) {
        stubBikeDetailFlows()
        coEvery { componentRepository.insertComponent(any()) } returns 1L
        val viewModel = BikeDetailViewModel(
            SavedStateHandle(mapOf("bikeId" to "42")),
            bikeRepository,
            rideRepository,
            componentRepository,
            serviceIntervalRepository,
            appPreferencesRepository,
        )
        advanceUntilIdle()

        viewModel.addComponent(request())
        advanceUntilIdle()

        coVerify(exactly = 1) {
            componentRepository.insertComponent(match { component ->
                component.bikeId == 42L &&
                    component.type == "chain" &&
                    component.name == "Race chain" &&
                    component.make == "Shimano" &&
                    component.model == "CN-HG54" &&
                    component.baselineKm == 1200.0 &&
                    component.priorUsageCertainty == PriorUsageCertainty.APPROXIMATE
            })
        }
    }

    @Test
    fun addComponent_fromGarage_insertsUnassignedComponentExactlyOnce() = runTest(dispatcher) {
        stubGarageFlows()
        coEvery { componentRepository.insertComponent(any()) } returns 1L
        val viewModel = GarageViewModel(
            bikeRepository,
            componentRepository,
            componentSwapRepository,
            serviceIntervalRepository,
            appPreferencesRepository,
            rideRepository,
        )
        advanceUntilIdle()

        viewModel.addComponentToGarage(request())
        advanceUntilIdle()

        coVerify(exactly = 1) {
            componentRepository.insertComponent(match { component ->
                component.bikeId == null &&
                    component.type == "chain" &&
                    component.name == "Race chain" &&
                    component.make == "Shimano" &&
                    component.model == "CN-HG54" &&
                    component.baselineKm == 1200.0 &&
                    component.priorUsageCertainty == PriorUsageCertainty.APPROXIMATE
            })
        }
    }

    private fun stubBikeDetailFlows() {
        coEvery { bikeRepository.getBikeById(42L) } returns BikeEntity(
            id = 42L,
            name = "Bike",
            createdAt = 1L,
        )
        every { bikeRepository.getAllBikes() } returns flowOf(emptyList())
        every { componentRepository.getComponentsByBikeId(42L) } returns flowOf(emptyList())
        every { rideRepository.getRidesByBikeId(42L) } returns flowOf(emptyList())
        every { appPreferencesRepository.closeToServiceHealthThreshold } returns flowOf(20)
        every { appPreferencesRepository.dismissedRideFlagIds } returns flowOf(emptySet())
        every { serviceIntervalRepository.getIntervalsByComponentIds(emptyList()) } returns flowOf(emptyList())
    }

    private fun stubGarageFlows() {
        every { bikeRepository.getAllBikes() } returns flowOf(emptyList())
        every { componentRepository.getAllComponentsFlow() } returns flowOf(emptyList())
        every { componentRepository.getNonRetiredComponents() } returns flowOf(emptyList())
        every { componentSwapRepository.getAllSwaps() } returns flowOf(emptyList())
        every { appPreferencesRepository.closeToServiceHealthThreshold } returns flowOf(20)
        every { rideRepository.getAllRides() } returns flowOf(emptyList())
    }

    private fun request() = AddComponentRequest(
        type = "chain",
        displayName = "Race chain",
        make = "Shimano",
        model = "CN-HG54",
        lifespanKm = 3500.0,
        baselineKm = 1200.0,
        priorUsageCertainty = PriorUsageCertainty.APPROXIMATE,
    )
}
