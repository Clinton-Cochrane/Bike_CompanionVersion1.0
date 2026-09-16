package com.clintoncochrane.bikecompanion.data

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/** Installs database-level guards that Room's schema annotations cannot express. */
object ComponentSwapIntegrity {
    fun install(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS component_swaps_one_active_install_insert
            BEFORE INSERT ON component_swaps
            WHEN NEW.uninstalledAt IS NULL AND EXISTS (
                SELECT 1 FROM component_swaps
                WHERE componentId = NEW.componentId AND uninstalledAt IS NULL
            )
            BEGIN
                SELECT RAISE(ABORT, 'component already has an active installation');
            END
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS component_swaps_one_active_install_update
            BEFORE UPDATE OF componentId, uninstalledAt ON component_swaps
            WHEN NEW.uninstalledAt IS NULL AND EXISTS (
                SELECT 1 FROM component_swaps
                WHERE componentId = NEW.componentId
                  AND uninstalledAt IS NULL
                  AND id != OLD.id
            )
            BEGIN
                SELECT RAISE(ABORT, 'component already has an active installation');
            END
            """.trimIndent(),
        )
    }
}

object BikeCompanionDatabaseCallback : RoomDatabase.Callback() {
    override fun onOpen(database: SupportSQLiteDatabase) {
        super.onOpen(database)
        ComponentSwapIntegrity.install(database)
    }
}
