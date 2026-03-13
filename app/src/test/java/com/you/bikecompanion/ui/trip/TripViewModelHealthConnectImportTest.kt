package com.you.bikecompanion.ui.trip

import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.component.ComponentRepository
import com.you.bikecompanion.data.preferences.AppPreferencesRepository
import com.you.bikecompanion.data.ride.RideRepository
import com.you.bikecompanion.healthconnect.HealthConnectImporter
import com.you.bikecompanion.healthconnect.HealthConnectSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TripViewModel] Health Connect import functionality.
 */
class TripViewModelHealthConnectImportTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var bikeRepository: BikeRepository
    private lateinit var rideRepository: RideRepository
    private lateinit var componentRepository: ComponentRepository
    private lateinit var healthConnectImporter: HealthConnectImporter
    private lateinit var appPreferencesRepository: AppPreferencesRepository
    private lateinit var viewModel: TripViewModel

    private val testBike = BikeEntity(id = 1L, name = "Test Bike", createdAt = 1000L)

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        bikeRepository = mockk()
        rideRepository = mockk()
        componentRepository = mockk()
        healthConnectImporter = mockk()
        appPreferencesRepository = mockk()

        coEvery { bikeRepository.getAllBikes() } returns flowOf(listOf(testBike))
        coEvery { bikeRepository.getMostRecentlyRiddenBike() } returns testBike
        coEvery { rideRepository.getAllRides() } returns flowOf(emptyList())
        coEvery { appPreferencesRepository.dismissedRideFlagIds } returns flowOf(emptySet())
        coEvery { appPreferencesRepository.dismissedPlaceholderReminderIds } returns flowOf(emptySet())
        coEvery { appPreferencesRepository.snoozedPlaceholderReminderUntilMs } returns flowOf(null)
    }

    @Test
    fun importFromHealthConnect_noBikeSelected_emitsNoBikeSelected() = runTest(testDispatcher) {
        coEvery { bikeRepository.getAllBikes() } returns flowOf(emptyList())
        coEvery { bikeRepository.getMostRecentlyRiddenBike() } returns null

        viewModel = TripViewModel(
            bikeRepository,
            rideRepository,
            componentRepository,
            healthConnectImporter,
            appPreferencesRepository,
        )
        advanceUntilIdle()

        var result: HealthConnectImportResult? = null
        val collectJob = launch {
            result = viewModel.healthConnectImportResult.first()
        }
        advanceUntilIdle()

        viewModel.importFromHealthConnect()
        advanceUntilIdle()

        collectJob.join()
        assertEquals(HealthConnectImportResult.NoBikeSelected, result)
    }

    @Test
    fun importFromHealthConnect_emptySessions_emitsNone() = runTest(testDispatcher) {
        coEvery { healthConnectImporter.readCyclingSessions() } returns emptyList()

        viewModel = TripViewModel(
            bikeRepository,
            rideRepository,
            componentRepository,
            healthConnectImporter,
            appPreferencesRepository,
        )
        advanceUntilIdle()
        viewModel.selectBike(testBike)

        var result: HealthConnectImportResult? = null
        val collectJob = launch {
            result = viewModel.healthConnectImportResult.first()
        }
        advanceUntilIdle()

        viewModel.importFromHealthConnect()
        advanceUntilIdle()

        collectJob.join()
        assertEquals(HealthConnectImportResult.None, result)
    }

    @Test
    fun importFromHealthConnect_sessionsImported_emitsSuccessAndSavesRides() = runTest(testDispatcher) {
        val sessions = listOf(
            HealthConnectSession(
                startTimeMs = 1000L,
                endTimeMs = 4600000L,
                durationMs = 3600000L,
                distanceKm = 15.5,
            ),
            HealthConnectSession(
                startTimeMs = 5000000L,
                endTimeMs = 8600000L,
                durationMs = 3600000L,
                distanceKm = 20.0,
            ),
        )
        coEvery { healthConnectImporter.readCyclingSessions() } returns sessions
        coEvery { appPreferencesRepository.getHasSeenHealthConnectImportDisclaimer() } returns true
        coEvery { rideRepository.saveRideAndUpdateBikeAndComponents(any()) } coAnswers { }

        viewModel = TripViewModel(
            bikeRepository,
            rideRepository,
            componentRepository,
            healthConnectImporter,
            appPreferencesRepository,
        )
        advanceUntilIdle()
        viewModel.selectBike(testBike)

        var result: HealthConnectImportResult? = null
        val collectJob = launch {
            result = viewModel.healthConnectImportResult.first()
        }
        advanceUntilIdle()

        viewModel.importFromHealthConnect()
        advanceUntilIdle()

        collectJob.join()
        val success = result as HealthConnectImportResult.Success
        assertEquals(2, success.count)
        assertEquals(false, success.showDisclaimer)

        coVerify(exactly = 2) { rideRepository.saveRideAndUpdateBikeAndComponents(any()) }
    }

    @Test
    fun importFromHealthConnect_firstImport_showsDisclaimerAndSetsFlag() = runTest(testDispatcher) {
        val sessions = listOf(
            HealthConnectSession(
                startTimeMs = 1000L,
                endTimeMs = 4600000L,
                durationMs = 3600000L,
                distanceKm = 10.0,
            ),
        )
        coEvery { healthConnectImporter.readCyclingSessions() } returns sessions
        coEvery { appPreferencesRepository.getHasSeenHealthConnectImportDisclaimer() } returns false
        coEvery { appPreferencesRepository.setHasSeenHealthConnectImportDisclaimer() } coAnswers { }
        coEvery { rideRepository.saveRideAndUpdateBikeAndComponents(any()) } coAnswers { }

        viewModel = TripViewModel(
            bikeRepository,
            rideRepository,
            componentRepository,
            healthConnectImporter,
            appPreferencesRepository,
        )
        advanceUntilIdle()
        viewModel.selectBike(testBike)

        var result: HealthConnectImportResult? = null
        val collectJob = launch {
            result = viewModel.healthConnectImportResult.first()
        }
        advanceUntilIdle()

        viewModel.importFromHealthConnect()
        advanceUntilIdle()

        collectJob.join()
        val success = result as HealthConnectImportResult.Success
        assertEquals(true, success.showDisclaimer)
        coVerify { appPreferencesRepository.setHasSeenHealthConnectImportDisclaimer() }
    }

    @Test
    fun importFromHealthConnect_importerThrows_emitsError() = runTest(testDispatcher) {
        coEvery { healthConnectImporter.readCyclingSessions() } throws RuntimeException("Health Connect unavailable")

        viewModel = TripViewModel(
            bikeRepository,
            rideRepository,
            componentRepository,
            healthConnectImporter,
            appPreferencesRepository,
        )
        advanceUntilIdle()
        viewModel.selectBike(testBike)

        var result: HealthConnectImportResult? = null
        val collectJob = launch {
            result = viewModel.healthConnectImportResult.first()
        }
        advanceUntilIdle()

        viewModel.importFromHealthConnect()
        advanceUntilIdle()

        collectJob.join()
        assertEquals(HealthConnectImportResult.Error, result)
    }
}
