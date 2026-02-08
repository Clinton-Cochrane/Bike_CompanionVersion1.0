package com.you.bikecompanion.data.bike

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BikeDao {
    @Query("SELECT * FROM bikes ORDER BY name COLLATE NOCASE")
    fun getAllBikes(): Flow<List<BikeEntity>>

    @Query("SELECT COUNT(*) FROM bikes")
    fun getBikeCount(): Flow<Int>

    @Query("SELECT * FROM bikes WHERE id = :id")
    suspend fun getBikeById(id: Long): BikeEntity?

    @Query("SELECT * FROM bikes ORDER BY lastRideAt IS NULL ASC, lastRideAt DESC LIMIT 1")
    suspend fun getMostRecentlyRiddenBike(): BikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bike: BikeEntity): Long

    @Update
    suspend fun update(bike: BikeEntity)

    @Query("DELETE FROM bikes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
