package com.you.bikecompanion.ui.settings

import com.you.bikecompanion.data.preferences.AppPreferencesRepository
import com.you.bikecompanion.data.preferences.SecurePreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SettingsViewModel]: API key save/clear, snackbar consumption,
 * and health threshold (delegation to [AppPreferencesRepository]).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val appPreferencesRepository = mockk<AppPreferencesRepository>()
    private val securePreferences = mockk<SecurePreferencesRepository>()

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        every { appPreferencesRepository.closeToServiceHealthThreshold } returns flowOf(20)
        every { securePreferences.apiKey } returns flowOf(null)
        every { securePreferences.setApiKey(any()) } returns Unit
        every { securePreferences.clearApiKey() } returns Unit
    }

    @Test
    fun init_sets_hasStoredApiKey_false_when_no_key() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        every { securePreferences.apiKey } returns flowOf(null)
        viewModel = SettingsViewModel(appPreferencesRepository, securePreferences)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasStoredApiKey)
    }

    @Test
    fun init_sets_hasStoredApiKey_true_when_key_present() = runTest(UnconfinedTestDispatcher()) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { securePreferences.apiKey } returns flowOf("my-api-key")
        viewModel = SettingsViewModel(appPreferencesRepository, securePreferences)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasStoredApiKey)
    }

    @Test
    fun updateApiKeyInput_updates_state() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        viewModel = SettingsViewModel(appPreferencesRepository, securePreferences)
        advanceUntilIdle()

        viewModel.updateApiKeyInput("abc")
        assertEquals("abc", viewModel.uiState.value.apiKeyInput)

        viewModel.updateApiKeyInput("abc123")
        assertEquals("abc123", viewModel.uiState.value.apiKeyInput)
    }

    @Test
    fun saveApiKey_calls_securePreferences_and_clears_input_and_sets_snackbar() = runTest(testDispatcher) {
        viewModel = SettingsViewModel(appPreferencesRepository, securePreferences)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateApiKeyInput("  key123  ")

        viewModel.saveApiKey()

        verify(exactly = 1) { securePreferences.setApiKey("key123") }
        assertEquals("", viewModel.uiState.value.apiKeyInput)
        assertEquals(SettingsSnackbar.ApiKeySaved, viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun saveApiKey_does_nothing_when_input_empty() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        viewModel = SettingsViewModel(appPreferencesRepository, securePreferences)
        advanceUntilIdle()

        viewModel.saveApiKey()

        verify(exactly = 0) { securePreferences.setApiKey(any()) }
        assertNull(viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun saveApiKey_does_nothing_when_input_whitespace_only() = runTest(testDispatcher) {
        viewModel = SettingsViewModel(appPreferencesRepository, securePreferences)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateApiKeyInput("   ")

        viewModel.saveApiKey()

        verify(exactly = 0) { securePreferences.setApiKey(any()) }
    }

    @Test
    fun clearApiKey_calls_securePreferences_and_sets_snackbar() = runTest(testDispatcher) {
        viewModel = SettingsViewModel(appPreferencesRepository, securePreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearApiKey()

        verify(exactly = 1) { securePreferences.clearApiKey() }
        assertEquals(SettingsSnackbar.ApiKeyCleared, viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun consumeSnackbarMessage_clears_snackbar() = runTest(testDispatcher) {
        every { securePreferences.apiKey } returns flowOf("key")
        viewModel = SettingsViewModel(appPreferencesRepository, securePreferences)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.updateApiKeyInput("x")
        viewModel.saveApiKey()
        assertEquals(SettingsSnackbar.ApiKeySaved, viewModel.uiState.value.snackbarMessage)

        viewModel.consumeSnackbarMessage()

        assertNull(viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun setCloseToServiceHealthThreshold_clamps_and_updates_state() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        coEvery { appPreferencesRepository.setCloseToServiceHealthThreshold(any()) } returns Unit
        viewModel = SettingsViewModel(appPreferencesRepository, securePreferences)
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
        viewModel = SettingsViewModel(appPreferencesRepository, securePreferences)
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
        viewModel = SettingsViewModel(appPreferencesRepository, securePreferences)
        advanceUntilIdle()

        viewModel.setCloseToServiceHealthThreshold(200)
        advanceUntilIdle()

        assertEquals(AppPreferencesRepository.MAX_THRESHOLD, viewModel.uiState.value.closeToServiceHealthThreshold)
        coVerify(exactly = 1) { appPreferencesRepository.setCloseToServiceHealthThreshold(AppPreferencesRepository.MAX_THRESHOLD) }
    }
}
