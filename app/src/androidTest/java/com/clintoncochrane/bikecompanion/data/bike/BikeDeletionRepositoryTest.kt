package com.clintoncochrane.bikecompanion.data.bike

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentLifecycleStatus
import com.clintoncochrane.bikecompanion.data.component.ComponentLifecycleTransaction
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import com.clintoncochrane.bikecompanion.data.component.ComponentSwapEntity
import com.clintoncochrane.bikecompanion.data.ride.RideEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BikeDeletionRepositoryTest {

    private lateinit var database: BikeCompanionDatabase
    private lateinit var repository: BikeDeletionRepository
    private var bikeId = 0L

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BikeCompanionDatabase::class.java).build()
        val componentRepository = ComponentRepository(
            database.componentDao(),
            database.serviceIntervalDao(),
            database.componentSwapDao(),
            database.bikeDao(),
            ComponentLifecycleTransaction(database),
            database.serviceHistoryDao(),
        )
        repository = BikeDeletionRepository(
            database,
            database.bikeDao(),
            componentRepository,
        )
        bikeId = database.bikeDao().insert(BikeEntity(name = "Bike A", createdAt = 1L))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun deleteBike_withoutInstalledComponents_preservesRideHistoryWithoutBikeAssignment() = runBlocking {
        val rideId = database.rideDao().insert(
            RideEntity(
                bikeId = bikeId,
                distanceKm = 20.0,
                durationMs = 3_600_000L,
                startedAt = 1L,
                endedAt = 2L,
            ),
        )

        repository.deleteBike(requireBike(), BikeDeletionComponentDisposition.MOVE_TO_GARAGE)

        assertNull(database.bikeDao().getBikeById(bikeId))
        assertNull(database.rideDao().getRideById(rideId)?.bikeId)
    }

    @Test
    fun deleteBike_moveToGarage_preservesComponentUsageAndDetachedSwapHistory() = runBlocking {
        val componentId = addInstalledComponent()

        repository.deleteBike(requireBike(), BikeDeletionComponentDisposition.MOVE_TO_GARAGE)

        val component = requireNotNull(database.componentDao().getComponentById(componentId))
        assertNull(component.bikeId)
        assertEquals(ComponentLifecycleStatus.IN_GARAGE, component.lifecycleStatus)
        assertEquals(125.0, component.distanceUsedKm, 0.0)
        assertEquals(3_600L, component.totalTimeSeconds)
        val swap = database.componentSwapDao().getSwapsByComponentIdOnce(componentId).single()
        assertNull(swap.bikeId)
        assertNotNull(swap.uninstalledAt)
    }

    @Test
    fun deleteBike_retire_preservesComponentUsageAndDetachedSwapHistory() = runBlocking {
        val componentId = addInstalledComponent()

        repository.deleteBike(requireBike(), BikeDeletionComponentDisposition.RETIRE)

        val component = requireNotNull(database.componentDao().getComponentById(componentId))
        assertNull(component.bikeId)
        assertEquals(ComponentLifecycleStatus.RETIRED, component.lifecycleStatus)
        assertEquals(125.0, component.distanceUsedKm, 0.0)
        assertEquals(3_600L, component.totalTimeSeconds)
        val swap = database.componentSwapDao().getSwapsByComponentIdOnce(componentId).single()
        assertNull(swap.bikeId)
        assertNotNull(swap.uninstalledAt)
    }

    private suspend fun addInstalledComponent(): Long {
        val componentId = database.componentDao().insert(
            ComponentEntity(
                bikeId = bikeId,
                type = "chain",
                name = "Reusable chain",
                lifespanKm = 3_000.0,
                distanceUsedKm = 125.0,
                totalTimeSeconds = 3_600L,
                installedAt = 1L,
            ),
        )
        database.componentSwapDao().insert(
            ComponentSwapEntity(componentId = componentId, bikeId = bikeId, installedAt = 1L),
        )
        return componentId
    }

    private suspend fun requireBike(): BikeEntity = requireNotNull(database.bikeDao().getBikeById(bikeId))
}
