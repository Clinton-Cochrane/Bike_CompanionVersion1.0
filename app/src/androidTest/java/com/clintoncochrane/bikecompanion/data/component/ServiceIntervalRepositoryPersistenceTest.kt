package com.clintoncochrane.bikecompanion.data.component

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServiceIntervalRepositoryPersistenceTest {

    private lateinit var database: BikeCompanionDatabase
    private lateinit var repository: ServiceIntervalRepository
    private var componentId = 0L

    @Before
    fun setUp(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BikeCompanionDatabase::class.java).build()
        repository = ServiceIntervalRepository(database.serviceIntervalDao())

        val bikeId = database.bikeDao().insert(
            BikeEntity(
                name = "Interval Test Bike",
                totalDistanceKm = 9_000.0,
                totalTimeSeconds = 900_000L,
                createdAt = 1L,
            ),
        )
        componentId = database.componentDao().insert(
            ComponentEntity(
                bikeId = bikeId,
                type = "hub",
                name = "Rear hub",
                lifespanKm = 20_000.0,
                distanceUsedKm = 8_500.0,
                totalTimeSeconds = 850_000L,
                baselineKm = 250.0,
                baselineTimeSeconds = 25_000L,
                installedAt = 2L,
                avgSpeedKmh = 18.5,
                maxSpeedKmh = 62.0,
                maxSpeedBikeId = bikeId,
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun completeServiceInterval_multipleIntervals_preservesComponentAndSiblingIntervals() = runBlocking {
        val selectedId = database.serviceIntervalDao().insert(
            ServiceIntervalEntity(
                componentId = componentId,
                name = "Inspect bearings",
                intervalKm = 1_000.0,
                trackedKm = 900.0,
                type = SERVICE_INTERVAL_TYPE_INSPECTION,
                intervalTimeSeconds = 100_000L,
                trackedTimeSeconds = 90_000L,
            ),
        )
        val grease = ServiceIntervalEntity(
            componentId = componentId,
            name = "Grease bearings",
            intervalKm = 5_000.0,
            trackedKm = 4_000.0,
            type = SERVICE_INTERVAL_TYPE_GREASE,
            intervalTimeSeconds = 500_000L,
            trackedTimeSeconds = 400_000L,
        )
        val greaseId = database.serviceIntervalDao().insert(grease)
        val replacement = ServiceIntervalEntity(
            componentId = componentId,
            name = "Replace hub",
            intervalKm = 20_000.0,
            trackedKm = 8_500.0,
            type = SERVICE_INTERVAL_TYPE_REPLACE,
        )
        val replacementId = database.serviceIntervalDao().insert(replacement)
        val componentBefore = requireNotNull(database.componentDao().getComponentById(componentId))

        assertTrue(repository.completeServiceInterval(selectedId))
        assertTrue(repository.completeServiceInterval(selectedId))

        val componentAfter = requireNotNull(database.componentDao().getComponentById(componentId))
        val intervalsAfter = database.serviceIntervalDao()
            .getIntervalsByComponentIdOnce(componentId)
            .associateBy { it.id }
        val selectedAfter = requireNotNull(intervalsAfter[selectedId])

        assertEquals(componentBefore, componentAfter)
        assertEquals(0.0, selectedAfter.trackedKm, 0.0)
        assertEquals(0L, selectedAfter.trackedTimeSeconds)
        assertEquals("Inspect bearings", selectedAfter.name)
        assertEquals(1_000.0, selectedAfter.intervalKm, 0.0)
        assertEquals(100_000L, selectedAfter.intervalTimeSeconds)
        assertEquals(grease.copy(id = greaseId), intervalsAfter[greaseId])
        assertEquals(replacement.copy(id = replacementId), intervalsAfter[replacementId])
    }
}
