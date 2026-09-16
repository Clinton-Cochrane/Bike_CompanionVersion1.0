package com.clintoncochrane.bikecompanion.data.component

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
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
    fun replaceComponentForService_movesOldToGarageAndClonesActualPoliciesAtZero() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, BikeCompanionDatabase::class.java).build()
        try {
            val bikeId = database.bikeDao().insert(
                BikeEntity(name = "Bike", totalDistanceKm = 4_321.0, createdAt = 1L),
            )
            val oldId = database.componentDao().insert(
                ComponentEntity(
                    bikeId = bikeId,
                    type = "chain",
                    name = "Summer chain",
                    make = "Shimano",
                    model = "HG-701",
                    lifespanKm = 4_000.0,
                    distanceUsedKm = 4_000.0,
                    totalTimeSeconds = 80_000L,
                    position = "none",
                    installedAt = 1L,
                ),
            )
            database.componentSwapDao().insert(ComponentSwapEntity(componentId = oldId, bikeId = bikeId, installedAt = 1L))
            database.serviceIntervalDao().insert(
                ServiceIntervalEntity(
                    componentId = oldId,
                    name = "Custom clean",
                    intervalKm = 321.0,
                    trackedKm = 300.0,
                    type = SERVICE_INTERVAL_TYPE_GREASE,
                    intervalTimeSeconds = 7_777L,
                    trackedTimeSeconds = 7_000L,
                ),
            )
            val replaceIntervalId = database.serviceIntervalDao().insert(
                ServiceIntervalEntity(
                    componentId = oldId,
                    name = "Custom replacement",
                    intervalKm = 4_000.0,
                    trackedKm = 4_000.0,
                    type = SERVICE_INTERVAL_TYPE_REPLACE,
                ),
            )
            val repository = ComponentRepository(
                database.componentDao(), database.serviceIntervalDao(), database.componentSwapDao(), database.bikeDao(),
                ComponentLifecycleTransaction(database), database.serviceHistoryDao(),
            )

            val replacementId = requireNotNull(
                repository.replaceComponentForService(replaceIntervalId, "session", completedAt = 10_000L),
            )

            val old = requireNotNull(database.componentDao().getComponentById(oldId))
            val replacement = requireNotNull(database.componentDao().getComponentById(replacementId))
            val replacementIntervals = database.serviceIntervalDao().getIntervalsByComponentIdOnce(replacementId)
            val history = database.serviceHistoryDao().getBySessionId("session").single()
            assertEquals(ComponentLifecycleStatus.IN_GARAGE, old.lifecycleStatus)
            assertNull(old.bikeId)
            assertEquals(ComponentLifecycleStatus.INSTALLED, replacement.lifecycleStatus)
            assertEquals(bikeId, replacement.bikeId)
            assertEquals("", replacement.name)
            assertEquals("Shimano", replacement.make)
            assertEquals("HG-701", replacement.model)
            assertEquals("chain", replacement.type)
            assertEquals("none", replacement.position)
            assertEquals(0.0, replacement.lifetimeDistanceKm, 0.0)
            assertEquals(2, replacementIntervals.size)
            assertEquals(setOf("Custom clean", "Custom replacement"), replacementIntervals.map { it.name }.toSet())
            assertTrue(replacementIntervals.all { it.trackedKm == 0.0 })
            assertEquals(0L, replacementIntervals.single { it.name == "Custom clean" }.trackedTimeSeconds)
            assertEquals(321.0, replacementIntervals.single { it.name == "Custom clean" }.intervalKm, 0.0)
            assertEquals(7_777L, replacementIntervals.single { it.name == "Custom clean" }.intervalTimeSeconds)
            assertEquals(oldId, history.componentId)
            assertEquals(replacementId, history.replacementComponentId)
            assertEquals(4_321.0, history.bikeOdometerKm, 0.0)
        } finally {
            database.close()
        }
    }

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
                ComponentLifecycleTransaction(database), database.serviceHistoryDao(),
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
                ComponentLifecycleTransaction(database), database.serviceHistoryDao(),
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
