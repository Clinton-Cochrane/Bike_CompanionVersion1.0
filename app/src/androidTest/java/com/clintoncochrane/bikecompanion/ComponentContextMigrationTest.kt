package com.clintoncochrane.bikecompanion

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.BikeCompanionMigrations
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Ensures migration 3→4 creates the component_context table and the index
 * required by [com.clintoncochrane.bikecompanion.data.component.ComponentContextEntity].
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
    fun migration1ToLatest_preservesRepresentativeUserData(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dbFile.delete()
        createDatabaseAtVersion1(context)
        val sqlite = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        sqlite.execSQL("INSERT INTO bikes (id, name, totalDistanceKm, createdAt) VALUES (1, 'Original bike', 42.5, 1000)")
        sqlite.execSQL(
            "INSERT INTO rides (id, bikeId, distanceKm, durationMs, startedAt, endedAt) " +
                "VALUES (1, 1, 12.5, 3600000, 1000, 3601000)",
        )
        sqlite.execSQL(
            "INSERT INTO components (id, bikeId, type, name, lifespanKm, distanceUsedKm, installedAt) " +
                "VALUES (1, 1, 'chain', 'Original chain', 3000, 12.5, 1000)",
        )
        sqlite.close()

        db = Room.databaseBuilder(context, BikeCompanionDatabase::class.java, MIGRATION_TEST_DB_NAME)
            .addMigrations(*BikeCompanionMigrations.ALL)
            .allowMainThreadQueries()
            .build()

        assertEquals("Original bike", db.bikeDao().getAllBikes().first().single().name)
        assertEquals(12.5, db.rideDao().getRideById(1L)?.distanceKm ?: -1.0, 0.0)
        val component = db.componentDao().getComponentById(1L)
        assertEquals("Original chain", component?.name)
        assertEquals(12.5, component?.distanceUsedKm ?: -1.0, 0.0)
        val activeSwaps = db.componentSwapDao().getSwapsByComponentIdOnce(1L)
            .filter { it.uninstalledAt == null }
        assertEquals(1, activeSwaps.size)
        assertEquals(1L, activeSwaps.single().bikeId)
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

    @Test
    fun migration3ToLatest_addsZeroBikeBaselineWithoutChangingExistingTotal() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val legacyDb = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        legacyDb.execSQL(
            "INSERT INTO bikes (name, totalDistanceKm, createdAt) VALUES ('Existing bike', 321.5, 1000)",
        )
        legacyDb.close()

        db = Room.databaseBuilder(context, BikeCompanionDatabase::class.java, MIGRATION_TEST_DB_NAME)
            .addMigrations(*BikeCompanionMigrations.ALL)
            .build()
        val migratedDb = db.openHelper.writableDatabase

        migratedDb.query("SELECT totalDistanceKm, baselineDistanceKm FROM bikes").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(321.5, cursor.getDouble(0), 0.0)
            assertEquals(0.0, cursor.getDouble(1), 0.0)
        }
    }

    @Test
    fun migration3ToLatest_preservesTrackedDistanceAndDefaultsInstalledLifecycle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sqlite = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        sqlite.execSQL("INSERT INTO bikes (id, name, createdAt) VALUES (1, 'Bike', 0)")
        sqlite.execSQL(
            """
            INSERT INTO components (
                id, bikeId, type, name, lifespanKm, distanceUsedKm, installedAt
            ) VALUES (1, 1, 'chain', 'Chain', 3000.0, 42.5, 0)
            """.trimIndent(),
        )
        sqlite.close()

        db = Room.databaseBuilder(context, BikeCompanionDatabase::class.java, MIGRATION_TEST_DB_NAME)
            .addMigrations(*BikeCompanionMigrations.ALL)
            .allowMainThreadQueries()
            .build()

        val cursor = db.openHelper.writableDatabase.query(
            "SELECT distanceUsedKm, priorUsageCertainty, lifecycleStatus FROM components WHERE id = 1",
        )
        assertTrue(cursor.moveToFirst())
        assertTrue(cursor.getDouble(0) == 42.5)
        assertTrue(cursor.getString(1) == "UNKNOWN")
        assertTrue(cursor.getString(2) == "INSTALLED")
        cursor.close()
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

    private fun createDatabaseAtVersion1(context: Context) {
        val helper = object : SQLiteOpenHelper(context, MIGRATION_TEST_DB_NAME, null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE bikes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        make TEXT NOT NULL DEFAULT '',
                        model TEXT NOT NULL DEFAULT '',
                        year TEXT NOT NULL DEFAULT '',
                        totalDistanceKm REAL NOT NULL DEFAULT 0,
                        lastRideAt INTEGER,
                        description TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                createRidesTable(db)
                db.execSQL(
                    """
                    CREATE TABLE components (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bikeId INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        name TEXT NOT NULL,
                        makeModel TEXT NOT NULL DEFAULT '',
                        lifespanKm REAL NOT NULL,
                        distanceUsedKm REAL NOT NULL DEFAULT 0,
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
                db.execSQL("CREATE INDEX index_components_bikeId ON components(bikeId)")
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
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
