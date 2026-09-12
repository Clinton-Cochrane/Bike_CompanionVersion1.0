package com.clintoncochrane.bikecompanion.data.ride

/**
 * The minimum durable state needed to reconstruct or save an interrupted ride.
 * GPS coordinates are deliberately excluded; the checkpoint remains local-only.
 */
data class ActiveRideCheckpoint(
    val bikeId: Long?,
    val hadPlaceholdersAtStart: Boolean,
    val startTimeMs: Long,
    val distanceKm: Double,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val elevGainM: Double,
    val elevLossM: Double,
    val locationUpdateCount: Int,
    val isPaused: Boolean,
    val pausedAtMs: Long,
    val totalPausedDurationMs: Long,
    val checkpointedAtMs: Long,
)
