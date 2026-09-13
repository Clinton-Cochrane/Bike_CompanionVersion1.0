package com.clintoncochrane.bikecompanion.data.component

import androidx.room.withTransaction
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComponentLifecycleTransaction @Inject constructor(
    private val database: BikeCompanionDatabase,
) {
    suspend fun run(block: suspend () -> Unit) = database.withTransaction {
        block()
    }
}
