package com.clintoncochrane.bikecompanion.data.ride

/** Outcome of validating and attempting to save a newly completed ride. */
enum class RideSaveResult {
    SAVED,
    DISCARDED_EMPTY,
    REJECTED_INVALID,
}

internal object RideSaveValidator {
    fun validate(ride: RideEntity): RideSaveResult {
        if (ride.distanceKm < 0.0 || ride.durationMs < 0L) {
            return RideSaveResult.REJECTED_INVALID
        }
        if (ride.distanceKm.isFinite().not() ||
            ride.avgSpeedKmh.isFinite().not() ||
            ride.maxSpeedKmh.isFinite().not() ||
            ride.elevGainM.isFinite().not() ||
            ride.elevLossM.isFinite().not()
        ) {
            return RideSaveResult.REJECTED_INVALID
        }
        if (ride.avgSpeedKmh < 0.0 ||
            ride.maxSpeedKmh < 0.0 ||
            ride.elevGainM < 0.0 ||
            ride.elevLossM < 0.0
        ) {
            return RideSaveResult.REJECTED_INVALID
        }
        if (ride.distanceKm == 0.0 && ride.source != RideSource.HEALTH_CONNECT) {
            return RideSaveResult.DISCARDED_EMPTY
        }
        return RideSaveResult.SAVED
    }
}
