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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RideRepositoryDeletionTest {

    private lateinit var database: BikeCompanionDatabase
    private lateinit var repository: RideRepository
    private var bikeId = 0L
    private var componentId = 0L
    private var intervalId = 0L

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BikeCompanionDatabase::class.java).build()
        repository = RideRepository(
            database.rideDao(),
            database.bikeDao(),
            database.componentDao(),
            database.serviceIntervalDao(),
            database.componentSwapDao(),
            ComponentAlertNotifier(context, database.componentDao()),
            RidePersistenceTransaction(database),
        )
        bikeId = database.bikeDao().insert(
            BikeEntity(name = "Deletion Test Bike", baselineDistanceKm = 100.0, totalDistanceKm = 100.0, createdAt = 0L),
        )
        componentId = database.componentDao().insert(
            ComponentEntity(bikeId = bikeId, type = "chain", name = "Chain", lifespanKm = 5_000.0, installedAt = 0L),
        )
        intervalId = database.serviceIntervalDao().insert(
            ServiceIntervalEntity(componentId = componentId, name = "Inspect", intervalKm = 1_000.0, intervalTimeSeconds = 10_000L, trackedTimeSeconds = 0L),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun deleteRide_newestRide_rebuildsTotalsFromOlderHistory() = runBlocking {
        repository.saveRideAndUpdateBikeAndComponents(ride(distanceKm = 10.0, durationSeconds = 3_600L, maxSpeedKmh = 20.0, endedAt = 10_000L))
        repository.saveRideAndUpdateBikeAndComponents(ride(distanceKm = 20.0, durationSeconds = 1_800L, maxSpeedKmh = 40.0, endedAt = 20_000L))

        repository.deleteRide(rideEndingAt(20_000L))

        assertAggregates(distanceKm = 110.0, timeSeconds = 3_600L, lastRideAt = 10_000L, avgSpeedKmh = 10.0, maxSpeedKmh = 20.0)
    }

    @Test
    fun deleteRide_olderRide_preservesNewestRideDerivedValues() = runBlocking {
        repository.saveRideAndUpdateBikeAndComponents(ride(distanceKm = 10.0, durationSeconds = 3_600L, maxSpeedKmh = 40.0, endedAt = 10_000L))
        repository.saveRideAndUpdateBikeAndComponents(ride(distanceKm = 20.0, durationSeconds = 1_800L, maxSpeedKmh = 20.0, endedAt = 20_000L))

        repository.deleteRide(rideEndingAt(10_000L))

        assertAggregates(distanceKm = 120.0, timeSeconds = 1_800L, lastRideAt = 20_000L, avgSpeedKmh = 40.0, maxSpeedKmh = 20.0)
    }

    @Test
    fun deleteRide_onlyRide_restoresBaselineAndNeverProducesNegativeTotals() = runBlocking {
        repository.saveRideAndUpdateBikeAndComponents(ride(distanceKm = 10.0, durationSeconds = 3_600L, maxSpeedKmh = 40.0, endedAt = 10_000L))

        repository.deleteRide(rideEndingAt(10_000L))
        repository.deleteRide(rideEndingAt(10_000L))

        assertAggregates(distanceKm = 100.0, timeSeconds = 0L, lastRideAt = null, avgSpeedKmh = 0.0, maxSpeedKmh = 0.0)
    }

    @Test
    fun deleteRide_componentInstalledAfterRide_doesNotChangeItsUsage() = runBlocking {
        repository.saveRideAndUpdateBikeAndComponents(ride(distanceKm = 10.0, durationSeconds = 3_600L, maxSpeedKmh = 40.0, endedAt = 10_000L))
        val laterComponentId = database.componentDao().insert(
            ComponentEntity(
                bikeId = bikeId,
                type = "tire",
                name = "Later Tire",
                lifespanKm = 5_000.0,
                distanceUsedKm = 7.0,
                totalTimeSeconds = 500L,
                installedAt = 20_000L,
            ),
        )
        val laterIntervalId = database.serviceIntervalDao().insert(
            ServiceIntervalEntity(componentId = laterComponentId, name = "Inspect", intervalKm = 1_000.0, trackedKm = 7.0),
        )

        repository.deleteRide(rideEndingAt(10_000L))

        val laterComponent = requireNotNull(database.componentDao().getComponentById(laterComponentId))
        val laterInterval = database.serviceIntervalDao().getIntervalsByComponentIdOnce(laterComponentId).single { it.id == laterIntervalId }
        assertEquals(7.0, laterComponent.distanceUsedKm, 0.0)
        assertEquals(500L, laterComponent.totalTimeSeconds)
        assertEquals(7.0, laterInterval.trackedKm, 0.0)
    }

    @Test
    fun deleteRide_componentUpdateFails_rollsBackRideAndEveryAggregate() = runBlocking {
        repository.saveRideAndUpdateBikeAndComponents(ride(distanceKm = 10.0, durationSeconds = 3_600L, maxSpeedKmh = 40.0, endedAt = 10_000L))
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER force_component_update_failure
            BEFORE UPDATE ON components
            BEGIN
                SELECT RAISE(ABORT, 'forced component update failure');
            END
            """.trimIndent(),
        )

        val result = runCatching { repository.deleteRide(rideEndingAt(10_000L)) }

        assertTrue(result.isFailure)
        assertEquals(1, database.rideDao().getAllRides().first().size)
        assertAggregates(distanceKm = 110.0, timeSeconds = 3_600L, lastRideAt = 10_000L, avgSpeedKmh = 10.0, maxSpeedKmh = 40.0)
    }

    private suspend fun rideEndingAt(endedAt: Long): RideEntity = database.rideDao().getAllRides().first().single { it.endedAt == endedAt }

    private fun ride(distanceKm: Double, durationSeconds: Long, maxSpeedKmh: Double, endedAt: Long) = RideEntity(
        bikeId = bikeId,
        distanceKm = distanceKm,
        durationMs = durationSeconds * 1_000L,
        maxSpeedKmh = maxSpeedKmh,
        elevGainM = distanceKm,
        elevLossM = distanceKm / 2,
        startedAt = endedAt - durationSeconds * 1_000L,
        endedAt = endedAt,
    )

    private suspend fun assertAggregates(
        distanceKm: Double,
        timeSeconds: Long,
        lastRideAt: Long?,
        avgSpeedKmh: Double,
        maxSpeedKmh: Double,
    ) {
        val bike = requireNotNull(database.bikeDao().getBikeById(bikeId))
        val component = requireNotNull(database.componentDao().getComponentById(componentId))
        val interval = database.serviceIntervalDao().getIntervalsByComponentIdOnce(componentId).single { it.id == intervalId }
        assertEquals(distanceKm, bike.totalDistanceKm, 0.0)
        assertEquals(timeSeconds, bike.totalTimeSeconds)
        assertEquals(lastRideAt, bike.lastRideAt)
        assertEquals(avgSpeedKmh, bike.avgSpeedKmh, 1e-9)
        assertEquals(maxSpeedKmh, bike.maxSpeedKmh, 0.0)
        assertEquals(distanceKm - 100.0, bike.totalElevGainM, 0.0)
        assertEquals((distanceKm - 100.0) / 2, bike.totalElevLossM, 0.0)
        assertEquals(distanceKm - 100.0, component.distanceUsedKm, 0.0)
        assertEquals(timeSeconds, component.totalTimeSeconds)
        assertEquals(avgSpeedKmh, component.avgSpeedKmh, 1e-9)
        assertEquals(maxSpeedKmh, component.maxSpeedKmh, 0.0)
        assertEquals(distanceKm - 100.0, interval.trackedKm, 0.0)
        assertEquals(timeSeconds, interval.trackedTimeSeconds)
    }
}
