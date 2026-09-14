package com.clintoncochrane.bikecompanion.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentContextEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentLifecycleTransaction
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import com.clintoncochrane.bikecompanion.data.component.ComponentSwapEntity
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import com.clintoncochrane.bikecompanion.data.image.ImageRepository
import com.clintoncochrane.bikecompanion.data.ride.RideEntity
import com.clintoncochrane.bikecompanion.data.ride.RidePersistenceTransaction
import com.clintoncochrane.bikecompanion.data.ride.RideRepository
import com.clintoncochrane.bikecompanion.data.ride.RideSource
import com.clintoncochrane.bikecompanion.notifications.ComponentAlertNotifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistedDataIntegrityTest {

    @Test
    fun representativeData_reopen_preservesRelationshipsBaselinesAndDedupe() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DATABASE_NAME)
        var database: BikeCompanionDatabase? = null
        try {
            val firstDatabase = openDatabase(context)
            database = firstDatabase
            val bikeId = firstDatabase.bikeDao().insert(
                BikeEntity(
                    name = "Persisted bike",
                    baselineDistanceKm = 123.0,
                    totalDistanceKm = 123.0,
                    createdAt = 1L,
                ),
            )
            val componentRepository = componentRepository(context, firstDatabase)
            val componentId = componentRepository.insertComponent(
                ComponentEntity(
                    bikeId = bikeId,
                    type = "chain",
                    name = "Persisted chain",
                    lifespanKm = 3_000.0,
                    baselineKm = 45.0,
                    priorUsageCertainty = PriorUsageCertainty.APPROXIMATE,
                    installedAt = 2L,
                ),
            )
            firstDatabase.componentContextDao().insert(
                ComponentContextEntity(componentId = componentId, notes = "Keep me"),
            )
            val importedRide = RideEntity(
                bikeId = bikeId,
                distanceKm = 12.0,
                durationMs = 3_600_000L,
                startedAt = 3L,
                endedAt = 3_600_003L,
                source = RideSource.HEALTH_CONNECT,
                healthConnectRecordId = "persisted-health-record",
            )
            assertTrue(rideRepository(context, firstDatabase).saveHealthConnectRideAndUpdateBikeAndComponents(importedRide))
            firstDatabase.close()

            val reopenedDatabase = openDatabase(context)
            database = reopenedDatabase
            val bike = requireNotNull(reopenedDatabase.bikeDao().getBikeById(bikeId))
            val component = requireNotNull(reopenedDatabase.componentDao().getComponentById(componentId))
            val intervals = reopenedDatabase.serviceIntervalDao().getIntervalsByComponentIdOnce(componentId)
            val swaps = reopenedDatabase.componentSwapDao().getSwapsByComponentIdOnce(componentId)

            assertEquals(135.0, bike.totalDistanceKm, 0.0)
            assertEquals(12.0, component.distanceUsedKm, 0.0)
            assertEquals(57.0, component.lifetimeDistanceKm, 0.0)
            assertTrue(intervals.isNotEmpty())
            assertTrue(intervals.all { it.trackedKm == 57.0 })
            assertEquals("Keep me", reopenedDatabase.componentContextDao().getByComponentId(componentId)?.notes)
            assertEquals(1, swaps.count { it.uninstalledAt == null && it.bikeId == bikeId })
            assertEquals(1, reopenedDatabase.rideDao().getAllRides().first().size)
            assertFalse(rideRepository(context, reopenedDatabase).saveHealthConnectRideAndUpdateBikeAndComponents(importedRide))
            val duplicateActiveSwap = runCatching {
                reopenedDatabase.componentSwapDao().insert(
                    ComponentSwapEntity(componentId = componentId, bikeId = bikeId, installedAt = 4L),
                )
            }
            assertTrue(duplicateActiveSwap.isFailure)
            reopenedDatabase.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { cursor ->
                assertFalse(cursor.moveToFirst())
            }
        } finally {
            database?.close()
            context.deleteDatabase(DATABASE_NAME)
        }
    }

    private fun openDatabase(context: Context): BikeCompanionDatabase = Room.databaseBuilder(
        context,
        BikeCompanionDatabase::class.java,
        DATABASE_NAME,
    ).addMigrations(*BikeCompanionMigrations.ALL)
        .addCallback(BikeCompanionDatabaseCallback)
        .build()

    private fun componentRepository(
        context: Context,
        database: BikeCompanionDatabase,
    ) = ComponentRepository(
        database.componentDao(),
        database.serviceIntervalDao(),
        database.componentSwapDao(),
        database.bikeDao(),
        ImageRepository(context.cacheDir.resolve("persisted-data-images")) { null },
        ComponentLifecycleTransaction(database),
    )

    private fun rideRepository(
        context: Context,
        database: BikeCompanionDatabase,
    ) = RideRepository(
        database.rideDao(),
        database.bikeDao(),
        database.componentDao(),
        database.serviceIntervalDao(),
        database.componentSwapDao(),
        ComponentAlertNotifier(context, database.componentDao()),
        RidePersistenceTransaction(database),
    )

    private companion object {
        const val DATABASE_NAME = "persisted_data_integrity_test.db"
    }
}
