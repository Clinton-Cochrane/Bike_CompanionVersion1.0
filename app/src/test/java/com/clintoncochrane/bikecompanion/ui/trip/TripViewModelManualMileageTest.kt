package com.clintoncochrane.bikecompanion.ui.trip

import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.bike.BikeRepository
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import com.clintoncochrane.bikecompanion.data.preferences.AppPreferencesRepository
import com.clintoncochrane.bikecompanion.data.ride.RideRepository
import com.clintoncochrane.bikecompanion.healthconnect.HealthConnectImporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripViewModelManualMileageTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var bikeRepository: BikeRepository
    private lateinit var rideRepository: RideRepository
    private lateinit var componentRepository: ComponentRepository
    private lateinit var healthConnectImporter: HealthConnectImporter
    private lateinit var appPreferencesRepository: AppPreferencesRepository
    private lateinit var viewModel: TripViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        bikeRepository = mockk()
        rideRepository = mockk()
        componentRepository = mockk()
        healthConnectImporter = mockk()
        appPreferencesRepository = mockk()

        coEvery { bikeRepository.getAllBikes() } returns flowOf(
            listOf(BikeEntity(id = 7L, name = "Test Bike", createdAt = 1L)),
        )
        coEvery { bikeRepository.getMostRecentlyRiddenBike() } returns null
        coEvery { rideRepository.getAllRides() } returns flowOf(emptyList())
        coEvery { appPreferencesRepository.dismissedRideFlagIds } returns flowOf(emptySet())
        coEvery { appPreferencesRepository.dismissedPlaceholderReminderIds } returns flowOf(emptySet())
        coEvery { appPreferencesRepository.snoozedPlaceholderReminderUntilMs } returns flowOf(null)
        coEvery { rideRepository.saveManualRide(any(), any(), any()) } returns Unit

        viewModel = TripViewModel(
            bikeRepository,
            rideRepository,
            componentRepository,
            healthConnectImporter,
            appPreferencesRepository,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveManualMileage_validBikeAndDistance_usesManualRepositoryPath() = runTest(testDispatcher) {
        advanceUntilIdle()

        var result: ManualMileageSaveResult? = null
        val collectJob = launch { result = viewModel.manualMileageSaveResult.first() }
        advanceUntilIdle()
        viewModel.saveManualMileage(bikeId = 7L, distanceKm = 12.5)
        advanceUntilIdle()
        collectJob.join()

        coVerify(exactly = 1) { rideRepository.saveManualRide(7L, 12.5, any()) }
        assertEquals(ManualMileageSaveResult.Success, result)
        assertEquals(false, viewModel.uiState.value.isSavingManualMileage)
    }

    @Test
    fun saveManualMileage_invalidInput_doesNotCallRepository() = runTest(testDispatcher) {
        val invalidInputs = listOf(
            null to 1.0,
            7L to null,
            7L to 0.0,
            7L to -1.0,
            7L to Double.NaN,
            7L to Double.POSITIVE_INFINITY,
            7L to Double.NEGATIVE_INFINITY,
        )

        invalidInputs.forEach { (bikeId, distanceKm) ->
            var result: ManualMileageSaveResult? = null
            val collectJob = launch { result = viewModel.manualMileageSaveResult.first() }
            advanceUntilIdle()
            viewModel.saveManualMileage(bikeId, distanceKm)
            advanceUntilIdle()
            collectJob.join()
            assertEquals(ManualMileageSaveResult.InvalidInput, result)
        }

        coVerify(exactly = 0) { rideRepository.saveManualRide(any(), any(), any()) }
    }
}
