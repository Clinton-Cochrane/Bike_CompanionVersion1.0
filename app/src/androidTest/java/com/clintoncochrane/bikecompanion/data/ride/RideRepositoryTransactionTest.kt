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
class RideRepositoryTransactionTest {

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
            BikeEntity(
                name = "Transaction Test Bike",
                totalDistanceKm = 10.0,
                totalTimeSeconds = 600L,
                createdAt = 1L,
            ),
        )
        componentId = database.componentDao().insert(
            ComponentEntity(
                bikeId = bikeId,
                type = "chain",
                name = "Transaction Test Chain",
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
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveRideAndUpdateBikeAndComponents_success_commitsAllUpdatesExactlyOnce() = runBlocking {
        repository.saveRideAndUpdateBikeAndComponents(testRide())

        val rides = database.rideDao().getAllRides().first()
        val bike = requireNotNull(database.bikeDao().getBikeById(bikeId))
        val component = requireNotNull(database.componentDao().getComponentById(componentId))
        val interval = database.serviceIntervalDao()
            .getIntervalsByComponentIdOnce(componentId)
            .single { it.id == intervalId }

        assertEquals(1, rides.size)
        assertEquals(15.0, bike.totalDistanceKm, 0.0)
        assertEquals(4_200L, bike.totalTimeSeconds)
        assertEquals(3_601_000L, bike.lastRideAt)
        val expectedAvgSpeed = 15.0 / (4_200.0 / 3600.0)
        assertEquals(expectedAvgSpeed, bike.avgSpeedKmh, 1e-9)
        assertEquals(30.0, bike.maxSpeedKmh, 0.0)
        assertEquals(100.0, bike.totalElevGainM, 0.0)
        assertEquals(90.0, bike.totalElevLossM, 0.0)
        assertEquals(15.0, component.distanceUsedKm, 0.0)
        assertEquals(4_200L, component.totalTimeSeconds)
        assertEquals(expectedAvgSpeed, component.avgSpeedKmh, 1e-9)
        assertEquals(30.0, component.maxSpeedKmh, 0.0)
        assertEquals(bikeId, component.maxSpeedBikeId)
        assertEquals(15.0, interval.trackedKm, 0.0)
        assertEquals(4_200L, interval.trackedTimeSeconds)
    }

    @Test
    fun saveManualRide_createsHistoryAndAccountsDistanceExactlyOnce() = runBlocking {
        repository.saveManualRide(bikeId = bikeId, distanceKm = 5.0, occurredAt = 2_000L)

        val ride = database.rideDao().getAllRides().first().single()
        val bike = requireNotNull(database.bikeDao().getBikeById(bikeId))
        val component = requireNotNull(database.componentDao().getComponentById(componentId))
        val interval = database.serviceIntervalDao()
            .getIntervalsByComponentIdOnce(componentId)
            .single { it.id == intervalId }

        assertEquals(RideSource.MANUAL, ride.source)
        assertEquals(bikeId, ride.bikeId)
        assertEquals(5.0, ride.distanceKm, 0.0)
        assertEquals(0L, ride.durationMs)
        assertEquals(2_000L, ride.startedAt)
        assertEquals(2_000L, ride.endedAt)
        assertEquals(15.0, bike.totalDistanceKm, 0.0)
        assertEquals(600L, bike.totalTimeSeconds)
        assertEquals(15.0, component.distanceUsedKm, 0.0)
        assertEquals(600L, component.totalTimeSeconds)
        assertEquals(15.0, interval.trackedKm, 0.0)
        assertEquals(600L, interval.trackedTimeSeconds)
    }

    @Test
    fun saveRideAndUpdateBikeAndComponents_serviceIntervalUpdateFails_rollsBackEveryUpdate() = runBlocking {
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
            repository.saveRideAndUpdateBikeAndComponents(testRide())
        }

        assertTrue("The forced service-interval failure must escape the save operation", result.isFailure)
        val rides = database.rideDao().getAllRides().first()
        val bike = requireNotNull(database.bikeDao().getBikeById(bikeId))
        val component = requireNotNull(database.componentDao().getComponentById(componentId))
        val interval = database.serviceIntervalDao()
            .getIntervalsByComponentIdOnce(componentId)
            .single { it.id == intervalId }

        assertTrue(rides.isEmpty())
        assertEquals(10.0, bike.totalDistanceKm, 0.0)
        assertEquals(600L, bike.totalTimeSeconds)
        assertEquals(10.0, component.distanceUsedKm, 0.0)
        assertEquals(600L, component.totalTimeSeconds)
        assertEquals(10.0, interval.trackedKm, 0.0)
        assertEquals(600L, interval.trackedTimeSeconds)
    }

    @Test
    fun saveRideAndUpdateBikeAndComponents_bikeUpdateFails_rollsBackEveryUpdate() = runBlocking {
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER force_bike_update_failure
            BEFORE UPDATE ON bikes
            BEGIN
                SELECT RAISE(ABORT, 'forced bike update failure');
            END
            """.trimIndent(),
        )

        val result = runCatching {
            repository.saveRideAndUpdateBikeAndComponents(testRide())
        }

        assertTrue("The forced bike-update failure must escape the save operation", result.isFailure)
        val rides = database.rideDao().getAllRides().first()
        val bike = requireNotNull(database.bikeDao().getBikeById(bikeId))
        val component = requireNotNull(database.componentDao().getComponentById(componentId))
        val interval = database.serviceIntervalDao()
            .getIntervalsByComponentIdOnce(componentId)
            .single { it.id == intervalId }

        assertTrue(rides.isEmpty())
        assertEquals(10.0, bike.totalDistanceKm, 0.0)
        assertEquals(600L, bike.totalTimeSeconds)
        assertEquals(10.0, component.distanceUsedKm, 0.0)
        assertEquals(600L, component.totalTimeSeconds)
        assertEquals(10.0, interval.trackedKm, 0.0)
        assertEquals(600L, interval.trackedTimeSeconds)
    }

    private fun testRide() = RideEntity(
        bikeId = bikeId,
        distanceKm = 5.0,
        durationMs = 3_600_000L,
        maxSpeedKmh = 30.0,
        elevGainM = 100.0,
        elevLossM = 90.0,
        startedAt = 1_000L,
        endedAt = 3_601_000L,
    )
}
