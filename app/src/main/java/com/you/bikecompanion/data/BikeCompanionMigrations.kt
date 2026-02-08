package com.you.bikecompanion.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Central place for all Room migrations so they can be reused in tests
 * (e.g. to run migration 3→4 and validate the resulting schema).
 */
object BikeCompanionMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE components ADD COLUMN position TEXT NOT NULL DEFAULT 'none'")
            db.execSQL("ALTER TABLE components ADD COLUMN baselineKm REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE components ADD COLUMN baselineTimeSeconds INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE bikes ADD COLUMN totalTimeSeconds INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE components ADD COLUMN totalTimeSeconds INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS component_context (
                    componentId INTEGER PRIMARY KEY NOT NULL,
                    notes TEXT NOT NULL DEFAULT '',
                    installDateMs INTEGER,
                    purchaseLink TEXT,
                    serialNumber TEXT,
                    lastServiceNotes TEXT,
                    FOREIGN KEY(componentId) REFERENCES components(id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_component_context_componentId ON component_context(componentId)",
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys=OFF")

            db.execSQL("ALTER TABLE bikes ADD COLUMN thumbnailUri TEXT")
            db.execSQL("ALTER TABLE bikes ADD COLUMN avgSpeedKmh REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE bikes ADD COLUMN maxSpeedKmh REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE bikes ADD COLUMN totalElevGainM REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE bikes ADD COLUMN totalElevLossM REAL NOT NULL DEFAULT 0")

            db.execSQL("ALTER TABLE component_context ADD COLUMN purchasePrice TEXT")
            db.execSQL("ALTER TABLE component_context ADD COLUMN purchaseDateMs INTEGER")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS components_new (
                    id INTEGER PRIMARY KEY NOT NULL,
                    bikeId INTEGER,
                    type TEXT NOT NULL,
                    name TEXT NOT NULL,
                    makeModel TEXT NOT NULL DEFAULT '',
                    lifespanKm REAL NOT NULL,
                    distanceUsedKm REAL NOT NULL DEFAULT 0,
                    totalTimeSeconds INTEGER NOT NULL DEFAULT 0,
                    position TEXT NOT NULL DEFAULT 'none',
                    baselineKm REAL NOT NULL DEFAULT 0,
                    baselineTimeSeconds INTEGER NOT NULL DEFAULT 0,
                    alertThresholdPercent INTEGER NOT NULL DEFAULT 10,
                    alertSnoozeUntilKm REAL,
                    alertSnoozeUntilTime INTEGER,
                    alertsEnabled INTEGER NOT NULL DEFAULT 1,
                    installedAt INTEGER NOT NULL,
                    notes TEXT NOT NULL DEFAULT '',
                    thumbnailUri TEXT,
                    avgSpeedKmh REAL NOT NULL DEFAULT 0,
                    maxSpeedKmh REAL NOT NULL DEFAULT 0,
                    maxSpeedBikeId INTEGER,
                    FOREIGN KEY(bikeId) REFERENCES bikes(id) ON DELETE SET NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO components_new (id, bikeId, type, name, makeModel, lifespanKm, distanceUsedKm,
                    totalTimeSeconds, position, baselineKm, baselineTimeSeconds, alertThresholdPercent,
                    alertSnoozeUntilKm, alertSnoozeUntilTime, alertsEnabled, installedAt, notes,
                    thumbnailUri, avgSpeedKmh, maxSpeedKmh, maxSpeedBikeId)
                SELECT id, bikeId, type, name, makeModel, lifespanKm, distanceUsedKm,
                    totalTimeSeconds, position, baselineKm, baselineTimeSeconds, alertThresholdPercent,
                    alertSnoozeUntilKm, alertSnoozeUntilTime, alertsEnabled, installedAt, notes,
                    NULL, 0, 0, NULL FROM components
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE components")
            db.execSQL("ALTER TABLE components_new RENAME TO components")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_components_bikeId ON components(bikeId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS component_swaps (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    componentId INTEGER NOT NULL,
                    bikeId INTEGER NOT NULL,
                    installedAt INTEGER NOT NULL,
                    uninstalledAt INTEGER,
                    FOREIGN KEY(componentId) REFERENCES components(id) ON DELETE CASCADE,
                    FOREIGN KEY(bikeId) REFERENCES bikes(id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_component_swaps_componentId ON component_swaps(componentId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS service_intervals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    componentId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    intervalKm REAL NOT NULL,
                    trackedKm REAL NOT NULL DEFAULT 0,
                    type TEXT NOT NULL DEFAULT 'replace',
                    FOREIGN KEY(componentId) REFERENCES components(id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_service_intervals_componentId ON service_intervals(componentId)")

            db.execSQL(
                """
                INSERT INTO service_intervals (componentId, name, intervalKm, trackedKm, type)
                SELECT id, 'Max life', lifespanKm, distanceUsedKm, 'replace' FROM components
                """.trimIndent(),
            )

            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_component_swaps_bikeId ON component_swaps(bikeId)")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE service_intervals ADD COLUMN intervalTimeSeconds INTEGER")
            db.execSQL("ALTER TABLE service_intervals ADD COLUMN trackedTimeSeconds INTEGER")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE bikes ADD COLUMN chainReplacementCount INTEGER NOT NULL DEFAULT 0")
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
}
