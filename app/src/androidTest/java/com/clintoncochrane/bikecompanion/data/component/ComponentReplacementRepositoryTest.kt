package com.clintoncochrane.bikecompanion.data.component

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.image.ImageRepository
import com.clintoncochrane.bikecompanion.data.ride.RideEntity
import com.clintoncochrane.bikecompanion.data.ride.RidePersistenceTransaction
import com.clintoncochrane.bikecompanion.data.ride.RideRepository
import com.clintoncochrane.bikecompanion.notifications.ComponentAlertNotifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentReplacementRepositoryTest {
    @Test
    fun replaceComponent_retiresOldComponentAndInstallsIndependentReplacement() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, BikeCompanionDatabase::class.java).build()
        try {
            val bikeId = database.bikeDao().insert(BikeEntity(name = "Bike", createdAt = 1L))
            val oldId = database.componentDao().insert(
                ComponentEntity(bikeId = bikeId, type = "chain", name = "Old", lifespanKm = 3_000.0, distanceUsedKm = 500.0, installedAt = 1L),
            )
            database.componentSwapDao().insert(ComponentSwapEntity(componentId = oldId, bikeId = bikeId, installedAt = 1L))
            val repository = ComponentRepository(
                database.componentDao(), database.serviceIntervalDao(), database.componentSwapDao(), database.bikeDao(),
                ImageRepository(context.cacheDir.resolve("replacement-images")) { null }, ComponentLifecycleTransaction(database),
            )

            val replacementId = repository.replaceComponent(
                requireNotNull(database.componentDao().getComponentById(oldId)),
                ComponentEntity(bikeId = bikeId, type = "chain", name = "Used replacement", lifespanKm = 3_000.0, baselineKm = 50.0, priorUsageCertainty = PriorUsageCertainty.APPROXIMATE, installedAt = 0L),
            )

            val old = requireNotNull(database.componentDao().getComponentById(oldId))
            val replacement = requireNotNull(database.componentDao().getComponentById(replacementId))
            assertEquals(ComponentLifecycleStatus.RETIRED, old.lifecycleStatus)
            assertNull(old.bikeId)
            assertEquals(500.0, old.distanceUsedKm, 0.0)
            assertEquals(ComponentLifecycleStatus.INSTALLED, replacement.lifecycleStatus)
            assertEquals(bikeId, replacement.bikeId)
            assertEquals(50.0, replacement.lifetimeDistanceKm, 0.0)
            assertTrue(database.componentSwapDao().getSwapsByComponentIdOnce(oldId).none { it.uninstalledAt == null })
            assertEquals(1, database.componentSwapDao().getSwapsByComponentIdOnce(replacementId).count { it.uninstalledAt == null })

            val rideRepository = RideRepository(
                database.rideDao(), database.bikeDao(), database.componentDao(), database.serviceIntervalDao(),
                database.componentSwapDao(), ComponentAlertNotifier(context, database.componentDao()),
                RidePersistenceTransaction(database),
            )
            val rideEndedAt = System.currentTimeMillis() + 1_000L
            rideRepository.saveRideAndUpdateBikeAndComponents(
                RideEntity(
                    bikeId = bikeId,
                    distanceKm = 10.0,
                    durationMs = 3_600_000L,
                    startedAt = rideEndedAt - 3_600_000L,
                    endedAt = rideEndedAt,
                ),
            )

            assertEquals(500.0, requireNotNull(database.componentDao().getComponentById(oldId)).distanceUsedKm, 0.0)
            assertEquals(10.0, requireNotNull(database.componentDao().getComponentById(replacementId)).distanceUsedKm, 0.0)

            val unknownReplacementId = repository.replaceComponent(
                requireNotNull(database.componentDao().getComponentById(replacementId)),
                ComponentEntity(
                    bikeId = bikeId,
                    type = "chain",
                    name = "Unknown replacement",
                    lifespanKm = 3_000.0,
                    baselineKm = 999.0,
                    priorUsageCertainty = PriorUsageCertainty.UNKNOWN,
                    installedAt = 0L,
                ),
            )
            assertEquals(
                0.0,
                requireNotNull(database.componentDao().getComponentById(unknownReplacementId)).lifetimeDistanceKm,
                0.0,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun replaceComponent_serviceIntervalInsertFails_rollsBackOldRetirementAndNewComponent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, BikeCompanionDatabase::class.java).build()
        try {
            val bikeId = database.bikeDao().insert(BikeEntity(name = "Bike", createdAt = 1L))
            val oldId = database.componentDao().insert(
                ComponentEntity(bikeId = bikeId, type = "chain", name = "Old", lifespanKm = 3_000.0, installedAt = 1L),
            )
            database.componentSwapDao().insert(
                ComponentSwapEntity(componentId = oldId, bikeId = bikeId, installedAt = 1L),
            )
            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER fail_replacement_interval
                BEFORE INSERT ON service_intervals
                BEGIN
                    SELECT RAISE(ABORT, 'forced interval failure');
                END
                """.trimIndent(),
            )
            val repository = ComponentRepository(
                database.componentDao(), database.serviceIntervalDao(), database.componentSwapDao(), database.bikeDao(),
                ImageRepository(context.cacheDir.resolve("replacement-rollback-images")) { null },
                ComponentLifecycleTransaction(database),
            )

            val result = runCatching {
                repository.replaceComponent(
                    requireNotNull(database.componentDao().getComponentById(oldId)),
                    ComponentEntity(bikeId = bikeId, type = "chain", name = "New", lifespanKm = 3_000.0, installedAt = 0L),
                )
            }

            assertTrue(result.isFailure)
            val old = requireNotNull(database.componentDao().getComponentById(oldId))
            assertEquals(ComponentLifecycleStatus.INSTALLED, old.lifecycleStatus)
            assertEquals(bikeId, old.bikeId)
            assertEquals(1, database.componentDao().getAllComponents().size)
            assertEquals(
                1,
                database.componentSwapDao().getSwapsByComponentIdOnce(oldId).count { it.uninstalledAt == null },
            )
        } finally {
            database.close()
        }
    }
}
