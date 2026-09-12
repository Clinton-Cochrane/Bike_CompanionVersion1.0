package com.clintoncochrane.bikecompanion.di

import android.content.Context
import androidx.room.Room
import com.clintoncochrane.bikecompanion.data.BikeCompanionDatabase
import com.clintoncochrane.bikecompanion.data.BikeCompanionMigrations
import com.clintoncochrane.bikecompanion.data.bike.BikeDao
import com.clintoncochrane.bikecompanion.data.component.ComponentContextDao
import com.clintoncochrane.bikecompanion.data.component.ComponentDao
import com.clintoncochrane.bikecompanion.data.component.ComponentSwapDao
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalDao
import com.clintoncochrane.bikecompanion.data.ride.RideDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "bike_companion.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideBikeCompanionDatabase(
        @ApplicationContext context: Context,
    ): BikeCompanionDatabase = Room.databaseBuilder(
        context,
        BikeCompanionDatabase::class.java,
        DATABASE_NAME,
    ).addMigrations(*BikeCompanionMigrations.ALL)
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun provideBikeDao(db: BikeCompanionDatabase): BikeDao = db.bikeDao()

    @Provides
    @Singleton
    fun provideRideDao(db: BikeCompanionDatabase): RideDao = db.rideDao()

    @Provides
    @Singleton
    fun provideComponentDao(db: BikeCompanionDatabase): ComponentDao = db.componentDao()

    @Provides
    @Singleton
    fun provideComponentContextDao(db: BikeCompanionDatabase): ComponentContextDao = db.componentContextDao()

    @Provides
    @Singleton
    fun provideComponentSwapDao(db: BikeCompanionDatabase): ComponentSwapDao = db.componentSwapDao()

    @Provides
    @Singleton
    fun provideServiceIntervalDao(db: BikeCompanionDatabase): ServiceIntervalDao = db.serviceIntervalDao()
}
