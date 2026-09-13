package com.clintoncochrane.bikecompanion.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class HealthConnectSessionMapperTest {

    @Test
    fun mapCyclingSessions_distancePresent_mapsKilometers() = runTest {
        val session = bikingSession(startTimeMs = 1_000L, endTimeMs = 3_601_000L)

        val result = HealthConnectSessionMapper.mapCyclingSessions(listOf(session)) {
            Length.meters(15_500.0)
        }

        assertEquals(15.5, result.single().distanceKm!!, 0.0)
    }

    @Test
    fun mapCyclingSessions_recordIdPresent_mapsStableRecordId() = runTest {
        val session = exerciseSession(
            startTimeMs = 1_000L,
            endTimeMs = 3_601_000L,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
            recordId = "health-connect-session-1",
        )

        val result = HealthConnectSessionMapper.mapCyclingSessions(listOf(session)) { Length.kilometers(10.0) }

        assertEquals("health-connect-session-1", result.single().healthConnectRecordId)
    }

    @Test
    fun mapCyclingSessions_distanceUnavailable_preservesUnavailableState() = runTest {
        val session = bikingSession(startTimeMs = 1_000L, endTimeMs = 3_601_000L)

        val result = HealthConnectSessionMapper.mapCyclingSessions(listOf(session)) { null }

        assertNull(result.single().distanceKm)
    }

    @Test
    fun mapCyclingSessions_zeroDistance_preservesNumericZero() = runTest {
        val session = bikingSession(startTimeMs = 1_000L, endTimeMs = 3_601_000L)

        val result = HealthConnectSessionMapper.mapCyclingSessions(listOf(session)) {
            Length.meters(0.0)
        }

        assertEquals(0.0, result.single().distanceKm!!, 0.0)
    }

    @Test
    fun mapCyclingSessions_multipleSessions_keepsEachDistanceAssociated() = runTest {
        val first = bikingSession(startTimeMs = 1_000L, endTimeMs = 3_601_000L)
        val second = bikingSession(startTimeMs = 5_000_000L, endTimeMs = 8_600_000L)

        val result = HealthConnectSessionMapper.mapCyclingSessions(listOf(first, second)) { session ->
            when (session.startTime.toEpochMilli()) {
                1_000L -> Length.kilometers(12.25)
                5_000_000L -> Length.kilometers(23.5)
                else -> null
            }
        }

        assertEquals(listOf(12.25, 23.5), result.map { it.distanceKm })
    }

    @Test
    fun mapCyclingSessions_unsupportedExercise_doesNotMapSession() = runTest {
        val runningSession = exerciseSession(
            startTimeMs = 1_000L,
            endTimeMs = 3_601_000L,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        )

        val result = HealthConnectSessionMapper.mapCyclingSessions(listOf(runningSession)) {
            Length.kilometers(10.0)
        }

        assertEquals(emptyList<HealthConnectSession>(), result)
    }

    private fun bikingSession(startTimeMs: Long, endTimeMs: Long): ExerciseSessionRecord =
        exerciseSession(
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        )

    private fun exerciseSession(
        startTimeMs: Long,
        endTimeMs: Long,
        exerciseType: Int,
        recordId: String? = null,
    ) = ExerciseSessionRecord(
        startTime = Instant.ofEpochMilli(startTimeMs),
        startZoneOffset = null,
        endTime = Instant.ofEpochMilli(endTimeMs),
        endZoneOffset = null,
        metadata = recordId?.let { Metadata.manualEntryWithId(it) } ?: Metadata.manualEntry(),
        exerciseType = exerciseType,
    )
}
