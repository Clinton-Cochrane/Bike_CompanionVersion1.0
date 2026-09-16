package com.clintoncochrane.bikecompanion.ui.garage

import androidx.lifecycle.SavedStateHandle
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.bike.BikeRepository
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Test

class AddEditBikeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val bikeRepository = mockk<BikeRepository>()
    private val componentRepository = mockk<ComponentRepository>()

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @Test
    fun saveBike_editBaseline_preservesRecordedDistance() = runTest(dispatcher) {
        val bike = BikeEntity(
            id = 1L,
            name = "Test",
            baselineDistanceKm = 1_000.0,
            totalDistanceKm = 1_075.0,
            createdAt = 1_000L,
        )
        coEvery { bikeRepository.getBikeById(1L) } returns bike
        coEvery { bikeRepository.updateBike(any()) } coAnswers { }
        val viewModel = AddEditBikeViewModel(
            SavedStateHandle(mapOf("bikeId" to "1")), bikeRepository, componentRepository,
        )
        advanceUntilIdle()

        viewModel.saveBike(bike, "800")
        advanceUntilIdle()

        coVerify { bikeRepository.updateBike(match { it.baselineDistanceKm == 800.0 && it.totalDistanceKm == 875.0 }) }
    }

    @Test
    fun saveBike_invalidBaseline_doesNotCallRepository() = runTest(dispatcher) {
        val viewModel = AddEditBikeViewModel(SavedStateHandle(), bikeRepository, componentRepository)

        viewModel.saveBike(BikeEntity(name = "Test", createdAt = 1_000L), "-2")
        advanceUntilIdle()

        coVerify(exactly = 0) { bikeRepository.insertBike(any()) }
    }

    @Test
    fun saveBike_newBike_seedsComponentsWithStartingOdometer() = runTest(dispatcher) {
        val viewModel = AddEditBikeViewModel(SavedStateHandle(), bikeRepository, componentRepository)
        coEvery { bikeRepository.insertBike(any()) } returns 42L
        coEvery { componentRepository.seedDefaultComponentsIfEmpty(42L, 750.0) } returns Unit

        viewModel.saveBike(BikeEntity(name = "Test", createdAt = 1_000L), "750")
        advanceUntilIdle()

        coVerify { componentRepository.seedDefaultComponentsIfEmpty(42L, 750.0) }
    }

    @Test
    fun saveBike_blankDisplayName_insertsBike() = runTest(dispatcher) {
        coEvery { bikeRepository.insertBike(any()) } returns 42L
        coEvery { componentRepository.seedDefaultComponentsIfEmpty(42L, 0.0) } returns Unit
        val viewModel = AddEditBikeViewModel(SavedStateHandle(), bikeRepository, componentRepository)

        viewModel.saveBike(BikeEntity(name = "", createdAt = 1_000L))
        advanceUntilIdle()

        coVerify { bikeRepository.insertBike(match { it.name.isEmpty() }) }
        assertEquals(SaveOutcome.NewBike(42L), viewModel.uiState.value.saveOutcome)
    }
}
