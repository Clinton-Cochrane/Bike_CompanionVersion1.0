package com.you.bikecompanion.util

/**
 * Formats duration for display in UI.
 * Uses epoch timestamps internally to avoid timezone issues.
 */
object DurationFormatHelper {

    /** Durations above this are capped for display to avoid absurd values (e.g. from bad data). */
    const val MAX_REASONABLE_DURATION_MS = 24L * 60 * 60 * 1000

    /**
     * Formats a static duration (e.g. completed ride) as HH:MM:SS.
     * @param durationMs Duration in milliseconds.
     * @return String like "0:12:34" or "1:05:00".
     */
    fun formatDurationMs(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0L)
        return formatDurationSeconds(totalSeconds)
    }

    /**
     * Formats total seconds as HH:MM:SS.
     * @param totalSeconds Duration in seconds.
     * @return String like "0:12:34" or "1:05:00".
     */
    fun formatDurationSeconds(totalSeconds: Long): String {
        val seconds = totalSeconds.coerceAtLeast(0L)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return "%d:%02d:%02d".format(hours, minutes, secs)
    }

    /**
     * Formats duration as a breakdown of days, hours, minutes, and seconds.
     * Only non-zero units are shown (e.g. "1d 2h 30m 45s" or "45m 12s").
     *
     * @param totalSeconds Duration in seconds.
     * @return Human-readable breakdown like "1d 2h 30m 45s" or "0s".
     */
    fun formatDurationBreakdownSeconds(totalSeconds: Long): String {
        val seconds = totalSeconds.coerceAtLeast(0L)
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        val parts = mutableListOf<String>()
        if (days > 0) parts.add("${days}d")
        if (hours > 0) parts.add("${hours}h")
        if (minutes > 0) parts.add("${minutes}m")
        parts.add("${secs}s")
        return parts.joinToString(" ")
    }

    /**
     * Formats duration in milliseconds as a breakdown of days, hours, minutes, and seconds.
     * When [capAt24h] is true, durations above [MAX_REASONABLE_DURATION_MS] return [over24hPlaceholder].
     *
     * @param durationMs Duration in milliseconds.
     * @param over24hPlaceholder Shown when duration exceeds 24h (for localization).
     * @param capAt24h When true, cap display at 24h for completed rides; when false, show full duration (e.g. live ticker).
     * @return Human-readable breakdown like "1d 2h 30m 45s".
     */
    fun formatDurationBreakdownMs(
        durationMs: Long,
        over24hPlaceholder: String = ">24h",
        capAt24h: Boolean = true,
    ): String {
        if (capAt24h && durationMs > MAX_REASONABLE_DURATION_MS) return over24hPlaceholder
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0L)
        return formatDurationBreakdownSeconds(totalSeconds)
    }

    /**
     * Computes elapsed duration from start time (epoch ms) to now.
     * Used for live ticker on active ride screen.
     * @param startTimeMs Start timestamp in epoch milliseconds.
     * @return Formatted string as days/hours/min/sec breakdown.
     */
    fun formatElapsedFromStart(startTimeMs: Long): String {
        val elapsedMs = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(0L)
        return formatDurationBreakdownMs(elapsedMs, capAt24h = false)
    }

    /**
     * Parses a duration string (e.g. "1:30:00" or "0:45:30") into total seconds.
     * Accepts H:MM:SS, HH:MM:SS, M:SS, or plain seconds.
     * @return Parsed seconds, or null if the string is invalid.
     */
    fun parseDurationToSeconds(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val parts = trimmed.split(":").map { it.trim() }
        return when (parts.size) {
            1 -> parts[0].toLongOrNull()?.coerceAtLeast(0)
            2 -> {
                val minutes = parts[0].toLongOrNull() ?: return null
                val seconds = parts[1].toLongOrNull() ?: return null
                if (minutes < 0 || seconds < 0) null else (minutes * 60 + seconds).coerceAtLeast(0)
            }
            3 -> {
                val hours = parts[0].toLongOrNull() ?: return null
                val minutes = parts[1].toLongOrNull() ?: return null
                val seconds = parts[2].toLongOrNull() ?: return null
                if (hours < 0 || minutes < 0 || seconds < 0) null
                else (hours * 3600 + minutes * 60 + seconds).coerceAtLeast(0)
            }
            else -> null
        }
    }
}
