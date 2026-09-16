package com.clintoncochrane.bikecompanion.ui.stats

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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val bikeRepository = mockk<BikeRepository>()
    private val rideRepository = mockk<RideRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun stats_aggregatesAllRidesIncludingHistoricalRidesWithoutABike() = runTest(testDispatcher) {
        val roadBike = bike(id = 1L, name = "Road bike")
        every { bikeRepository.getAllBikes() } returns flowOf(listOf(roadBike))
        every { rideRepository.getAllRides() } returns flowOf(
            listOf(
                ride(id = 1L, bikeId = roadBike.id, distanceKm = 12.5, durationMs = 1_800_000),
                ride(id = 2L, bikeId = null, distanceKm = 7.5, durationMs = 900_000),
            ),
        )

        val viewModel = StatsViewModel(bikeRepository, rideRepository)
        advanceUntilIdle()

        assertEquals(20.0, viewModel.uiState.value.allBikesStats.totalDistanceKm, 0.0)
        assertEquals(2_700_000L, viewModel.uiState.value.allBikesStats.totalRideDurationMs)
        assertEquals(2, viewModel.uiState.value.allBikesStats.rideCount)
        assertEquals(0, viewModel.uiState.value.allBikesStats.completedServiceCount)
        assertEquals(12.5, viewModel.uiState.value.bikesWithStats.single().stats.totalDistanceKm, 0.0)
    }

    @Test
    fun stats_bikesWithoutActivityShowZeroForEveryMetric() = runTest(testDispatcher) {
        every { bikeRepository.getAllBikes() } returns flowOf(listOf(bike(id = 1L, name = "Commuter")))
        every { rideRepository.getAllRides() } returns flowOf(emptyList())

        val viewModel = StatsViewModel(bikeRepository, rideRepository)
        advanceUntilIdle()

        assertEquals(
            StatsSummary(),
            viewModel.uiState.value.bikesWithStats.single().stats,
        )
    }

    @Test
    fun stats_withoutBikesHasAnEmptyBikeListAndZeroAllBikesTotals() = runTest(testDispatcher) {
        every { bikeRepository.getAllBikes() } returns flowOf(emptyList())
        every { rideRepository.getAllRides() } returns flowOf(emptyList())

        val viewModel = StatsViewModel(bikeRepository, rideRepository)
        advanceUntilIdle()

        assertEquals(emptyList<BikeWithStats>(), viewModel.uiState.value.bikesWithStats)
        assertEquals(StatsSummary(), viewModel.uiState.value.allBikesStats)
    }

    @Test
    fun carouselNavigation_keepsTheSelectedIndexWithinBounds() = runTest(testDispatcher) {
        every { bikeRepository.getAllBikes() } returns flowOf(
            listOf(
                bike(id = 1L, name = "Bike 1"),
                bike(id = 2L, name = "Bike 2"),
                bike(id = 3L, name = "Bike 3"),
            ),
        )
        every { rideRepository.getAllRides() } returns flowOf(emptyList())

        val viewModel = StatsViewModel(bikeRepository, rideRepository)
        advanceUntilIdle()

        viewModel.selectNextBike()
        viewModel.selectNextBike()
        viewModel.selectNextBike()
        assertEquals(2, viewModel.uiState.value.selectedBikeIndex)

        viewModel.selectPreviousBike()
        viewModel.selectPreviousBike()
        viewModel.selectPreviousBike()
        assertEquals(0, viewModel.uiState.value.selectedBikeIndex)
    }

    private fun bike(id: Long, name: String) = BikeEntity(
        id = id,
        name = name,
        createdAt = 0L,
    )

    private fun ride(
        id: Long,
        bikeId: Long?,
        distanceKm: Double,
        durationMs: Long,
    ) = RideEntity(
        id = id,
        bikeId = bikeId,
        distanceKm = distanceKm,
        durationMs = durationMs,
        startedAt = 0L,
        endedAt = durationMs,
    )
}
