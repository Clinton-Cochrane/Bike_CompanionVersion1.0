package com.clintoncochrane.bikecompanion.data.ride

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.SERVICE_INTERVAL_TYPE_INSPECTION
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalEntity
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalRepository
import com.clintoncochrane.bikecompanion.notifications.ComponentAlertNotifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServiceIntervalResetBoundaryTest {

    private lateinit var database: BikeCompanionDatabase
    private lateinit var rideRepository: RideRepository
    private lateinit var serviceIntervalRepository: ServiceIntervalRepository
    private var bikeAId = 0L
    private var bikeBId = 0L
    private var componentAId = 0L
    private var componentBId = 0L
    private var intervalAId = 0L

    @Before
    fun setUp() = runBlocking {
        database = createDatabase()
        rideRepository = RideRepository(
            database.rideDao(),
            database.bikeDao(),
            database.componentDao(),
            database.serviceIntervalDao(),
            database.componentSwapDao(),
            ComponentAlertNotifier(ApplicationProvider.getApplicationContext(), database.componentDao()),
            RidePersistenceTransaction(database),
        )
        serviceIntervalRepository = ServiceIntervalRepository(database.serviceIntervalDao())
        bikeAId = database.bikeDao().insert(BikeEntity(name = "Bike A", createdAt = 1L))
        bikeBId = database.bikeDao().insert(BikeEntity(name = "Bike B", createdAt = 1L))
        componentAId = addComponent(bikeAId, "Chain A")
        componentBId = addComponent(bikeBId, "Chain B")
        intervalAId = database.serviceIntervalDao().getIntervalsByComponentIdOnce(componentAId).single().id
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun deleteRide_beforeCompletedService_keepsPostServiceProgressAfterDatabaseReopen() = runBlocking {
        val oldRideId = saveRide(bikeAId, distanceKm = 10.0, endedAt = OLD_RIDE_END)
        resetInterval()
        saveRide(bikeAId, distanceKm = 7.0, endedAt = NEW_RIDE_END)

        rideRepository.deleteRide(requireNotNull(database.rideDao().getRideById(oldRideId)))

        reopenDatabase()

        assertIntervalProgress(componentAId, distanceKm = 7.0, timeSeconds = RIDE_DURATION_SECONDS)
    }

    @Test
    fun updateRide_beforeCompletedService_keepsPostServiceProgress() = runBlocking {
        val oldRideId = saveRide(bikeAId, distanceKm = 10.0, endedAt = OLD_RIDE_END)
        resetInterval()
        saveRide(bikeAId, distanceKm = 7.0, endedAt = NEW_RIDE_END)
        val oldRide = requireNotNull(database.rideDao().getRideById(oldRideId))

        rideRepository.updateCompletedRideAndReconcileAggregates(oldRide, oldRide.copy(distanceKm = 20.0))

        reopenDatabase()

        assertIntervalProgress(componentAId, distanceKm = 7.0, timeSeconds = RIDE_DURATION_SECONDS)
    }

    @Test
    fun reassignRide_beforeCompletedService_keepsSourcePostServiceProgress() = runBlocking {
        val oldRideId = saveRide(bikeAId, distanceKm = 10.0, endedAt = OLD_RIDE_END)
        resetInterval()
        saveRide(bikeAId, distanceKm = 7.0, endedAt = NEW_RIDE_END)

        rideRepository.reassignCompletedRide(oldRideId, bikeBId)

        reopenDatabase()

        assertIntervalProgress(componentAId, distanceKm = 7.0, timeSeconds = RIDE_DURATION_SECONDS)
        assertIntervalProgress(componentBId, distanceKm = 10.0, timeSeconds = RIDE_DURATION_SECONDS)
    }

    @Test
    fun saveRide_beforeCompletedService_doesNotChangeProgress() = runBlocking {
        resetInterval()

        saveRide(bikeAId, distanceKm = 10.0, endedAt = OLD_RIDE_END)

        assertIntervalProgress(componentAId, distanceKm = 0.0, timeSeconds = 0L)
    }

    private suspend fun addComponent(bikeId: Long, name: String): Long {
        val componentId = database.componentDao().insert(
            ComponentEntity(bikeId = bikeId, type = "chain", name = name, lifespanKm = 5_000.0, installedAt = 1L),
        )
        database.serviceIntervalDao().insert(
            ServiceIntervalEntity(
                componentId = componentId,
                name = "Inspect",
                intervalKm = 1_000.0,
                intervalTimeSeconds = 10_000L,
                trackedTimeSeconds = 0L,
                type = SERVICE_INTERVAL_TYPE_INSPECTION,
            ),
        )
        return componentId
    }

    private suspend fun resetInterval() {
        serviceIntervalRepository.completeServiceInterval(intervalAId, RESET_AT)
    }

    private suspend fun saveRide(bikeId: Long, distanceKm: Double, endedAt: Long): Long {
        rideRepository.saveRideAndUpdateBikeAndComponents(
            RideEntity(
                bikeId = bikeId,
                distanceKm = distanceKm,
                durationMs = RIDE_DURATION_SECONDS * 1_000L,
                startedAt = endedAt - RIDE_DURATION_SECONDS * 1_000L,
                endedAt = endedAt,
            ),
        )
        return database.rideDao().getAllRides().first().single { it.endedAt == endedAt }.id
    }

    private fun reopenDatabase() {
        database.close()
        database = createDatabase()
    }

    private suspend fun assertIntervalProgress(componentId: Long, distanceKm: Double, timeSeconds: Long) {
        val interval = database.serviceIntervalDao().getIntervalsByComponentIdOnce(componentId).single()
        assertEquals(distanceKm, interval.trackedKm, 0.0)
        assertEquals(timeSeconds, interval.trackedTimeSeconds)
    }

    private fun createDatabase(): BikeCompanionDatabase = Room.databaseBuilder(
        ApplicationProvider.getApplicationContext(),
        BikeCompanionDatabase::class.java,
        DATABASE_NAME,
    ).build()

    private companion object {
        const val DATABASE_NAME = "service_interval_reset_boundary_test.db"
        const val OLD_RIDE_END = 1_000L
        const val RESET_AT = 2_000L
        const val NEW_RIDE_END = 3_000L
        const val RIDE_DURATION_SECONDS = 60L
    }
}
