package com.clintoncochrane.bikecompanion

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.BikeCompanionMigrations
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoRemovalMigrationTest {

    private val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BikeCompanionDatabase::class.java,
    )

    @Test
    fun migration17To18_removesLegacyThumbnailColumns() {
        migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 17).close()

        migrationTestHelper.runMigrationsAndValidate(
            TEST_DATABASE_NAME,
            18,
            true,
            BikeCompanionMigrations.MIGRATION_17_18,
        ).use { database ->
            assertFalse(database.columnNames("bikes").contains("thumbnailUri"))
            assertFalse(database.columnNames("components").contains("thumbnailUri"))
        }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.columnNames(table: String): Set<String> =
        query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }

    private companion object {
        const val TEST_DATABASE_NAME = "photo_removal_migration_test.db"
    }
}
