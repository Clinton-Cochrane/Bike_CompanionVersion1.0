package com.clintoncochrane.bikecompanion.data.ride

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.clintoncochrane.bikecompanion.location.RideState
import com.clintoncochrane.bikecompanion.location.toActiveRideCheckpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ActiveRideCheckpointRepositoryTest {

    private val scopes = mutableListOf<CoroutineScope>()
    private val files = mutableListOf<File>()

    @After
    fun tearDown() {
        scopes.forEach(CoroutineScope::cancel)
        files.forEach(File::delete)
    }

    @Test
    fun save_newRide_createsCheckpointWithRequiredState() = runTest {
        val repository = createRepository()
        val checkpoint = RideState(
            bikeId = 42L,
            hadPlaceholdersAtStart = true,
            isTracking = true,
            startTimeMs = 1_000L,
        ).toActiveRideCheckpoint(checkpointedAtMs = 1_100L)

        repository.save(checkpoint)

        assertEquals(checkpoint, repository.get())
    }

    @Test
    fun save_newRideWithoutBike_persistsNoBikeAssignment() = runTest {
        val repository = createRepository()

        repository.save(
            RideState(bikeId = -1L, isTracking = true, startTimeMs = 1_000L)
                .toActiveRideCheckpoint(checkpointedAtMs = 1_100L),
        )

        assertNull(repository.get()!!.bikeId)
    }

    @Test
    fun save_existingRide_updatesAccumulatedStats() = runTest {
        val repository = createRepository()
        repository.save(
            RideState(isTracking = true, startTimeMs = 1_000L)
                .toActiveRideCheckpoint(checkpointedAtMs = 1_100L),
        )

        val updated = RideState(
            bikeId = 42L,
            isTracking = true,
            startTimeMs = 1_000L,
            distanceKm = 12.5,
            avgSpeedKmh = 21.4,
            maxSpeedKmh = 38.2,
            elevGainM = 184.0,
            elevLossM = 121.0,
            locationUpdateCount = 17,
        ).toActiveRideCheckpoint(checkpointedAtMs = 31_100L)

        repository.save(updated)

        assertEquals(updated, repository.get())
    }

    @Test
    fun save_pausedRide_persistsPauseState() = runTest {
        val repository = createRepository()
        val paused = RideState(
            bikeId = 42L,
            isTracking = true,
            isPaused = true,
            pausedAtMs = 5_000L,
            totalPausedDurationMs = 300L,
            startTimeMs = 1_000L,
        ).toActiveRideCheckpoint(checkpointedAtMs = 5_100L)

        repository.save(paused)

        val restored = repository.get()!!
        assertTrue(restored.isPaused)
        assertEquals(5_000L, restored.pausedAtMs)
        assertEquals(300L, restored.totalPausedDurationMs)
    }

    @Test
    fun save_resumedRide_updatesAccumulatedPauseDuration() = runTest {
        val repository = createRepository()
        repository.save(
            RideState(
                isTracking = true,
                isPaused = true,
                pausedAtMs = 5_000L,
                startTimeMs = 1_000L,
            ).toActiveRideCheckpoint(checkpointedAtMs = 5_100L),
        )
        val resumed = RideState(
            isTracking = true,
            isPaused = false,
            pausedAtMs = 0L,
            totalPausedDurationMs = 2_000L,
            startTimeMs = 1_000L,
        ).toActiveRideCheckpoint(checkpointedAtMs = 7_100L)

        repository.save(resumed)

        val restored = repository.get()!!
        assertFalse(restored.isPaused)
        assertEquals(0L, restored.pausedAtMs)
        assertEquals(2_000L, restored.totalPausedDurationMs)
    }

    @Test
    fun clear_afterIntentionalCompletion_removesCheckpoint() = runTest {
        val repository = createRepository()
        repository.save(
            RideState(isTracking = true, startTimeMs = 1_000L)
                .toActiveRideCheckpoint(checkpointedAtMs = 1_100L),
        )

        repository.clear()

        assertNull(repository.get())
    }

    @Test
    fun get_afterRepositoryAndDataStoreRecreation_restoresCheckpoint() = runTest {
        val file = File.createTempFile("active_ride_checkpoint", ".preferences_pb")
        files += file
        file.delete()
        val checkpoint = RideState(
            bikeId = 42L,
            isTracking = true,
            isPaused = true,
            pausedAtMs = 4_000L,
            totalPausedDurationMs = 250L,
            startTimeMs = 1_000L,
            distanceKm = 3.75,
        ).toActiveRideCheckpoint(checkpointedAtMs = 4_100L)
        val firstScope = newScope()
        val firstRepository = ActiveRideCheckpointRepository(createDataStore(file, firstScope))
        firstRepository.save(checkpoint)
        firstScope.coroutineContext[Job]?.cancelAndJoin()

        val recreatedRepository = ActiveRideCheckpointRepository(createDataStore(file, newScope()))

        assertEquals(checkpoint, recreatedRepository.get())
    }

    private fun createRepository(): ActiveRideCheckpointRepository {
        val file = File.createTempFile("active_ride_checkpoint", ".preferences_pb")
        files += file
        file.delete()
        return ActiveRideCheckpointRepository(createDataStore(file, newScope()))
    }

    private fun newScope(): CoroutineScope =
        CoroutineScope(Dispatchers.IO + SupervisorJob()).also(scopes::add)

    private fun createDataStore(file: File, scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { file }
}
