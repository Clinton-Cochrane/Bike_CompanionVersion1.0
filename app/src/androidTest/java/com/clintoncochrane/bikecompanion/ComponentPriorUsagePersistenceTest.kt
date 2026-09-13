package com.clintoncochrane.bikecompanion

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentPriorUsagePersistenceTest {

    @Test
    fun componentDao_persistsAllPriorUsageStatesAndBaselines() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, BikeCompanionDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val components = PriorUsageCertainty.entries.mapIndexed { index, certainty ->
                ComponentEntity(
                    bikeId = null,
                    type = "custom_$index",
                    name = certainty.name,
                    lifespanKm = 1_000.0,
                    baselineKm = if (certainty == PriorUsageCertainty.UNKNOWN) 0.0 else 100.0 + index,
                    priorUsageCertainty = certainty,
                    installedAt = 0L,
                )
            }

            db.componentDao().insertAll(components)

            val persisted = db.componentDao().getAllComponents().associateBy { it.priorUsageCertainty }
            assertEquals(PriorUsageCertainty.entries.toSet(), persisted.keys)
            assertEquals(100.0, persisted.getValue(PriorUsageCertainty.KNOWN).baselineKm, 0.0)
            assertEquals(101.0, persisted.getValue(PriorUsageCertainty.APPROXIMATE).baselineKm, 0.0)
            assertEquals(0.0, persisted.getValue(PriorUsageCertainty.UNKNOWN).baselineKm, 0.0)
        } finally {
            db.close()
        }
    }
}
