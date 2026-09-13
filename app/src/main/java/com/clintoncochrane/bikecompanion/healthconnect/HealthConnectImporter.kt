package com.clintoncochrane.bikecompanion.healthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

/**
 * Reads cycling sessions from Health Connect and maps them to ride data.
 * Availability and permission failures are returned as explicit results.
 */
class HealthConnectImporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Reads cycling sessions from the last 30 days.
     */
    suspend fun readCyclingSessions(): HealthConnectReadResult = withContext(Dispatchers.IO) {
        val client by lazy(LazyThreadSafetyMode.NONE) { HealthConnectClient.getOrCreate(context) }
        readHealthConnectSessions(
            sdkStatus = { HealthConnectClient.getSdkStatus(context) },
            requiredPermissions = HEALTH_CONNECT_READ_PERMISSIONS,
            grantedPermissions = { client.permissionController.getGrantedPermissions() },
            readSessions = {
                val endTime = Instant.now()
                val startTime = endTime.minusSeconds(30L * 24 * 3600)
                val request = ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
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
            },
            logFailure = { message -> Log.e(TAG, message) },
        )
    }

    private companion object {
        const val TAG = "HealthConnectImporter"
    }
}

val HEALTH_CONNECT_READ_PERMISSIONS: Set<String> = setOf(
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
)

sealed interface HealthConnectReadResult {
    data object Unavailable : HealthConnectReadResult
    data object ProviderUpdateRequired : HealthConnectReadResult
    data object PermissionRequired : HealthConnectReadResult
    data class Success(val sessions: List<HealthConnectSession>) : HealthConnectReadResult
    data object Failure : HealthConnectReadResult
}

internal suspend fun readHealthConnectSessions(
    sdkStatus: () -> Int,
    requiredPermissions: Set<String>,
    grantedPermissions: suspend () -> Set<String>,
    readSessions: suspend () -> List<HealthConnectSession>,
    logFailure: (String) -> Unit,
): HealthConnectReadResult = try {
    when (sdkStatus()) {
        HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectReadResult.Unavailable
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
            HealthConnectReadResult.ProviderUpdateRequired
        HealthConnectClient.SDK_AVAILABLE -> {
            if (!grantedPermissions().containsAll(requiredPermissions)) {
                HealthConnectReadResult.PermissionRequired
            } else {
                HealthConnectReadResult.Success(readSessions())
            }
        }
        else -> {
            logFailure("Health Connect availability check returned an unknown status")
            HealthConnectReadResult.Failure
        }
    }
} catch (exception: CancellationException) {
    throw exception
} catch (exception: Exception) {
    logFailure("Health Connect query failed (${exception.javaClass.simpleName})")
    HealthConnectReadResult.Failure
}

data class HealthConnectSession(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val distanceKm: Double,
)
