package com.clintoncochrane.bikecompanion.data.ride

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentSwapEntity
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
class RideRepositoryReassignmentTest {

    private lateinit var database: BikeCompanionDatabase
    private lateinit var repository: RideRepository
    private var bikeAId = 0L
    private var bikeBId = 0L
    private var componentAId = 0L
    private var componentBId = 0L

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
        bikeAId = database.bikeDao().insert(
            BikeEntity(name = "Bike A", baselineDistanceKm = 100.0, totalDistanceKm = 100.0, createdAt = 0L),
        )
        bikeBId = database.bikeDao().insert(
            BikeEntity(name = "Bike B", baselineDistanceKm = 200.0, totalDistanceKm = 200.0, createdAt = 0L),
        )
        componentAId = addComponent(bikeAId, "Chain A")
        componentBId = addComponent(bikeBId, "Chain B")
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun reassignCompletedRide_bikeAToBikeB_movesBikeComponentAndServiceTotalsExactlyOnce() = runBlocking {
        val rideId = saveRide(bikeAId, distanceKm = 10.0, endedAt = 10_000L)
        val componentA = requireNotNull(database.componentDao().getComponentById(componentAId))
        database.componentDao().update(componentA.copy(bikeId = null))
        val componentASwap = requireNotNull(database.componentSwapDao().getCurrentSwap(componentAId))
        database.componentSwapDao().update(componentASwap.copy(uninstalledAt = 20_000L))

        repository.reassignCompletedRide(rideId, bikeBId)

        assertBike(bikeAId, totalDistanceKm = 100.0, totalTimeSeconds = 0L, lastRideAt = null)
        assertBike(bikeBId, totalDistanceKm = 210.0, totalTimeSeconds = 3_600L, lastRideAt = 10_000L)
        assertComponentAndInterval(componentAId, distanceKm = 0.0, totalTimeSeconds = 0L)
        assertComponentAndInterval(componentBId, distanceKm = 10.0, totalTimeSeconds = 3_600L)
        assertEquals(bikeBId, requireNotNull(database.rideDao().getRideById(rideId)).bikeId)
    }

    @Test
    fun reassignCompletedRide_repeatedReassignment_doesNotDoubleCount() = runBlocking {
        val rideId = saveRide(bikeAId, distanceKm = 10.0, endedAt = 10_000L)

        repository.reassignCompletedRide(rideId, bikeBId)
        repository.reassignCompletedRide(rideId, bikeAId)
        repository.reassignCompletedRide(rideId, bikeBId)

        assertBike(bikeAId, totalDistanceKm = 100.0, totalTimeSeconds = 0L, lastRideAt = null)
        assertBike(bikeBId, totalDistanceKm = 210.0, totalTimeSeconds = 3_600L, lastRideAt = 10_000L)
        assertComponentAndInterval(componentAId, distanceKm = 0.0, totalTimeSeconds = 0L)
        assertComponentAndInterval(componentBId, distanceKm = 10.0, totalTimeSeconds = 3_600L)
    }

    @Test
    fun reassignCompletedRide_sameBike_isANoOp() = runBlocking {
        val rideId = saveRide(bikeAId, distanceKm = 10.0, endedAt = 10_000L)

        repository.reassignCompletedRide(rideId, bikeAId)

        assertBike(bikeAId, totalDistanceKm = 110.0, totalTimeSeconds = 3_600L, lastRideAt = 10_000L)
        assertBike(bikeBId, totalDistanceKm = 200.0, totalTimeSeconds = 0L, lastRideAt = null)
        assertComponentAndInterval(componentAId, distanceKm = 10.0, totalTimeSeconds = 3_600L)
        assertComponentAndInterval(componentBId, distanceKm = 0.0, totalTimeSeconds = 0L)
    }

    @Test
    fun reassignCompletedRide_olderRide_rebuildsBothBikesFromTheirRideHistory() = runBlocking {
        val olderRideId = saveRide(bikeAId, distanceKm = 10.0, endedAt = 10_000L)
        saveRide(bikeAId, distanceKm = 20.0, endedAt = 20_000L)

        repository.reassignCompletedRide(olderRideId, bikeBId)

        assertBike(bikeAId, totalDistanceKm = 120.0, totalTimeSeconds = 3_600L, lastRideAt = 20_000L)
        assertBike(bikeBId, totalDistanceKm = 210.0, totalTimeSeconds = 3_600L, lastRideAt = 10_000L)
        assertComponentAndInterval(componentAId, distanceKm = 20.0, totalTimeSeconds = 3_600L)
        assertComponentAndInterval(componentBId, distanceKm = 10.0, totalTimeSeconds = 3_600L)
    }

    @Test
    fun reassignCompletedRide_newestRide_preservesOldBikeLastRideFromOlderHistory() = runBlocking {
        saveRide(bikeAId, distanceKm = 10.0, endedAt = 10_000L)
        val newestRideId = saveRide(bikeAId, distanceKm = 20.0, endedAt = 20_000L)

        repository.reassignCompletedRide(newestRideId, bikeBId)

        assertBike(bikeAId, totalDistanceKm = 110.0, totalTimeSeconds = 3_600L, lastRideAt = 10_000L)
        assertBike(bikeBId, totalDistanceKm = 220.0, totalTimeSeconds = 3_600L, lastRideAt = 20_000L)
        assertComponentAndInterval(componentAId, distanceKm = 10.0, totalTimeSeconds = 3_600L)
        assertComponentAndInterval(componentBId, distanceKm = 20.0, totalTimeSeconds = 3_600L)
    }

    @Test
    fun reassignCompletedRide_componentUpdateFails_rollsBackRideAndBothBikeTotals() = runBlocking {
        val rideId = saveRide(bikeAId, distanceKm = 10.0, endedAt = 10_000L)
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER force_component_update_failure
            BEFORE UPDATE ON components
            BEGIN
                SELECT RAISE(ABORT, 'forced component update failure');
            END
            """.trimIndent(),
        )

        val result = runCatching { repository.reassignCompletedRide(rideId, bikeBId) }

        assertTrue(result.isFailure)
        assertEquals(bikeAId, requireNotNull(database.rideDao().getRideById(rideId)).bikeId)
        assertBike(bikeAId, totalDistanceKm = 110.0, totalTimeSeconds = 3_600L, lastRideAt = 10_000L)
        assertBike(bikeBId, totalDistanceKm = 200.0, totalTimeSeconds = 0L, lastRideAt = null)
        assertComponentAndInterval(componentAId, distanceKm = 10.0, totalTimeSeconds = 3_600L)
        assertComponentAndInterval(componentBId, distanceKm = 0.0, totalTimeSeconds = 0L)
    }

    private suspend fun addComponent(bikeId: Long, name: String): Long {
        val componentId = database.componentDao().insert(
            ComponentEntity(bikeId = bikeId, type = "chain", name = name, lifespanKm = 5_000.0, installedAt = 0L),
        )
        database.componentSwapDao().insert(
            ComponentSwapEntity(componentId = componentId, bikeId = bikeId, installedAt = 0L),
        )
        database.serviceIntervalDao().insert(
            ServiceIntervalEntity(
                componentId = componentId,
                name = "Inspect",
                intervalKm = 1_000.0,
                intervalTimeSeconds = 10_000L,
                trackedTimeSeconds = 0L,
            ),
        )
        return componentId
    }

    private suspend fun saveRide(bikeId: Long, distanceKm: Double, endedAt: Long): Long {
        repository.saveRideAndUpdateBikeAndComponents(
            RideEntity(
                bikeId = bikeId,
                distanceKm = distanceKm,
                durationMs = 3_600_000L,
                maxSpeedKmh = distanceKm * 2,
                elevGainM = distanceKm,
                elevLossM = distanceKm / 2,
                startedAt = endedAt - 3_600_000L,
                endedAt = endedAt,
            ),
        )
        return database.rideDao().getAllRides().first().single { it.endedAt == endedAt }.id
    }

    private suspend fun assertBike(bikeId: Long, totalDistanceKm: Double, totalTimeSeconds: Long, lastRideAt: Long?) {
        val bike = requireNotNull(database.bikeDao().getBikeById(bikeId))
        assertEquals(totalDistanceKm, bike.totalDistanceKm, 0.0)
        assertEquals(totalTimeSeconds, bike.totalTimeSeconds)
        assertEquals(lastRideAt, bike.lastRideAt)
    }

    private suspend fun assertComponentAndInterval(componentId: Long, distanceKm: Double, totalTimeSeconds: Long) {
        val component = requireNotNull(database.componentDao().getComponentById(componentId))
        val interval = database.serviceIntervalDao().getIntervalsByComponentIdOnce(componentId).single()
        assertEquals(distanceKm, component.distanceUsedKm, 0.0)
        assertEquals(totalTimeSeconds, component.totalTimeSeconds)
        assertEquals(distanceKm, interval.trackedKm, 0.0)
        assertEquals(totalTimeSeconds, interval.trackedTimeSeconds)
    }
}
