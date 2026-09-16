package com.clintoncochrane.bikecompanion

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.clintoncochrane.bikecompanion.data.BikeCompanionMigrations
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServiceIntervalResetMigrationTest {

    @Test
    fun migration16To17_addsNullableCompletionBoundary() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext())
                .name(null)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(16) {
                        override fun onCreate(database: SupportSQLiteDatabase) {
                            database.execSQL(
                                "CREATE TABLE service_intervals (id INTEGER PRIMARY KEY NOT NULL, componentId INTEGER NOT NULL, name TEXT NOT NULL, intervalKm REAL NOT NULL, trackedKm REAL NOT NULL, type TEXT NOT NULL, intervalTimeSeconds INTEGER, trackedTimeSeconds INTEGER)",
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

        BikeCompanionMigrations.MIGRATION_16_17.migrate(database)

        database.query("PRAGMA table_info(service_intervals)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            assertTrue(generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
                .contains("lastCompletedAt"))
        }
        helper.close()
    }
}
