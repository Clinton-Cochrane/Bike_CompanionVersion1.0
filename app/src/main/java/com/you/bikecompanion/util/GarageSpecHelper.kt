package com.you.bikecompanion.util

import com.you.bikecompanion.data.bike.BikeEntity

/**
 * Helper for Garage spec sheet: bike sorting and aggregate stats.
 * Extracted for testability.
 */
object GarageSpecHelper {

    /**
     * Sorts bikes by last ridden (most recent first), then by name for ties.
     * Bikes with null lastRideAt sort last.
     */
    fun sortBikesByLastRidden(bikes: List<BikeEntity>): List<BikeEntity> =
        bikes.sortedWith(
            compareByDescending<BikeEntity> { it.lastRideAt ?: 0L }.thenBy { it.name },
        )

    /**
     * Total distance across all bikes (rider total).
     */
    fun computeTotalDistanceKm(bikes: List<BikeEntity>): Double =
        bikes.sumOf { it.totalDistanceKm }

    /**
     * Bike ID that was ridden most recently, or null when no bike has been ridden.
     */
    fun getLastRiddenBikeId(bikes: List<BikeEntity>): Long? =
        bikes.maxByOrNull { it.lastRideAt ?: 0L }?.takeIf { (it.lastRideAt ?: 0L) > 0 }?.id
}
