package com.you.bikecompanion

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.you.bikecompanion.data.BikeCompanionDatabase
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.component.ComponentRepository
import com.you.bikecompanion.data.component.DefaultSeedComponents
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
            .fallbackToDestructiveMigration()
            .build()
        componentRepository = ComponentRepository(
            db.componentDao(),
            db.serviceIntervalDao(),
            db.componentSwapDao(),
            db.bikeDao(),
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

        componentRepository.seedDefaultComponentsIfEmpty(bikeId)
        assertEquals(
            "Second seed must not add duplicates",
            expectedCount,
            db.componentDao().getComponentCountByBikeId(bikeId),
        )
    }
}
