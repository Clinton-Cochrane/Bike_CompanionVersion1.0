package com.you.bikecompanion

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.you.bikecompanion.data.BikeCompanionDatabase
import com.you.bikecompanion.data.BikeCompanionMigrations
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Ensures migration 3→4 creates the component_context table and the index
 * required by [com.you.bikecompanion.data.component.ComponentContextEntity].
 * Without the index, Room's post-migration validation throws IllegalStateException on app load.
 */
@RunWith(AndroidJUnit4::class)
class ComponentContextMigrationTest {

    private lateinit var dbFile: File
    private lateinit var db: BikeCompanionDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dbFile = context.getDatabasePath(MIGRATION_TEST_DB_NAME)
        if (dbFile.exists()) dbFile.delete()
        createDatabaseAtVersion3(context)
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
        if (::dbFile.isInitialized && dbFile.exists()) dbFile.delete()
    }

    @Test
    fun migration3To4_producesSchemaWithComponentContextIndex() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.databaseBuilder(context, BikeCompanionDatabase::class.java, MIGRATION_TEST_DB_NAME)
            .addMigrations(*BikeCompanionMigrations.ALL)
            .build()
        db.openHelper.writableDatabase
        db.componentContextDao()
        assertTrue("DB opened and componentContextDao() succeeded; schema validated by Room", true)
    }

    /**
     * Creates a database file at version 3 with bikes, rides, and components tables
     * so that opening it with Room triggers migration 3→4 and schema validation.
     */
    private fun createDatabaseAtVersion3(context: Context) {
        val helper = object : SQLiteOpenHelper(context, MIGRATION_TEST_DB_NAME, null, 3) {
            override fun onCreate(db: SQLiteDatabase) {
                createBikesTable(db)
                createRidesTable(db)
                createComponentsTable(db)
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }
        helper.writableDatabase.close()
    }

    private fun createBikesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS bikes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                make TEXT NOT NULL DEFAULT '',
                model TEXT NOT NULL DEFAULT '',
                year TEXT NOT NULL DEFAULT '',
                totalDistanceKm REAL NOT NULL DEFAULT 0.0,
                totalTimeSeconds INTEGER NOT NULL DEFAULT 0,
                lastRideAt INTEGER,
                description TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    private fun createRidesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS rides (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                bikeId INTEGER,
                distanceKm REAL NOT NULL,
                durationMs INTEGER NOT NULL,
                avgSpeedKmh REAL NOT NULL DEFAULT 0.0,
                maxSpeedKmh REAL NOT NULL DEFAULT 0.0,
                elevGainM REAL NOT NULL DEFAULT 0.0,
                elevLossM REAL NOT NULL DEFAULT 0.0,
                startedAt INTEGER NOT NULL,
                endedAt INTEGER NOT NULL,
                source TEXT NOT NULL DEFAULT 'APP',
                FOREIGN KEY(bikeId) REFERENCES bikes(id) ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_rides_bikeId ON rides(bikeId)")
    }

    private fun createComponentsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS components (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                bikeId INTEGER NOT NULL,
                type TEXT NOT NULL,
                name TEXT NOT NULL,
                makeModel TEXT NOT NULL DEFAULT '',
                lifespanKm REAL NOT NULL,
                distanceUsedKm REAL NOT NULL DEFAULT 0.0,
                totalTimeSeconds INTEGER NOT NULL DEFAULT 0,
                position TEXT NOT NULL DEFAULT 'none',
                baselineKm REAL NOT NULL DEFAULT 0.0,
                baselineTimeSeconds INTEGER NOT NULL DEFAULT 0,
                alertThresholdPercent INTEGER NOT NULL DEFAULT 10,
                alertSnoozeUntilKm REAL,
                alertSnoozeUntilTime INTEGER,
                alertsEnabled INTEGER NOT NULL DEFAULT 1,
                installedAt INTEGER NOT NULL,
                notes TEXT NOT NULL DEFAULT '',
                FOREIGN KEY(bikeId) REFERENCES bikes(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_components_bikeId ON components(bikeId)")
    }

    companion object {
        private const val MIGRATION_TEST_DB_NAME = "component_context_migration_test.db"
    }
}
