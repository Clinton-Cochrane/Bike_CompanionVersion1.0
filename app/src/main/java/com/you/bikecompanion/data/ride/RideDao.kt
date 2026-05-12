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

    /** Max end time among rides for this bike excluding ids about to be deleted. */
    @Query(
        "SELECT MAX(endedAt) FROM rides WHERE bikeId = :bikeId AND id NOT IN (:excludeIds)",
    )
    suspend fun getMaxEndedAtExcluding(bikeId: Long, excludeIds: List<Long>): Long?

    /** Max recorded max speed among remaining rides for this bike. */
    @Query(
        "SELECT MAX(maxSpeedKmh) FROM rides WHERE bikeId = :bikeId AND id NOT IN (:excludeIds)",
    )
    suspend fun getMaxMaxSpeedKmhExcluding(bikeId: Long, excludeIds: List<Long>): Double?
}
