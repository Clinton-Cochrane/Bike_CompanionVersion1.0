package com.clintoncochrane.bikecompanion.ui.garage

import androidx.lifecycle.SavedStateHandle
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.bike.BikeMileageCorrectionRepository
import com.clintoncochrane.bikecompanion.data.bike.BikeRepository
import com.clintoncochrane.bikecompanion.data.bike.MileageCorrectionResult
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BikeDetailMileageCorrectionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val bikeRepository = mockk<BikeRepository>()
    private val correctionRepository = mockk<BikeMileageCorrectionRepository>()
    private val componentRepository = mockk<ComponentRepository>()
    private val serviceIntervalRepository = mockk<ServiceIntervalRepository>()
    private val preferencesRepository = mockk<AppPreferencesRepository>()
    private val rideRepository = mockk<RideRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { bikeRepository.getAllBikes() } returns flowOf(emptyList())
        every { componentRepository.getComponentsByBikeId(BIKE_ID) } returns flowOf(emptyList())
        every { rideRepository.getRidesByBikeId(BIKE_ID) } returns flowOf(emptyList())
        every { preferencesRepository.closeToServiceHealthThreshold } returns flowOf(20)
        every { preferencesRepository.dismissedRideFlagIds } returns flowOf(emptySet())
        every { serviceIntervalRepository.getIntervalsByComponentIds(emptyList()) } returns flowOf(emptyList())
    }

    @Test
    fun correctMileage_successRefreshesBikeAndPublishesOutcome() = runTest(dispatcher) {
        val original = bike(totalDistanceKm = 500.0)
        val corrected = bike(baselineDistanceKm = 500.0, totalDistanceKm = 1_000.0)
        coEvery { bikeRepository.getBikeById(BIKE_ID) } returnsMany listOf(original, corrected)
        coEvery {
            correctionRepository.correctMileage(BIKE_ID, 1_000.0, setOf(10L))
        } returns MileageCorrectionResult.APPLIED
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.correctMileage(1_000.0, setOf(10L))
        advanceUntilIdle()

        coVerify(exactly = 1) {
            correctionRepository.correctMileage(BIKE_ID, 1_000.0, setOf(10L))
        }
        assertEquals(corrected, viewModel.uiState.value.bike)
        assertEquals(MileageCorrectionResult.APPLIED, viewModel.uiState.value.mileageCorrectionResult)
        assertFalse(viewModel.uiState.value.mileageCorrectionInProgress)
    }

    @Test
    fun correctMileage_invalidSelectionPublishesOutcomeWithoutRefreshingBike() = runTest(dispatcher) {
        val original = bike(totalDistanceKm = 500.0)
        coEvery { bikeRepository.getBikeById(BIKE_ID) } returns original
        coEvery {
            correctionRepository.correctMileage(BIKE_ID, 100.0, setOf(10L))
        } returns MileageCorrectionResult.COMPONENT_WOULD_BECOME_NEGATIVE
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.correctMileage(100.0, setOf(10L))
        advanceUntilIdle()

        coVerify(exactly = 1) { bikeRepository.getBikeById(BIKE_ID) }
        assertEquals(
            MileageCorrectionResult.COMPONENT_WOULD_BECOME_NEGATIVE,
            viewModel.uiState.value.mileageCorrectionResult,
        )
        assertEquals(original, viewModel.uiState.value.bike)
    }

    private fun viewModel() = BikeDetailViewModel(
        SavedStateHandle(mapOf("bikeId" to BIKE_ID.toString())),
        bikeRepository,
        rideRepository,
        componentRepository,
        serviceIntervalRepository,
        preferencesRepository,
        correctionRepository,
    )

    private fun bike(
        baselineDistanceKm: Double = 0.0,
        totalDistanceKm: Double,
    ) = BikeEntity(
        id = BIKE_ID,
        name = "Test bike",
        baselineDistanceKm = baselineDistanceKm,
        totalDistanceKm = totalDistanceKm,
        createdAt = 1L,
    )

    private companion object {
        const val BIKE_ID = 42L
    }
}
