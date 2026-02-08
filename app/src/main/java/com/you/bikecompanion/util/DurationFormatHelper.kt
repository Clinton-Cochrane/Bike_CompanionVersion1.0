package com.you.bikecompanion.util

/**
 * Formats duration for display in UI.
 * Uses epoch timestamps internally to avoid timezone issues.
 */
object DurationFormatHelper {

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
     * Computes elapsed duration from start time (epoch ms) to now.
     * Used for live ticker on active ride screen.
     * @param startTimeMs Start timestamp in epoch milliseconds.
     * @return Formatted string HH:MM:SS.
     */
    fun formatElapsedFromStart(startTimeMs: Long): String {
        val elapsedMs = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(0L)
        return formatDurationMs(elapsedMs)
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
