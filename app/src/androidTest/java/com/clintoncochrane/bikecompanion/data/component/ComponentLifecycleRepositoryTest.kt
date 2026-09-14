package com.clintoncochrane.bikecompanion.data.component

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.image.ImageRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentLifecycleRepositoryTest {

    private lateinit var database: BikeCompanionDatabase
    private lateinit var repository: ComponentRepository
    private var bikeAId = 0L
    private var bikeBId = 0L
    private var componentId = 0L

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BikeCompanionDatabase::class.java).build()
        repository = ComponentRepository(
            database.componentDao(),
            database.serviceIntervalDao(),
            database.componentSwapDao(),
            database.bikeDao(),
            ImageRepository(context.cacheDir.resolve("component-lifecycle-images")) { null },
            ComponentLifecycleTransaction(database),
        )
        bikeAId = database.bikeDao().insert(BikeEntity(name = "Bike A", createdAt = 1L))
        bikeBId = database.bikeDao().insert(BikeEntity(name = "Bike B", createdAt = 1L))
        componentId = database.componentDao().insert(
            ComponentEntity(
                bikeId = bikeAId,
                type = "chain",
                name = "Reusable chain",
                lifespanKm = 3_000.0,
                distanceUsedKm = 125.0,
                totalTimeSeconds = 3_600L,
                installedAt = 1L,
            ),
        )
        database.componentSwapDao().insert(
            ComponentSwapEntity(componentId = componentId, bikeId = bikeAId, installedAt = 1L),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun removeToGarage_thenReinstallOnAnotherBike_preservesUsageAndCreatesOneActiveSwap() = runBlocking {
        repository.removeToGarage(requireComponent())

        val inGarage = requireComponent()
        assertNull(inGarage.bikeId)
        assertEquals(ComponentLifecycleStatus.IN_GARAGE, inGarage.lifecycleStatus)
        assertEquals(125.0, inGarage.distanceUsedKm, 0.0)
        assertEquals(3_600L, inGarage.totalTimeSeconds)
        assertNotNull(database.componentSwapDao().getSwapsByComponentIdOnce(componentId).single().uninstalledAt)

        repository.installComponent(inGarage, bikeBId)

        val reinstalled = requireComponent()
        assertEquals(bikeBId, reinstalled.bikeId)
        assertEquals(ComponentLifecycleStatus.INSTALLED, reinstalled.lifecycleStatus)
        assertEquals(125.0, reinstalled.distanceUsedKm, 0.0)
        assertEquals(3_600L, reinstalled.totalTimeSeconds)
        val swaps = database.componentSwapDao().getSwapsByComponentIdOnce(componentId)
        assertEquals(2, swaps.size)
        assertEquals(1, swaps.count { it.uninstalledAt == null })
        assertEquals(bikeBId, swaps.single { it.uninstalledAt == null }.bikeId)
    }

    @Test
    fun retireComponent_keepsHistoricalRecordButExcludesItFromGarageInstallChoices() = runBlocking {
        repository.retireComponent(requireComponent())

        val retired = requireComponent()
        assertNull(retired.bikeId)
        assertEquals(ComponentLifecycleStatus.RETIRED, retired.lifecycleStatus)
        assertEquals(125.0, retired.distanceUsedKm, 0.0)
        assertEquals(3_600L, retired.totalTimeSeconds)
        assertTrue(database.componentDao().getComponentsInGarageOnce().isEmpty())
        assertTrue(repository.getComponentsInGarageMatching("chain", "none").isEmpty())
        assertEquals(retired, database.componentDao().getAllComponents().single())
        assertNotNull(database.componentSwapDao().getSwapsByComponentIdOnce(componentId).single().uninstalledAt)
    }

    @Test
    fun installComponent_retiredComponent_rejectsInstallAndDoesNotCreateAnotherActiveSwap() = runBlocking {
        repository.retireComponent(requireComponent())

        val result = runCatching { repository.installComponent(requireComponent(), bikeBId) }

        assertTrue(result.isFailure)
        assertEquals(ComponentLifecycleStatus.RETIRED, requireComponent().lifecycleStatus)
        assertFalse(database.componentSwapDao().getSwapsByComponentIdOnce(componentId).any { it.uninstalledAt == null })
    }

    @Test
    fun componentQueries_separateRetiredComponentsFromNormalComponents() = runBlocking {
        repository.retireComponent(requireComponent())
        val garageComponentId = database.componentDao().insert(
            ComponentEntity(
                bikeId = null,
                lifecycleStatus = ComponentLifecycleStatus.IN_GARAGE,
                type = "cassette",
                name = "Reusable cassette",
                lifespanKm = 10_000.0,
                installedAt = 2L,
            ),
        )

        assertEquals(
            listOf(componentId),
            database.componentDao().getRetiredComponentsOnce().map { it.id },
        )
        assertEquals(
            listOf(garageComponentId),
            database.componentDao().getNonRetiredComponentsOnce().map { it.id },
        )
    }

    private suspend fun requireComponent(): ComponentEntity =
        requireNotNull(database.componentDao().getComponentById(componentId))
}
