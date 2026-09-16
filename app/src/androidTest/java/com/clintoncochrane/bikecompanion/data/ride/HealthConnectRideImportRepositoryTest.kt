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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthConnectRideImportRepositoryTest {

    private lateinit var database: BikeCompanionDatabase
    private lateinit var repository: RideRepository
    private var bikeId = 0L
    private var componentId = 0L
    private var intervalId = 0L

    @Before
    fun setUp(): Unit = runBlocking {
        database = newDatabase()
        repository = newRepository(database)
        bikeId = database.bikeDao().insert(BikeEntity(name = "Import bike", createdAt = 0L))
        componentId = database.componentDao().insert(
            ComponentEntity(bikeId = bikeId, type = "chain", name = "Chain", lifespanKm = 3_000.0, installedAt = 0L),
        )
        intervalId = database.serviceIntervalDao().insert(
            ServiceIntervalEntity(componentId = componentId, name = "Inspect", intervalKm = 1_000.0),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveHealthConnectRide_sameRecordIdTwice_appliesMileageOnce() = runBlocking {
        val ride = healthConnectRide(recordId = "session-1", distanceKm = 24.5)

        assertEquals(true, repository.saveHealthConnectRideAndUpdateBikeAndComponents(ride))
        assertEquals(false, repository.saveHealthConnectRideAndUpdateBikeAndComponents(ride))

        assertMileage(distanceKm = 24.5)
        assertEquals(1, database.rideDao().getAllRides().first().size)
    }

    @Test
    fun saveHealthConnectRide_afterDatabaseReopened_sameRecordIdIsStillIgnored() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("health_connect_import_restart_test.db")
        if (dbFile.exists()) dbFile.delete()
        var restartedDatabase: BikeCompanionDatabase? = null
        try {
            val firstDatabase = Room.databaseBuilder(context, BikeCompanionDatabase::class.java, dbFile.name).build()
            val firstBikeId = firstDatabase.bikeDao().insert(BikeEntity(name = "Restart bike", createdAt = 0L))
            firstDatabase.componentDao().insert(
                ComponentEntity(bikeId = firstBikeId, type = "chain", name = "Chain", lifespanKm = 3_000.0, installedAt = 0L),
            )
            val ride = healthConnectRide(recordId = "session-1", distanceKm = 24.5).copy(bikeId = firstBikeId)
            assertEquals(true, newRepository(firstDatabase).saveHealthConnectRideAndUpdateBikeAndComponents(ride))
            firstDatabase.close()

            restartedDatabase = Room.databaseBuilder(context, BikeCompanionDatabase::class.java, dbFile.name).build()
            assertEquals(false, newRepository(restartedDatabase).saveHealthConnectRideAndUpdateBikeAndComponents(ride))
            assertEquals(1, restartedDatabase.rideDao().getAllRides().first().size)
            assertEquals(24.5, requireNotNull(restartedDatabase.bikeDao().getBikeById(firstBikeId)).totalDistanceKm, 0.0)
        } finally {
            restartedDatabase?.close()
            if (dbFile.exists()) dbFile.delete()
        }
    }

    @Test
    fun saveHealthConnectRide_differentRecordIdsWithMatchingDetails_importsBoth() = runBlocking {
        assertEquals(true, repository.saveHealthConnectRideAndUpdateBikeAndComponents(healthConnectRide("session-1", 24.5)))
        assertEquals(true, repository.saveHealthConnectRideAndUpdateBikeAndComponents(healthConnectRide("session-2", 24.5)))

        assertMileage(distanceKm = 49.0)
        assertEquals(2, database.rideDao().getAllRides().first().size)
    }

    private fun newDatabase(): BikeCompanionDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        BikeCompanionDatabase::class.java,
    ).build()

    private fun newRepository(database: BikeCompanionDatabase): RideRepository = RideRepository(
        database.rideDao(),
        database.bikeDao(),
        database.componentDao(),
        database.serviceIntervalDao(),
        database.componentSwapDao(),
        ComponentAlertNotifier(ApplicationProvider.getApplicationContext(), database.componentDao()),
        RidePersistenceTransaction(database),
    )

    private fun healthConnectRide(recordId: String, distanceKm: Double) = RideEntity(
        bikeId = bikeId,
        distanceKm = distanceKm,
        durationMs = 3_600_000L,
        startedAt = 1_000L,
        endedAt = 3_601_000L,
        source = RideSource.HEALTH_CONNECT,
        healthConnectRecordId = recordId,
    )

    private suspend fun assertMileage(distanceKm: Double) {
        val bike = requireNotNull(database.bikeDao().getBikeById(bikeId))
        val component = requireNotNull(database.componentDao().getComponentById(componentId))
        val interval = database.serviceIntervalDao().getIntervalsByComponentIdOnce(componentId).single { it.id == intervalId }
        assertEquals(distanceKm, bike.totalDistanceKm, 0.0)
        assertEquals(distanceKm, component.distanceUsedKm, 0.0)
        assertEquals(distanceKm, interval.trackedKm, 0.0)
    }
}
