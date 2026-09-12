package com.clintoncochrane.bikecompanion.location

import com.clintoncochrane.bikecompanion.data.ride.ActiveRideCheckpoint

internal fun RideState.toActiveRideCheckpoint(checkpointedAtMs: Long): ActiveRideCheckpoint =
    ActiveRideCheckpoint(
        bikeId = bikeId.takeIf { it >= 0L },
        hadPlaceholdersAtStart = hadPlaceholdersAtStart,
        startTimeMs = startTimeMs,
        distanceKm = distanceKm,
        avgSpeedKmh = avgSpeedKmh,
        maxSpeedKmh = maxSpeedKmh,
        elevGainM = elevGainM,
        elevLossM = elevLossM,
        locationUpdateCount = locationUpdateCount,
        isPaused = isPaused,
        pausedAtMs = pausedAtMs,
        totalPausedDurationMs = totalPausedDurationMs,
        checkpointedAtMs = checkpointedAtMs,
    )
