package com.clintoncochrane.bikecompanion

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.clintoncochrane.bikecompanion.data.BikeCompanionMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentSwapBikeDeletionMigrationTest {

    @Test
    fun migration16To17_repairsActiveInstallationsAndEnforcesUniqueness() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext())
                .name(null)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(16) {
                        override fun onCreate(database: SupportSQLiteDatabase) {
                            database.execSQL("CREATE TABLE bikes (id INTEGER PRIMARY KEY NOT NULL)")
                            database.execSQL(
                                "CREATE TABLE components (id INTEGER PRIMARY KEY NOT NULL, bikeId INTEGER, lifecycleStatus TEXT NOT NULL, installedAt INTEGER NOT NULL)",
                            )
                            database.execSQL(
                                "CREATE TABLE component_swaps (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, componentId INTEGER NOT NULL, bikeId INTEGER, installedAt INTEGER NOT NULL, uninstalledAt INTEGER)",
                            )
                            database.execSQL(
                                "CREATE UNIQUE INDEX index_component_swaps_one_active_install ON component_swaps(componentId) WHERE uninstalledAt IS NULL",
                            )
                        }

                        override fun onUpgrade(
                            database: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )
        val database = helper.writableDatabase
        database.execSQL("INSERT INTO bikes VALUES (1)")
        database.execSQL("INSERT INTO bikes VALUES (2)")
        database.execSQL("INSERT INTO components VALUES (1, 1, 'INSTALLED', 10)")
        database.execSQL("INSERT INTO components VALUES (2, NULL, 'IN_GARAGE', 20)")
        database.execSQL("INSERT INTO components VALUES (3, 2, 'INSTALLED', 30)")
        database.execSQL("INSERT INTO component_swaps VALUES (1, 1, 2, 5, NULL)")
        database.execSQL("INSERT INTO component_swaps VALUES (2, 2, 1, 6, NULL)")

        BikeCompanionMigrations.MIGRATION_16_17.migrate(database)

        assertActiveSwap(database, componentId = 1L, expectedBikeId = 1L)
        assertActiveSwap(database, componentId = 2L, expectedBikeId = null)
        assertActiveSwap(database, componentId = 3L, expectedBikeId = 2L)
        val duplicate = runCatching {
            database.execSQL("INSERT INTO component_swaps (componentId, bikeId, installedAt) VALUES (1, 1, 40)")
        }
        assertTrue(duplicate.isFailure)
        helper.close()
    }

    @Test
    fun migration15To16_preservesSwapRowsWhenTheirBikeIsDeleted() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext())
                .name(null)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(15) {
                        override fun onCreate(database: SupportSQLiteDatabase) {
                            database.execSQL("CREATE TABLE bikes (id INTEGER PRIMARY KEY NOT NULL)")
                            database.execSQL("CREATE TABLE components (id INTEGER PRIMARY KEY NOT NULL)")
                            database.execSQL(
                                "CREATE TABLE component_swaps (id INTEGER PRIMARY KEY NOT NULL, componentId INTEGER NOT NULL, bikeId INTEGER NOT NULL, installedAt INTEGER NOT NULL, uninstalledAt INTEGER, FOREIGN KEY(componentId) REFERENCES components(id) ON DELETE CASCADE, FOREIGN KEY(bikeId) REFERENCES bikes(id) ON DELETE CASCADE)",
                            )
                        }

                        override fun onUpgrade(
                            database: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )
        val database = helper.writableDatabase
        database.execSQL("PRAGMA foreign_keys=ON")
        database.execSQL("INSERT INTO bikes VALUES (1)")
        database.execSQL("INSERT INTO components VALUES (1)")
        database.execSQL("INSERT INTO component_swaps VALUES (1, 1, 1, 1, 2)")

        BikeCompanionMigrations.MIGRATION_15_16.migrate(database)
        database.execSQL("DELETE FROM bikes WHERE id = 1")

        database.query("SELECT bikeId FROM component_swaps WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
        }
        database.query("PRAGMA foreign_key_list(component_swaps)").use { cursor ->
            val tableIndex = cursor.getColumnIndex("table")
            val onDeleteIndex = cursor.getColumnIndex("on_delete")
            var foundBikeForeignKey = false
            while (cursor.moveToNext()) {
                if (cursor.getString(tableIndex) == "bikes") {
                    assertEquals("SET NULL", cursor.getString(onDeleteIndex))
                    foundBikeForeignKey = true
                }
            }
            assertTrue(foundBikeForeignKey)
        }
        helper.close()
    }

    private fun assertActiveSwap(
        database: SupportSQLiteDatabase,
        componentId: Long,
        expectedBikeId: Long?,
    ) {
        database.query(
            "SELECT bikeId FROM component_swaps WHERE componentId = ? AND uninstalledAt IS NULL",
            arrayOf(componentId),
        ).use { cursor ->
            if (expectedBikeId == null) {
                assertEquals(0, cursor.count)
            } else {
                assertTrue(cursor.moveToFirst())
                assertEquals(expectedBikeId, cursor.getLong(0))
                assertEquals(1, cursor.count)
            }
        }
    }
}
