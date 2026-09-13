package com.clintoncochrane.bikecompanion.ui.ride

import com.clintoncochrane.bikecompanion.data.ride.ActiveRideCheckpoint
import com.clintoncochrane.bikecompanion.data.ride.ActiveRideCheckpointRepository
import com.clintoncochrane.bikecompanion.data.ride.RideRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RideRecoveryCoordinatorTest {
    private val checkpointRepository = mockk<ActiveRideCheckpointRepository>(relaxed = true)
    private val rideRepository = mockk<RideRepository>()
    private val coordinator = RideRecoveryCoordinator(checkpointRepository, rideRepository)

    @Test
    fun resume_keepsCheckpointAndReturnsItForServiceRestoration() = runTest {
        coEvery { checkpointRepository.get() } returns checkpoint

        assertEquals(checkpoint, coordinator.load())
        coVerify(exactly = 0) { checkpointRepository.clear() }
    }

    @Test
    fun save_successfullySavedRide_clearsCheckpointOnlyOnce() = runTest {
        coEvery { rideRepository.saveRecoveredRideAndUpdateBikeAndComponents(any()) } returns true

        assertTrue(coordinator.save(checkpoint))
        assertTrue(coordinator.save(checkpoint))

        coVerify(exactly = 1) { rideRepository.saveRecoveredRideAndUpdateBikeAndComponents(any()) }
        coVerify(exactly = 1) { checkpointRepository.clear() }
    }

    @Test
    fun save_failure_keepsCheckpointForRetry() = runTest {
        coEvery { rideRepository.saveRecoveredRideAndUpdateBikeAndComponents(any()) } throws IllegalStateException()

        assertFalse(coordinator.save(checkpoint))

        coVerify(exactly = 0) { checkpointRepository.clear() }
    }

    @Test
    fun discard_clearsOnlyTheCheckpoint() = runTest {
        coordinator.discard()

        coVerify(exactly = 1) { checkpointRepository.clear() }
        coVerify(exactly = 0) { rideRepository.saveRecoveredRideAndUpdateBikeAndComponents(any()) }
    }

    private companion object {
        val checkpoint = ActiveRideCheckpoint(
            bikeId = 7L, hadPlaceholdersAtStart = false, startTimeMs = 1_000L,
            distanceKm = 12.5, avgSpeedKmh = 20.0, maxSpeedKmh = 40.0,
            elevGainM = 100.0, elevLossM = 80.0, locationUpdateCount = 8,
            isPaused = false, pausedAtMs = 0L, totalPausedDurationMs = 0L,
            checkpointedAtMs = 2_000L,
        )
    }
}
