package com.clintoncochrane.bikecompanion.ui.trip

import androidx.lifecycle.SavedStateHandle
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.bike.BikeRepository
import com.clintoncochrane.bikecompanion.data.ride.RideEntity
import com.clintoncochrane.bikecompanion.data.ride.RideRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RideDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val rideRepository = mockk<RideRepository>()
    private val bikeRepository = mockk<BikeRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadRide_matchingRideAndBike_exposesReadOnlyRideDetails() = runTest(testDispatcher) {
        val selectedRide = ride(id = 2L, bikeId = 7L)
        every { rideRepository.getAllRides() } returns flowOf(listOf(ride(id = 1L, bikeId = null), selectedRide))
        every { bikeRepository.getAllBikes() } returns flowOf(listOf(BikeEntity(id = 7L, name = "Commuter", createdAt = 1L)))

        val viewModel = RideDetailViewModel(
            SavedStateHandle(mapOf("rideId" to selectedRide.id.toString())),
            rideRepository,
            bikeRepository,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(selectedRide, viewModel.uiState.value.ride)
        assertEquals("Commuter", viewModel.uiState.value.bike?.name)
        assertFalse(viewModel.uiState.value.loading)
    }

    @Test
    fun loadRide_missingRide_exposesNotFoundState() = runTest(testDispatcher) {
        every { rideRepository.getAllRides() } returns flowOf(emptyList())
        every { bikeRepository.getAllBikes() } returns flowOf(emptyList())

        val viewModel = RideDetailViewModel(
            SavedStateHandle(mapOf("rideId" to "999")),
            rideRepository,
            bikeRepository,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.ride)
        assertFalse(viewModel.uiState.value.loading)
    }

    private fun ride(id: Long, bikeId: Long?) = RideEntity(
        id = id,
        bikeId = bikeId,
        distanceKm = 12.5,
        durationMs = 3_600_000L,
        startedAt = 1L,
        endedAt = 3_600_001L,
    )
}
