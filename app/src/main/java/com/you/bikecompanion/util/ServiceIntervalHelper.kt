package com.you.bikecompanion.util

import com.you.bikecompanion.data.component.ServiceIntervalEntity

/**
 * Helper for service interval health calculations and display.
 * Considers both distance and time; uses the lower (most urgent) for display and sorting.
 */
object ServiceIntervalHelper {

    data class IntervalDescription(
        val kmText: String?,
        val timeText: String?,
    )

    /**
     * Builds display description parts for an interval (km and/or time).
     * - kmText: e.g. "180km of 250km left"
     * - timeText: e.g. "2w" (remaining time, human-readable)
     */
    fun description(interval: ServiceIntervalEntity): IntervalDescription {
        val kmText = if (interval.intervalKm > 0) {
            val remaining = (interval.intervalKm - interval.trackedKm).coerceAtLeast(0.0)
            "${remaining.toInt()}km of ${interval.intervalKm.toInt()}km left"
        } else null
        val timeText = if (interval.intervalTimeSeconds != null && interval.intervalTimeSeconds > 0) {
            val tracked = interval.trackedTimeSeconds ?: 0L
            val remaining = (interval.intervalTimeSeconds - tracked).coerceAtLeast(0L)
            IntervalTimeConstants.formatRemainingSeconds(remaining) + " left"
        } else null
        return IntervalDescription(kmText, timeText)
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
