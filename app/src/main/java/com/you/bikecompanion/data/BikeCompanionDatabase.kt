package com.you.bikecompanion.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.you.bikecompanion.data.bike.BikeDao
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.component.ComponentDao
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.ride.RideDao
import com.you.bikecompanion.data.ride.RideEntity
import com.you.bikecompanion.data.ride.RideSourceConverters

/**
 * Local database for the app. Named explicitly so "where does data go?" is obvious.
 * File on disk: bike_companion.db (see [androidx.room.Room.databaseBuilder]).
 */
@Database(
    entities = [
        BikeEntity::class,
        RideEntity::class,
        ComponentEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RideSourceConverters::class)
abstract class BikeCompanionDatabase : RoomDatabase() {
    abstract fun bikeDao(): BikeDao
    abstract fun rideDao(): RideDao
    abstract fun componentDao(): ComponentDao
}
