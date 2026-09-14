package com.clintoncochrane.bikecompanion

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentRepository
import com.clintoncochrane.bikecompanion.data.component.ComponentLifecycleTransaction
import com.clintoncochrane.bikecompanion.data.component.DefaultSeedComponents
import com.clintoncochrane.bikecompanion.data.image.ImageRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies that default component seeding is idempotent: calling it multiple times
 * for the same bike does not create duplicate components.
 */
@RunWith(AndroidJUnit4::class)
class ComponentSeedingIdempotencyTest {

    private lateinit var db: BikeCompanionDatabase
    private lateinit var componentRepository: ComponentRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, BikeCompanionDatabase::class.java)
            .build()
        componentRepository = ComponentRepository(
            db.componentDao(),
            db.serviceIntervalDao(),
            db.componentSwapDao(),
            db.bikeDao(),
            ImageRepository(context.cacheDir.resolve("component-seeding-images")) { null },
            ComponentLifecycleTransaction(db),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun seedDefaultComponentsIfEmpty_twiceForSameBike_createsComponentsOnce() = runBlocking {
        val bikeId = db.bikeDao().insert(
            BikeEntity(name = "Test Bike", createdAt = System.currentTimeMillis()),
        )
        val expectedCount = DefaultSeedComponents.LIST.size

        componentRepository.seedDefaultComponentsIfEmpty(bikeId)
        assertEquals(expectedCount, db.componentDao().getComponentCountByBikeId(bikeId))
        db.componentDao().getComponentsByBikeIdOnce(bikeId).forEach { component ->
            val activeSwaps = db.componentSwapDao().getSwapsByComponentIdOnce(component.id)
                .filter { it.uninstalledAt == null }
            assertEquals(1, activeSwaps.size)
            assertEquals(bikeId, activeSwaps.single().bikeId)
        }

        componentRepository.seedDefaultComponentsIfEmpty(bikeId)
        assertEquals(
            "Second seed must not add duplicates",
            expectedCount,
            db.componentDao().getComponentCountByBikeId(bikeId),
        )
    }
}
