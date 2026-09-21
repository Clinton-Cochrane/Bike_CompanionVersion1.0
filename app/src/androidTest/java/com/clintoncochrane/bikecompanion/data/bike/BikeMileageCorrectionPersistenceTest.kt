package com.clintoncochrane.bikecompanion.data.bike

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabaseCallback
import com.clintoncochrane.bikecompanion.data.BikeCompanionMigrations
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentLifecycleTransaction
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalEntity
import com.clintoncochrane.bikecompanion.data.ride.RideEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BikeMileageCorrectionPersistenceTest {

    @Test
    fun correctMileage_reopenPreservesCorrectionWithoutCreatingOrChangingRides() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DATABASE_NAME)
        var database: BikeCompanionDatabase? = null
        try {
            val firstDatabase = openDatabase(context)
            database = firstDatabase
            val bikeId = firstDatabase.bikeDao().insert(
                BikeEntity(
                    name = "Used bike",
                    baselineDistanceKm = 500.0,
                    totalDistanceKm = 600.0,
                    createdAt = 1L,
                ),
            )
            val ride = RideEntity(
                bikeId = bikeId,
                distanceKm = 100.0,
                durationMs = 3_600_000L,
                startedAt = 2L,
                endedAt = 3L,
            )
            val rideId = firstDatabase.rideDao().insert(ride)
            val selectedComponentId = firstDatabase.componentDao().insert(
                component(bikeId = bikeId, name = "Selected chain", baselineKm = 200.0),
            )
            val unselectedComponentId = firstDatabase.componentDao().insert(
                component(bikeId = bikeId, name = "New chain", baselineKm = 0.0),
            )
            val intervalId = firstDatabase.serviceIntervalDao().insert(
                ServiceIntervalEntity(
                    componentId = selectedComponentId,
                    name = "Replace",
                    intervalKm = 2_000.0,
                    trackedKm = 300.0,
                ),
            )
            val repository = BikeMileageCorrectionRepository(
                firstDatabase.bikeDao(),
                firstDatabase.componentDao(),
                firstDatabase.serviceIntervalDao(),
                ComponentLifecycleTransaction(firstDatabase),
            )

            assertEquals(
                MileageCorrectionResult.APPLIED,
                repository.correctMileage(bikeId, 1_000.0, setOf(selectedComponentId)),
            )
            firstDatabase.close()

            val reopenedDatabase = openDatabase(context)
            database = reopenedDatabase
            val correctedBike = requireNotNull(reopenedDatabase.bikeDao().getBikeById(bikeId))
            val selectedComponent = requireNotNull(
                reopenedDatabase.componentDao().getComponentById(selectedComponentId),
            )
            val unselectedComponent = requireNotNull(
                reopenedDatabase.componentDao().getComponentById(unselectedComponentId),
            )
            val interval = requireNotNull(
                reopenedDatabase.serviceIntervalDao().getIntervalById(intervalId),
            )

            assertEquals(900.0, correctedBike.baselineDistanceKm, 0.0)
            assertEquals(1_000.0, correctedBike.totalDistanceKm, 0.0)
            assertEquals(700.0, selectedComponent.lifetimeDistanceKm, 0.0)
            assertEquals(100.0, unselectedComponent.lifetimeDistanceKm, 0.0)
            assertEquals(700.0, interval.trackedKm, 0.0)
            assertEquals(ride.copy(id = rideId), reopenedDatabase.rideDao().getRideById(rideId))
            assertEquals(1, reopenedDatabase.rideDao().getRidesByBikeIdOnce(bikeId).size)
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

    private fun component(
        bikeId: Long,
        name: String,
        baselineKm: Double,
    ) = ComponentEntity(
        bikeId = bikeId,
        type = "chain",
        name = name,
        lifespanKm = 2_000.0,
        distanceUsedKm = 100.0,
        baselineKm = baselineKm,
        installedAt = 1L,
    )

    private companion object {
        const val DATABASE_NAME = "bike_mileage_correction_test.db"
    }
}
