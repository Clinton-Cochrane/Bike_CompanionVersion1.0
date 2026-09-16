package com.clintoncochrane.bikecompanion

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.BikeCompanionMigrations
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentIdentificationMigrationTest {

    private val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BikeCompanionDatabase::class.java,
    )

    @Test
    fun migration18To19_preservesLegacyMakeModelAsModel() {
        migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 18).use { database ->
            database.execSQL(
                "INSERT INTO components (id, bikeId, lifecycleStatus, type, name, makeModel, lifespanKm, distanceUsedKm, totalTimeSeconds, position, baselineKm, priorUsageCertainty, baselineTimeSeconds, alertThresholdPercent, alertSnoozeUntilKm, alertSnoozeUntilTime, alertsEnabled, installedAt, notes, avgSpeedKmh, maxSpeedKmh, maxSpeedBikeId) VALUES (7, NULL, 'IN_GARAGE', 'front_derailleur', '', 'Shimano FD-7600', 1000, 10, 20, 'none', 30, 'KNOWN', 40, 10, NULL, NULL, 1, 50, 'kept', 6, 7, NULL)",
            )
        }

        migrationTestHelper.runMigrationsAndValidate(
            TEST_DATABASE_NAME,
            19,
            true,
            BikeCompanionMigrations.MIGRATION_18_19,
        ).use { database ->
            database.query("SELECT make, model, notes, distanceUsedKm FROM components WHERE id = 7").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("", cursor.getString(0))
                assertEquals("Shimano FD-7600", cursor.getString(1))
                assertEquals("kept", cursor.getString(2))
                assertEquals(10.0, cursor.getDouble(3), 0.0)
            }
        }
    }

    private companion object {
        const val TEST_DATABASE_NAME = "component_identification_migration_test.db"
    }
}
