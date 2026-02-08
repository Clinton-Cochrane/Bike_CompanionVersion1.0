package com.you.bikecompanion.util

/**
 * Time constants for service intervals, in seconds.
 * Used by [DefaultServiceIntervalSpec] to define interval schedules.
 */
object IntervalTimeConstants {
    val TWO_WEEKS = 14 * 24 * 3600L
    val ONE_MONTH = 30 * 24 * 3600L
    val THREE_MONTHS = 90 * 24 * 3600L
    val SIX_MONTHS = 180 * 24 * 3600L
    val TWELVE_MONTHS = 365 * 24 * 3600L
    val EIGHTEEN_MONTHS = 18 * 30 * 24 * 3600L
    val TWENTY_FOUR_MONTHS = 24 * 30 * 24 * 3600L
    val FIFTY_HOURS = 50 * 3600L
    val TWO_HUNDRED_HOURS = 200 * 3600L

    /**
     * Formats remaining seconds into a human-readable string (e.g. "2w", "50h").
     */
    fun formatRemainingSeconds(seconds: Long): String {
        if (seconds <= 0) return "0"
        val hours = seconds / 3600
        val days = hours / 24
        return when {
            hours < 24 -> "${hours}h"
            days < 14 -> "${days}d"
            days <= 31 -> "${days / 7}w"
            days <= 365 -> "${days / 30}mo"
            else -> "${days / 365}y"
        }
    }

    /**
     * Parses a time interval string (e.g. "2 weeks", "50 hours", "1 month") into seconds.
     * @return Parsed seconds, or null if invalid.
     */
    fun parseIntervalTime(input: String): Long? {
        val trimmed = input.trim().lowercase()
        if (trimmed.isEmpty()) return null
        val parts = trimmed.split("\\s+".toRegex())
        if (parts.size < 2) return null
        val num = parts[0].toDoubleOrNull() ?: return null
        val unit = parts[1]
        val secondsPerUnit = when {
            unit.startsWith("hour") -> 3600.0
            unit.startsWith("day") -> 24 * 3600.0
            unit.startsWith("week") -> 7 * 24 * 3600.0
            unit.startsWith("month") -> 30 * 24 * 3600.0
            unit.startsWith("year") -> 365 * 24 * 3600.0
            else -> return null
        }
        return (num * secondsPerUnit).toLong().coerceAtLeast(0)
    }
}
