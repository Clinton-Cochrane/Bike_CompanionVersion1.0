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

    private companion object {
        const val TEST_DATABASE_NAME = "schema_fixture_test.db"
    }
}
