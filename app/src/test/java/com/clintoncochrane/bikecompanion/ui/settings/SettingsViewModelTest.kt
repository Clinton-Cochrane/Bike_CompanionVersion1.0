package com.clintoncochrane.bikecompanion.ui.settings

import com.clintoncochrane.bikecompanion.data.preferences.AppPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
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

/**
 * Unit tests for [SettingsViewModel] health threshold delegation.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val appPreferencesRepository = mockk<AppPreferencesRepository>()

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        every { appPreferencesRepository.closeToServiceHealthThreshold } returns flowOf(20)
        every { appPreferencesRepository.hasRequestedMaintenanceNotificationPermission } returns flowOf(false)
    }

    @Test
    fun setCloseToServiceHealthThreshold_clamps_and_updates_state() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        coEvery { appPreferencesRepository.setCloseToServiceHealthThreshold(any()) } returns Unit
        viewModel = SettingsViewModel(appPreferencesRepository)
        advanceUntilIdle()

        viewModel.setCloseToServiceHealthThreshold(50)
        advanceUntilIdle()

        assertEquals(50, viewModel.uiState.value.closeToServiceHealthThreshold)
        coVerify(exactly = 1) { appPreferencesRepository.setCloseToServiceHealthThreshold(50) }
    }

    @Test
    fun setCloseToServiceHealthThreshold_clamps_to_min() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        coEvery { appPreferencesRepository.setCloseToServiceHealthThreshold(any()) } returns Unit
        viewModel = SettingsViewModel(appPreferencesRepository)
        advanceUntilIdle()

        viewModel.setCloseToServiceHealthThreshold(0)
        advanceUntilIdle()

        assertEquals(AppPreferencesRepository.MIN_THRESHOLD, viewModel.uiState.value.closeToServiceHealthThreshold)
        coVerify(exactly = 1) { appPreferencesRepository.setCloseToServiceHealthThreshold(AppPreferencesRepository.MIN_THRESHOLD) }
    }

    @Test
    fun setCloseToServiceHealthThreshold_clamps_to_max() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        coEvery { appPreferencesRepository.setCloseToServiceHealthThreshold(any()) } returns Unit
        viewModel = SettingsViewModel(appPreferencesRepository)
        advanceUntilIdle()

        viewModel.setCloseToServiceHealthThreshold(200)
        advanceUntilIdle()

        assertEquals(AppPreferencesRepository.MAX_THRESHOLD, viewModel.uiState.value.closeToServiceHealthThreshold)
        coVerify(exactly = 1) { appPreferencesRepository.setCloseToServiceHealthThreshold(AppPreferencesRepository.MAX_THRESHOLD) }
    }

    @Test
    fun recordMaintenanceNotificationPermissionRequest_updatesStateAndPersists() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        coEvery { appPreferencesRepository.setHasRequestedMaintenanceNotificationPermission() } returns Unit
        viewModel = SettingsViewModel(appPreferencesRepository)
        advanceUntilIdle()

        viewModel.recordMaintenanceNotificationPermissionRequest()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.hasRequestedMaintenanceNotificationPermission)
        coVerify(exactly = 1) { appPreferencesRepository.setHasRequestedMaintenanceNotificationPermission() }
    }
}
