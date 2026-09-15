package com.clintoncochrane.bikecompanion.ui.trip

import com.clintoncochrane.bikecompanion.data.bike.BikeEntity

const val NO_BIKE_ASSIGNED = -1L

/**
 * Chooses the initial bike for a new ride.
 *
 * Bikes do not currently have a separate retired or disabled state, so every bike returned by
 * [BikeRepository.getAllBikes][com.clintoncochrane.bikecompanion.data.bike.BikeRepository.getAllBikes]
 * is rideable. A Garage carousel selection is deliberately not an input to this policy.
 */
object InitialRideAssignmentPolicy {
    fun initialBikeId(rideableBikes: List<BikeEntity>): Long =
        rideableBikes.singleOrNull()?.id ?: NO_BIKE_ASSIGNED
}
