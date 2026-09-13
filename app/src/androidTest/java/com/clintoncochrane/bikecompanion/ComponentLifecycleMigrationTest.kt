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
class ComponentLifecycleMigrationTest {

    @Test
    fun migration12To13_setsLifecycleAndClosesDuplicateActiveSwaps() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(
                ApplicationProvider.getApplicationContext(),
            ).name(null).callback(
                object : SupportSQLiteOpenHelper.Callback(12) {
                    override fun onCreate(database: SupportSQLiteDatabase) {
                        createVersion12Tables(database)
                    }

                    override fun onUpgrade(
                        database: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                },
            ).build(),
        )
        val database = helper.writableDatabase
        database.execSQL("INSERT INTO components VALUES (1, 10, 'chain', 'Installed', 3000, 25, 1)")
        database.execSQL("INSERT INTO components VALUES (2, NULL, 'chain', 'Garage', 3000, 25, 1)")
        database.execSQL("INSERT INTO component_swaps VALUES (1, 1, 10, 1, NULL)")
        database.execSQL("INSERT INTO component_swaps VALUES (2, 1, 10, 2, NULL)")

        BikeCompanionMigrations.MIGRATION_12_13.migrate(database)

        database.query("SELECT lifecycleStatus FROM components WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("INSTALLED", cursor.getString(0))
        }
        database.query("SELECT lifecycleStatus FROM components WHERE id = 2").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("IN_GARAGE", cursor.getString(0))
        }
        database.query(
            "SELECT COUNT(*) FROM component_swaps WHERE componentId = 1 AND uninstalledAt IS NULL",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        helper.close()
    }

    private fun createVersion12Tables(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE components (
                id INTEGER PRIMARY KEY NOT NULL,
                bikeId INTEGER,
                type TEXT NOT NULL,
                name TEXT NOT NULL,
                lifespanKm REAL NOT NULL,
                distanceUsedKm REAL NOT NULL,
                installedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE component_swaps (
                id INTEGER PRIMARY KEY NOT NULL,
                componentId INTEGER NOT NULL,
                bikeId INTEGER NOT NULL,
                installedAt INTEGER NOT NULL,
                uninstalledAt INTEGER
            )
            """.trimIndent(),
        )
    }
}
