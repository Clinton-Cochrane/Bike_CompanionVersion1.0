package com.you.bikecompanion.ui.garage

import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.component.ComponentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SimpleAddBikeViewModel]. Verifies that saving a bike creates a [BikeEntity]
 * with drivetrain and brake type and triggers [ComponentRepository.seedComponentsForBikeType].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SimpleAddBikeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val bikeRepository = mockk<BikeRepository>()
    private val componentRepository = mockk<ComponentRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveBike_createsBikeWithDrivetrainAndBrakeType() = runTest(testDispatcher) {
        val bikeSlot = slot<com.you.bikecompanion.data.bike.BikeEntity>()
        coEvery { bikeRepository.insertBike(capture(bikeSlot)) } returns 42L
        coEvery { componentRepository.seedComponentsForBikeType(any(), any(), any()) } returns Unit
        val viewModel = SimpleAddBikeViewModel(bikeRepository, componentRepository)

        viewModel.saveBike("Commuter", "1x", "disc_hydraulic")
        testDispatcher.scheduler.advanceUntilIdle()

        val bike = bikeSlot.captured
        assertEquals("Commuter", bike.name)
        assertEquals("1x", bike.drivetrainType)
        assertEquals("disc_hydraulic", bike.brakeType)
        coVerify { bikeRepository.insertBike(any()) }
        coVerify { componentRepository.seedComponentsForBikeType(42L, "1x", "disc_hydraulic") }
    }

    @Test
    fun saveBike_singleSpeed_callsSeedWithSingleSpeedAndRim() = runTest(testDispatcher) {
        coEvery { bikeRepository.insertBike(any()) } returns 1L
        coEvery { componentRepository.seedComponentsForBikeType(any(), any(), any()) } returns Unit
        val viewModel = SimpleAddBikeViewModel(bikeRepository, componentRepository)

        viewModel.saveBike("Fixie", "single_speed", "rim")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { componentRepository.seedComponentsForBikeType(1L, "single_speed", "rim") }
    }

    @Test
    fun saveBike_emitsNewBikeOutcome() = runTest(testDispatcher) {
        coEvery { bikeRepository.insertBike(any()) } returns 99L
        coEvery { componentRepository.seedComponentsForBikeType(any(), any(), any()) } returns Unit
        val viewModel = SimpleAddBikeViewModel(bikeRepository, componentRepository)

        viewModel.saveBike("Road", "multi_speed", "disc_mechanical")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SaveOutcome.NewBike(99L), viewModel.uiState.value.saveOutcome)
    }

    @Test
    fun saveBike_emptyName_doesNotCallRepository() = runTest(testDispatcher) {
        val viewModel = SimpleAddBikeViewModel(bikeRepository, componentRepository)

        viewModel.saveBike("", "1x", "disc_hydraulic")
        viewModel.saveBike("   ", "1x", "disc_hydraulic")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { bikeRepository.insertBike(any()) }
        coVerify(exactly = 0) { componentRepository.seedComponentsForBikeType(any(), any(), any()) }
    }

    @Test
    fun clearSaveOutcome_clearsState() = runTest(testDispatcher) {
        coEvery { bikeRepository.insertBike(any()) } returns 1L
        coEvery { componentRepository.seedComponentsForBikeType(any(), any(), any()) } returns Unit
        val viewModel = SimpleAddBikeViewModel(bikeRepository, componentRepository)
        viewModel.saveBike("Bike", "1x", "rim")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearSaveOutcome()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.saveOutcome)
    }
}
