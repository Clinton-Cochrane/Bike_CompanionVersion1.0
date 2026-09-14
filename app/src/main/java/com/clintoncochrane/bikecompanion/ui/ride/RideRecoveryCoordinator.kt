package com.clintoncochrane.bikecompanion.ui.ride

import com.clintoncochrane.bikecompanion.data.ride.ActiveRideCheckpoint
import com.clintoncochrane.bikecompanion.data.ride.ActiveRideCheckpointRepository
import com.clintoncochrane.bikecompanion.data.ride.RideEntity
import com.clintoncochrane.bikecompanion.data.ride.RideRepository
import com.clintoncochrane.bikecompanion.data.ride.RideSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** Coordinates one recovery decision without allowing duplicate terminal actions. */
class RideRecoveryCoordinator @Inject constructor(
    private val checkpointRepository: ActiveRideCheckpointRepository,
    private val rideRepository: RideRepository,
) {
    private val actionMutex = Mutex()
    private val consumedCheckpointIds = mutableSetOf<String>()

    suspend fun load(): ActiveRideCheckpoint? = checkpointRepository.get()

    suspend fun discard() = actionMutex.withLock { checkpointRepository.clear() }

    /** Saves through normal accounting and clears after a terminal save outcome. */
    suspend fun save(checkpoint: ActiveRideCheckpoint): Boolean = actionMutex.withLock {
        val checkpointId = checkpoint.recoveryId()
        if (!consumedCheckpointIds.add(checkpointId)) return@withLock true
        val bikeId = checkpoint.bikeId ?: run {
            consumedCheckpointIds.remove(checkpointId)
            return@withLock false
        }
        val endedAt = checkpoint.checkpointedAtMs
        val activePauseMs = if (checkpoint.isPaused && checkpoint.pausedAtMs > 0L) {
            (endedAt - checkpoint.pausedAtMs).coerceAtLeast(0L)
        } else 0L
        val ride = RideEntity(
            bikeId = bikeId, distanceKm = checkpoint.distanceKm,
            durationMs = (endedAt - checkpoint.startTimeMs - checkpoint.totalPausedDurationMs - activePauseMs).coerceAtLeast(0L),
            avgSpeedKmh = checkpoint.avgSpeedKmh, maxSpeedKmh = checkpoint.maxSpeedKmh,
            elevGainM = checkpoint.elevGainM, elevLossM = checkpoint.elevLossM,
            startedAt = checkpoint.startTimeMs, endedAt = endedAt, source = RideSource.APP,
            hadPlaceholdersAtStart = checkpoint.hadPlaceholdersAtStart,
            recoveryCheckpointId = checkpointId,
        )
        val saved = runCatching { rideRepository.saveRecoveredRideAndUpdateBikeAndComponents(ride) }.isSuccess
        if (saved) checkpointRepository.clear() else consumedCheckpointIds.remove(checkpointId)
        saved
    }
}

internal fun ActiveRideCheckpoint.recoveryId(): String =
    "active-ride:$startTimeMs:${bikeId ?: "unassigned"}"
