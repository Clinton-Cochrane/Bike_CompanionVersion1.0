package com.you.bikecompanion.util

import com.you.bikecompanion.data.ride.RideEntity
import com.you.bikecompanion.data.ride.RideSource

/**
 * Formats ride stats for display. When a stat is unavailable (e.g. from Health Connect import
 * which does not provide elevation or max speed), returns a placeholder instead of "0".
 */
object RideDisplayHelper {

    /** Rides longer than this should be flagged for user review. */
    const val FLAG_DURATION_MS = 10L * 60 * 60 * 1000

    /** Duration threshold (2h) above which zero-distance rides are flagged. */
    const val FLAG_DURATION_NO_DISTANCE_MS = 2L * 60 * 60 * 1000

    /**
     * Returns true when the ride duration exceeds the review threshold (e.g. > 10h).
     */
    fun isLongRideNeedsReview(durationMs: Long): Boolean = durationMs > FLAG_DURATION_MS

    /**
     * Returns true when the ride has duration over 2h but distance is 0.
     * Suggests possible bad data (e.g. tracking started but GPS didn't record movement).
     */
    fun isZeroDistanceLongDurationNeedsReview(durationMs: Long, distanceKm: Double): Boolean =
        durationMs > FLAG_DURATION_NO_DISTANCE_MS && distanceKm <= 0.0

    /**
     * Reason a ride is flagged for review. Used to show details in the review dialog.
     */
    sealed class RideFlagReason {
        /** Ride duration exceeds threshold (e.g. > 10h). May indicate bad data or very long ride. */
        data object LongRide : RideFlagReason()
        /** Ride has duration over 2h but zero distance. Suggests GPS/tracking issue. */
        data object NoDistance : RideFlagReason()
    }

    /**
     * Returns the reason this ride is flagged for review, or null if no flag applies.
     * No-distance takes precedence when both conditions match (more likely data error).
     */
    fun getRideFlagReason(ride: RideEntity): RideFlagReason? = when {
        isZeroDistanceLongDurationNeedsReview(ride.durationMs, ride.distanceKm) -> RideFlagReason.NoDistance
        isLongRideNeedsReview(ride.durationMs) -> RideFlagReason.LongRide
        else -> null
    }

    /**
     * Returns true when the ride should show the Review chip (has a flag and is not dismissed).
     */
    fun shouldShowReviewChip(ride: RideEntity, dismissedRideIds: Set<Long>): Boolean =
        getRideFlagReason(ride) != null && ride.id !in dismissedRideIds

    /**
     * Returns true when the ride had placeholder components at start and should show the
     * "Update bike info" reminder chip (not dismissed, not snoozed).
     *
     * @param snoozedUntilMs Epoch ms until which reminders are snoozed; null = not snoozed.
     */
    fun shouldShowPlaceholderReminderChip(
        ride: RideEntity,
        dismissedPlaceholderReminderIds: Set<Long>,
        snoozedUntilMs: Long?,
    ): Boolean {
        if (!ride.hadPlaceholdersAtStart || ride.id in dismissedPlaceholderReminderIds) return false
        if (snoozedUntilMs != null && System.currentTimeMillis() < snoozedUntilMs) return false
        return true
    }

    /**
     * Returns true when the given numeric stat is known to be unavailable (not provided by source).
     * Health Connect and manual entries do not supply elevation or max speed.
     */
    fun isStatUnavailable(value: Double, source: RideSource): Boolean =
        value == 0.0 && (source == RideSource.HEALTH_CONNECT || source == RideSource.MANUAL)

    /**
     * Formats max speed for display. Returns placeholder when unavailable.
     */
    fun formatMaxSpeedKmh(value: Double, source: RideSource, placeholder: String): String =
        if (isStatUnavailable(value, source)) placeholder else "%.1f km/h".format(value)

    /**
     * Formats elevation (gain or loss) for display. Returns placeholder when unavailable.
     */
    fun formatElevationM(value: Double, source: RideSource, placeholder: String): String =
        if (isStatUnavailable(value, source)) placeholder else "%.0f m".format(value)

    /**
     * Returns true when elevation data is unavailable (both gain and loss are 0 from a source
     * that does not provide elevation).
     */
    fun isElevationUnavailable(gainM: Double, lossM: Double, source: RideSource): Boolean =
        gainM == 0.0 && lossM == 0.0 && (source == RideSource.HEALTH_CONNECT || source == RideSource.MANUAL)

    /**
     * Formats elevation gain and loss as a combined value for display.
     * E.g. "+150 / -120" (gain − loss). Returns placeholder when unavailable.
     */
    fun formatElevationGainLoss(gainM: Double, lossM: Double, source: RideSource, placeholder: String): String =
        if (isElevationUnavailable(gainM, lossM, source)) placeholder
        else "+%.0f / -%.0f m".format(gainM, lossM)

    /**
     * Net elevation change (gain − loss) in meters. Positive = net climb, negative = net descent.
     */
    fun elevationNetDelta(gainM: Double, lossM: Double): Double = gainM - lossM
}
