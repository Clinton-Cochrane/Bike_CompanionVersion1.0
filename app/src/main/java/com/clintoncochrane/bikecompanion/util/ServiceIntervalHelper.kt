package com.clintoncochrane.bikecompanion.util

import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalEntity

/**
 * Helper for service interval health calculations and display.
 * Considers both distance and time; uses the lower (most urgent) for display and sorting.
 */
object ServiceIntervalHelper {

    data class IntervalDescription(
        val remainingKm: Int?,
        val remainingTimeSeconds: Long?,
        val expectedIntervalReached: Boolean,
    )

    /**
     * Builds localized-display inputs without treating the estimate as guaranteed life remaining.
     */
    fun description(interval: ServiceIntervalEntity): IntervalDescription {
        val remainingKm = if (interval.intervalKm > 0) {
            (interval.intervalKm - interval.trackedKm).coerceAtLeast(0.0).toInt()
        } else null
        val remainingTimeSeconds = if (interval.intervalTimeSeconds != null && interval.intervalTimeSeconds > 0) {
            val tracked = interval.trackedTimeSeconds ?: 0L
            (interval.intervalTimeSeconds - tracked).coerceAtLeast(0L)
        } else null
        val distanceIntervalReached = interval.intervalKm > 0 && interval.trackedKm >= interval.intervalKm
        val timeIntervalReached = interval.intervalTimeSeconds != null &&
            interval.intervalTimeSeconds > 0 &&
            (interval.trackedTimeSeconds ?: 0L) >= interval.intervalTimeSeconds
        return IntervalDescription(
            remainingKm = remainingKm,
            remainingTimeSeconds = remainingTimeSeconds,
            expectedIntervalReached = distanceIntervalReached || timeIntervalReached,
        )
    }

    /**
     * Computes health percent for a service interval. 100 = new, 0 = due/overdue.
     * Uses the lower of km-based and time-based health when both apply.
     */
    fun healthPercent(interval: ServiceIntervalEntity): Int {
        val kmHealth = if (interval.intervalKm > 0) {
            ((interval.intervalKm - interval.trackedKm) / interval.intervalKm * 100).toInt().coerceIn(0, 100)
        } else 100
        val timeHealth = if (interval.intervalTimeSeconds != null && interval.intervalTimeSeconds > 0) {
            val tracked = interval.trackedTimeSeconds ?: 0L
            ((interval.intervalTimeSeconds - tracked) / interval.intervalTimeSeconds.toDouble() * 100).toInt().coerceIn(0, 100)
        } else 100
        return minOf(kmHealth, timeHealth)
    }

    /**
     * Returns the minimum health percent across all intervals for NEXT_SERVICE sorting.
     * Lower = more urgent. Components are sorted ascending by this value.
     */
    fun minHealthForSort(intervals: List<ServiceIntervalEntity>): Int {
        if (intervals.isEmpty()) return 100
        return intervals.minOf { healthPercent(it) }
    }
}
