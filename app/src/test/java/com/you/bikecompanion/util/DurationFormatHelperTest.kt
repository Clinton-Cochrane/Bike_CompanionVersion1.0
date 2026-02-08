package com.you.bikecompanion.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [DurationFormatHelper].
 * Ensures duration formatting is correct for HH:MM:SS display.
 */
class DurationFormatHelperTest {

    @Test
    fun formatDurationMs_zero_returnsZeroColonZeroZeroZeroZero() {
        assertEquals("0:00:00", DurationFormatHelper.formatDurationMs(0))
    }

    @Test
    fun formatDurationMs_secondsOnly_formatsCorrectly() {
        assertEquals("0:00:34", DurationFormatHelper.formatDurationMs(34_000))
    }

    @Test
    fun formatDurationMs_minutesAndSeconds_formatsCorrectly() {
        assertEquals("0:12:34", DurationFormatHelper.formatDurationMs(754_000))
    }

    @Test
    fun formatDurationMs_hoursMinutesSeconds_formatsCorrectly() {
        assertEquals("1:05:00", DurationFormatHelper.formatDurationMs(3_900_000))
    }

    @Test
    fun formatDurationMs_negativeClampedToZero() {
        assertEquals("0:00:00", DurationFormatHelper.formatDurationMs(-1000))
    }

    @Test
    fun formatDurationSeconds_zero_returnsZeroColonZeroZeroZeroZero() {
        assertEquals("0:00:00", DurationFormatHelper.formatDurationSeconds(0))
    }

    @Test
    fun formatDurationSeconds_largeValue_formatsCorrectly() {
        assertEquals("12:34:56", DurationFormatHelper.formatDurationSeconds(45_296))
    }
}
