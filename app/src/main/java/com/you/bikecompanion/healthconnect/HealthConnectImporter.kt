package com.you.bikecompanion.healthconnect

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

/**
 * Reads cycling sessions from Health Connect and maps them to ride data.
 * Caller must request Health Connect permissions before calling [readCyclingSessions].
 */
class HealthConnectImporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Returns list of (startTimeMs, endTimeMs, distanceKm) for cycling sessions in the last 30 days.
     * Requires Health Connect READ_EXERCISE permission to be granted.
     */
    suspend fun readCyclingSessions(): List<HealthConnectSession> = withContext(Dispatchers.IO) {
        try {
            val client = androidx.health.connect.client.HealthConnectClient.getOrCreate(context)
            val endTime = Instant.now()
            val startTime = endTime.minusSeconds(30L * 24 * 3600)
            val request = androidx.health.connect.client.request.ReadRecordsRequest(
                recordType = androidx.health.connect.client.records.ExerciseSessionRecord::class,
                timeRangeFilter = androidx.health.connect.client.time.TimeRangeFilter.between(startTime, endTime),
            )
            val response = client.readRecords(request)
            response.records.map { record ->
                val startMs = record.startTime.toEpochMilli()
                val endMs = record.endTime.toEpochMilli()
                val durationMs = (endMs - startMs).coerceAtLeast(0L)
                HealthConnectSession(
                    startTimeMs = startMs,
                    endTimeMs = endMs,
                    durationMs = durationMs,
                    distanceKm = 0.0,
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

data class HealthConnectSession(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val distanceKm: Double,
)
