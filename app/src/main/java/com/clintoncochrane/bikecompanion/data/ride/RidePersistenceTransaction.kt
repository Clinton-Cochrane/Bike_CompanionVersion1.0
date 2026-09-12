package com.clintoncochrane.bikecompanion.data.ride

import androidx.room.withTransaction
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RidePersistenceTransaction @Inject constructor(
    private val database: BikeCompanionDatabase,
) {
    suspend fun run(block: suspend () -> Long?): Long? = database.withTransaction {
        block()
    }
}
