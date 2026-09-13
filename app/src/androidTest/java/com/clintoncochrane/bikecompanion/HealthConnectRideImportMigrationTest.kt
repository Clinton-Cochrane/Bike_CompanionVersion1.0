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
class HealthConnectRideImportMigrationTest {

    @Test
    fun migration13To14_addsRecordIdAndPreventsDuplicateProviderRecords() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext())
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(13) {
                    override fun onCreate(database: SupportSQLiteDatabase) {
                        database.execSQL(
                            "CREATE TABLE rides (id INTEGER PRIMARY KEY NOT NULL, distanceKm REAL NOT NULL)",
                        )
                    }

                    override fun onUpgrade(database: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        val database = helper.writableDatabase

        BikeCompanionMigrations.MIGRATION_13_14.migrate(database)
        database.execSQL("INSERT INTO rides (id, distanceKm, healthConnectRecordId) VALUES (1, 10.0, 'session-1')")
        val duplicate = runCatching {
            database.execSQL("INSERT INTO rides (id, distanceKm, healthConnectRecordId) VALUES (2, 10.0, 'session-1')")
        }

        assertTrue(duplicate.isFailure)
        database.query("SELECT healthConnectRecordId FROM rides WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("session-1", cursor.getString(0))
        }
        helper.close()
    }
}
