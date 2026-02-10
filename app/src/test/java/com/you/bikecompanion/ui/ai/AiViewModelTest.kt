package com.you.bikecompanion.ui.ai

import com.you.bikecompanion.ai.AiApiClient
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.component.ComponentRepository
import com.you.bikecompanion.data.preferences.SecurePreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AiViewModel]. Verifies context building (Bikes + Drivetrain sections)
 * and that the AI client receives the summary when the user sends a message.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AiViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val bikeRepository = mockk<BikeRepository>()
    private val componentRepository = mockk<ComponentRepository>()
    private val aiApiClient = mockk<AiApiClient>()
    private val securePreferences = mockk<SecurePreferencesRepository>()

    private lateinit var viewModel: AiViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { securePreferences.apiKey } returns flowOf("test-key")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendMessage_passes_context_with_bikes_section_to_client() = runTest(testDispatcher) {
        val bike = BikeEntity(id = 1, name = "Road Bike", createdAt = 0)
        val component = ComponentEntity(
            id = 1,
            bikeId = 1,
            type = "chain",
            name = "Chain 11s",
            lifespanKm = 3000.0,
            distanceUsedKm = 500.0,
            installedAt = 0,
        )
        every { bikeRepository.getAllBikes() } returns flowOf(listOf(bike))
        coEvery { componentRepository.getAllComponents() } returns listOf(component)

        val summarySlot = slot<String>()
        coEvery { aiApiClient.send(any(), capture(summarySlot)) } returns Result.success("Reply")

        viewModel = AiViewModel(
            bikeRepository = bikeRepository,
            componentRepository = componentRepository,
            aiApiClient = aiApiClient,
            securePreferences = securePreferences,
            ioDispatcher = testDispatcher,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateInput("What needs service?")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { aiApiClient.send(userMessage = "What needs service?", componentHealthSummary = any()) }
        val summary = summarySlot.captured
        assertTrue("Context should contain Bikes section", summary.contains("Bikes:"))
        assertTrue("Context should contain bike name", summary.contains("Road Bike"))
        assertTrue("Context should contain component health", summary.contains("Chain 11s"))
    }

    @Test
    fun sendMessage_includes_drivetrain_section_when_chain_cassette_present() = runTest(testDispatcher) {
        val bike = BikeEntity(id = 1, name = "MTB", createdAt = 0)
        val chain = ComponentEntity(
            id = 1,
            bikeId = 1,
            type = "chain",
            name = "Chain",
            makeModel = "Shimano",
            lifespanKm = 3000.0,
            distanceUsedKm = 0.0,
            installedAt = 0,
        )
        val cassette = ComponentEntity(
            id = 2,
            bikeId = 1,
            type = "cassette",
            name = "Cassette 11-42",
            lifespanKm = 10000.0,
            distanceUsedKm = 0.0,
            installedAt = 0,
        )
        every { bikeRepository.getAllBikes() } returns flowOf(listOf(bike))
        coEvery { componentRepository.getAllComponents() } returns listOf(chain, cassette)

        val summarySlot = slot<String>()
        coEvery { aiApiClient.send(any(), capture(summarySlot)) } returns Result.success("OK")

        viewModel = AiViewModel(
            bikeRepository = bikeRepository,
            componentRepository = componentRepository,
            aiApiClient = aiApiClient,
            securePreferences = securePreferences,
            ioDispatcher = testDispatcher,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateInput("Trip readiness?")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        val summary = summarySlot.captured
        assertTrue("Context should contain Drivetrain section", summary.contains("Drivetrain:"))
        assertTrue("Drivetrain should list chain", summary.contains("chain") && summary.contains("Chain"))
        assertTrue("Drivetrain should list cassette", summary.contains("cassette"))
    }

    @Test
    fun uiState_hasApiKey_true_when_secure_preferences_has_key() = runTest(testDispatcher) {
        every { securePreferences.apiKey } returns flowOf("my-key")
        every { bikeRepository.getAllBikes() } returns flowOf(emptyList())

        viewModel = AiViewModel(
            bikeRepository = bikeRepository,
            componentRepository = componentRepository,
            aiApiClient = aiApiClient,
            securePreferences = securePreferences,
            ioDispatcher = testDispatcher,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasApiKey)
    }

    @Test
    fun uiState_hasApiKey_false_when_secure_preferences_key_null() = runTest(testDispatcher) {
        every { securePreferences.apiKey } returns flowOf(null)
        every { bikeRepository.getAllBikes() } returns flowOf(emptyList())

        viewModel = AiViewModel(
            bikeRepository = bikeRepository,
            componentRepository = componentRepository,
            aiApiClient = aiApiClient,
            securePreferences = securePreferences,
            ioDispatcher = testDispatcher,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.hasApiKey)
    }
}
