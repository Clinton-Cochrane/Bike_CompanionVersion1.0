package com.you.bikecompanion.data.ride

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Query("SELECT * FROM rides ORDER BY endedAt DESC")
    fun getAllRides(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE bikeId = :bikeId ORDER BY endedAt DESC")
    fun getRidesByBikeId(bikeId: Long): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE id = :id")
    suspend fun getRideById(id: Long): RideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ride: RideEntity): Long

    @Query("DELETE FROM rides WHERE id = :id")
    suspend fun deleteById(id: Long)
}
