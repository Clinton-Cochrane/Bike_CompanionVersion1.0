package com.clintoncochrane.bikecompanion.data.ride

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalEntity
import com.clintoncochrane.bikecompanion.notifications.ComponentAlertNotifier
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RideRepositoryEditTransactionTest {

    private lateinit var database: BikeCompanionDatabase
    private lateinit var repository: RideRepository
    private var bikeId = 0L
    private var componentId = 0L
    private var intervalId = 0L
    private lateinit var originalRide: RideEntity

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BikeCompanionDatabase::class.java).build()
        repository = RideRepository(
            database.rideDao(),
            database.bikeDao(),
            database.componentDao(),
            database.serviceIntervalDao(),
            ComponentAlertNotifier(context, database.componentDao()),
            RidePersistenceTransaction(database),
        )

        bikeId = database.bikeDao().insert(
            BikeEntity(name = "Edit Test Bike", totalDistanceKm = 10.0, totalTimeSeconds = 600L, createdAt = 1L),
        )
        componentId = database.componentDao().insert(
            ComponentEntity(
                bikeId = bikeId,
                type = "chain",
                name = "Edit Test Chain",
                lifespanKm = 5_000.0,
                distanceUsedKm = 10.0,
                totalTimeSeconds = 600L,
                installedAt = 1L,
            ),
        )
        intervalId = database.serviceIntervalDao().insert(
            ServiceIntervalEntity(
                componentId = componentId,
                name = "Inspect",
                intervalKm = 1_000.0,
                trackedKm = 10.0,
                intervalTimeSeconds = 7_200L,
                trackedTimeSeconds = 600L,
            ),
        )
        database.rideDao().insert(
            RideEntity(
                bikeId = bikeId,
                distanceKm = 10.0,
                durationMs = 600_000L,
                maxSpeedKmh = 12.0,
                elevGainM = 40.0,
                elevLossM = 30.0,
                startedAt = 1L,
                endedAt = 600_001L,
            ),
        )
        val originalRideId = database.rideDao().insert(
            RideEntity(
                bikeId = bikeId,
                distanceKm = 5.0,
                durationMs = 3_600_000L,
                maxSpeedKmh = 30.0,
                elevGainM = 100.0,
                elevLossM = 90.0,
                startedAt = 700_000L,
                endedAt = 4_300_000L,
            ),
        )
        originalRide = requireNotNull(database.rideDao().getRideById(originalRideId))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun updateCompletedRideAndReconcileAggregates_increasingThenDecreasingValues_matchesEditedHistory() = runBlocking {
        val increasedRide = originalRide.copy(
            distanceKm = 8.0,
            durationMs = 7_200_000L,
            avgSpeedKmh = 4.0,
            maxSpeedKmh = 22.0,
            elevGainM = 120.0,
            elevLossM = 80.0,
            endedAt = 7_900_000L,
        )

        repository.updateCompletedRideAndReconcileAggregates(originalRide, increasedRide)

        assertAggregateState(
            expectedDistanceKm = 18.0,
            expectedTimeSeconds = 7_800L,
            expectedMaxSpeedKmh = 22.0,
            expectedElevGainM = 160.0,
            expectedElevLossM = 110.0,
            expectedLastRideAt = 7_900_000L,
        )

        val decreasedRide = increasedRide.copy(
            distanceKm = 2.0,
            durationMs = 1_800_000L,
            avgSpeedKmh = 4.0,
            maxSpeedKmh = 10.0,
            elevGainM = 20.0,
            elevLossM = 10.0,
        )

        repository.updateCompletedRideAndReconcileAggregates(increasedRide, decreasedRide)
        repository.updateCompletedRideAndReconcileAggregates(decreasedRide, decreasedRide)

        assertAggregateState(
            expectedDistanceKm = 12.0,
            expectedTimeSeconds = 2_400L,
            expectedMaxSpeedKmh = 12.0,
            expectedElevGainM = 60.0,
            expectedElevLossM = 40.0,
            expectedLastRideAt = 7_900_000L,
        )
        assertEquals(decreasedRide, database.rideDao().getRideById(decreasedRide.id))
    }

    @Test
    fun updateCompletedRideAndReconcileAggregates_invalidReplacement_doesNotWrite() = runBlocking {
        val invalidRides = listOf(
            originalRide.copy(distanceKm = -1.0),
            originalRide.copy(distanceKm = Double.NaN),
            originalRide.copy(maxSpeedKmh = Double.POSITIVE_INFINITY),
            originalRide.copy(durationMs = -1L),
        )

        invalidRides.forEach { invalidRide ->
            val result = runCatching {
                repository.updateCompletedRideAndReconcileAggregates(originalRide, invalidRide)
            }

            assertTrue(result.isFailure)
            assertEquals(originalRide, database.rideDao().getRideById(originalRide.id))
            assertAggregateState(15.0, 4_200L, 30.0, 140.0, 120.0, 4_300_000L)
        }
    }

    @Test
    fun updateCompletedRideAndReconcileAggregates_failedIntervalUpdate_rollsBackEveryWrite() = runBlocking {
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER force_service_interval_update_failure
            BEFORE UPDATE ON service_intervals
            BEGIN
                SELECT RAISE(ABORT, 'forced service interval update failure');
            END
            """.trimIndent(),
        )

        val result = runCatching {
            repository.updateCompletedRideAndReconcileAggregates(
                originalRide,
                originalRide.copy(distanceKm = 8.0, durationMs = 7_200_000L),
            )
        }

        assertTrue(result.isFailure)
        assertEquals(originalRide, database.rideDao().getRideById(originalRide.id))
        assertAggregateState(15.0, 4_200L, 30.0, 140.0, 120.0, 4_300_000L)
    }

    private suspend fun assertAggregateState(
        expectedDistanceKm: Double,
        expectedTimeSeconds: Long,
        expectedMaxSpeedKmh: Double,
        expectedElevGainM: Double,
        expectedElevLossM: Double,
        expectedLastRideAt: Long,
    ) {
        val bike = requireNotNull(database.bikeDao().getBikeById(bikeId))
        val component = requireNotNull(database.componentDao().getComponentById(componentId))
        val interval = database.serviceIntervalDao().getIntervalsByComponentIdOnce(componentId)
            .single { it.id == intervalId }
        val expectedAverage = expectedDistanceKm / (expectedTimeSeconds / 3600.0)

        assertEquals(expectedDistanceKm, bike.totalDistanceKm, 0.0)
        assertEquals(expectedTimeSeconds, bike.totalTimeSeconds)
        assertEquals(expectedAverage, bike.avgSpeedKmh, 1e-9)
        assertEquals(expectedMaxSpeedKmh, bike.maxSpeedKmh, 0.0)
        assertEquals(expectedElevGainM, bike.totalElevGainM, 0.0)
        assertEquals(expectedElevLossM, bike.totalElevLossM, 0.0)
        assertEquals(expectedLastRideAt, bike.lastRideAt)
        assertEquals(expectedDistanceKm, component.distanceUsedKm, 0.0)
        assertEquals(expectedTimeSeconds, component.totalTimeSeconds)
        assertEquals(expectedAverage, component.avgSpeedKmh, 1e-9)
        assertEquals(expectedMaxSpeedKmh, component.maxSpeedKmh, 0.0)
        assertEquals(bikeId, component.maxSpeedBikeId)
        assertEquals(expectedDistanceKm, interval.trackedKm, 0.0)
        assertEquals(expectedTimeSeconds, interval.trackedTimeSeconds)
    }
}
