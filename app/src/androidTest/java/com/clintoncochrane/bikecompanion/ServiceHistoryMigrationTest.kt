package com.clintoncochrane.bikecompanion

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.BikeCompanionMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServiceHistoryMigrationTest {

    private val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BikeCompanionDatabase::class.java,
    )

    @Test
    fun migration19To20_createsServiceHistoryWithSessionUniqueness() {
        migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 19).close()

        migrationTestHelper.runMigrationsAndValidate(
            TEST_DATABASE_NAME,
            20,
            true,
            BikeCompanionMigrations.MIGRATION_19_20,
        ).use { database ->
            database.execSQL(
                "INSERT INTO service_history (sessionId, serviceIntervalId, serviceName, serviceType, componentId, bikeId, completedAt, bikeOdometerKm) VALUES ('session', 4, 'Inspect', 'inspection', 7, 2, 100, 1234.5)",
            )
            database.query("SELECT sessionId, bikeOdometerKm FROM service_history").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("session", cursor.getString(0))
                assertEquals(1234.5, cursor.getDouble(1), 0.0)
            }
            val duplicate = runCatching {
                database.execSQL(
                    "INSERT INTO service_history (sessionId, serviceIntervalId, serviceName, serviceType, componentId, bikeId, completedAt, bikeOdometerKm) VALUES ('session', 4, 'Inspect again', 'inspection', 7, 2, 200, 1234.5)",
                )
            }
            assertTrue(duplicate.isFailure)
        }
    }

    private companion object {
        const val TEST_DATABASE_NAME = "service_history_migration_test.db"
    }
}
