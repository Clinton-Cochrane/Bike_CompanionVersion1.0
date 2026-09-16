package com.clintoncochrane.bikecompanion

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.BikeCompanionMigrations
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseSchemaFixtureTest {

    @get:Rule
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BikeCompanionDatabase::class.java,
    )

    @After
    fun tearDown() {
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(TEST_DATABASE_NAME)
    }

    @Test
    fun schemaVersion12_opensAndValidatesAgainstCommittedFixture() {
        migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 12).close()

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BikeCompanionDatabase::class.java,
            TEST_DATABASE_NAME,
        ).addMigrations(*BikeCompanionMigrations.ALL)
            .build()
        try {
            database.openHelper.writableDatabase
        } finally {
            database.close()
        }
    }

    @Test
    fun everyCommittedSchemaFixture_opensAndMigratesToLatest() {
        (12..21).forEach { version ->
            val databaseName = "schema_fixture_v$version.db"
            migrationTestHelper.createDatabase(databaseName, version).close()

            val database = Room.databaseBuilder(
                ApplicationProvider.getApplicationContext(),
                BikeCompanionDatabase::class.java,
                databaseName,
            ).addMigrations(*BikeCompanionMigrations.ALL)
                .build()
            try {
                database.openHelper.writableDatabase
            } finally {
                database.close()
                ApplicationProvider.getApplicationContext<Context>().deleteDatabase(databaseName)
            }
        }
    }

    private companion object {
        const val TEST_DATABASE_NAME = "schema_fixture_test.db"
    }
}
