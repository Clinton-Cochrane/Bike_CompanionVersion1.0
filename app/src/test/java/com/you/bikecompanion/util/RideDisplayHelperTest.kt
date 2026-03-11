package com.you.bikecompanion.util

import com.you.bikecompanion.data.ride.RideSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class RideDisplayHelperTest {

    private val unavailable = "—"

    @Test
    fun formatMaxSpeedKmh_appRideWithValue_formatsCorrectly() {
        assertEquals("42.5 km/h", RideDisplayHelper.formatMaxSpeedKmh(42.5, RideSource.APP, unavailable))
    }

    @Test
    fun formatMaxSpeedKmh_healthConnectWithZero_returnsPlaceholder() {
        assertEquals(unavailable, RideDisplayHelper.formatMaxSpeedKmh(0.0, RideSource.HEALTH_CONNECT, unavailable))
    }

    @Test
    fun formatMaxSpeedKmh_manualWithZero_returnsPlaceholder() {
        assertEquals(unavailable, RideDisplayHelper.formatMaxSpeedKmh(0.0, RideSource.MANUAL, unavailable))
    }

    @Test
    fun formatMaxSpeedKmh_appRideWithZero_returnsFormattedZero() {
        assertEquals("0.0 km/h", RideDisplayHelper.formatMaxSpeedKmh(0.0, RideSource.APP, unavailable))
    }

    @Test
    fun formatElevationM_healthConnectWithZero_returnsPlaceholder() {
        assertEquals(unavailable, RideDisplayHelper.formatElevationM(0.0, RideSource.HEALTH_CONNECT, unavailable))
    }

    @Test
    fun formatElevationM_appRideWithValue_formatsCorrectly() {
        assertEquals("150 m", RideDisplayHelper.formatElevationM(150.0, RideSource.APP, unavailable))
    }

    @Test
    fun formatElevationGainLoss_available_formatsGainMinusLoss() {
        assertEquals("+150 / -120 m", RideDisplayHelper.formatElevationGainLoss(150.0, 120.0, RideSource.APP, unavailable))
    }

    @Test
    fun formatElevationGainLoss_unavailable_returnsPlaceholder() {
        assertEquals(unavailable, RideDisplayHelper.formatElevationGainLoss(0.0, 0.0, RideSource.HEALTH_CONNECT, unavailable))
    }

    @Test
    fun elevationNetDelta_positiveWhenGainExceedsLoss() {
        assertEquals(30.0, RideDisplayHelper.elevationNetDelta(150.0, 120.0), 0.001)
    }

    @Test
    fun elevationNetDelta_zeroWhenBalanced() {
        assertEquals(0.0, RideDisplayHelper.elevationNetDelta(100.0, 100.0), 0.001)
    }

    @Test
    fun isZeroDistanceLongDurationNeedsReview_over2hZeroDistance_returnsTrue() {
        val threeHoursMs = 3L * 60 * 60 * 1000
        assertTrue(RideDisplayHelper.isZeroDistanceLongDurationNeedsReview(threeHoursMs, 0.0))
    }

    @Test
    fun isZeroDistanceLongDurationNeedsReview_under2hZeroDistance_returnsFalse() {
        val oneHourMs = 1L * 60 * 60 * 1000
        assertFalse(RideDisplayHelper.isZeroDistanceLongDurationNeedsReview(oneHourMs, 0.0))
    }

    @Test
    fun isZeroDistanceLongDurationNeedsReview_over2hWithDistance_returnsFalse() {
        val threeHoursMs = 3L * 60 * 60 * 1000
        assertFalse(RideDisplayHelper.isZeroDistanceLongDurationNeedsReview(threeHoursMs, 15.5))
    }
}
