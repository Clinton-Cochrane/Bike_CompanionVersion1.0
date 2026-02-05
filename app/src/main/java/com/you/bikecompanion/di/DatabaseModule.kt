package com.you.bikecompanion.di

import android.content.Context
import androidx.room.Room
import com.you.bikecompanion.data.BikeCompanionDatabase
import com.you.bikecompanion.data.bike.BikeDao
import com.you.bikecompanion.data.component.ComponentDao
import com.you.bikecompanion.data.ride.RideDao
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
    ).build()

    @Provides
    @Singleton
    fun provideBikeDao(db: BikeCompanionDatabase): BikeDao = db.bikeDao()

    @Provides
    @Singleton
    fun provideRideDao(db: BikeCompanionDatabase): RideDao = db.rideDao()

    @Provides
    @Singleton
    fun provideComponentDao(db: BikeCompanionDatabase): ComponentDao = db.componentDao()
}
