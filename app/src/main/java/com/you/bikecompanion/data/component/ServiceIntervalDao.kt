package com.you.bikecompanion.data.component

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceIntervalDao {
    @Query("SELECT * FROM service_intervals WHERE componentId = :componentId ORDER BY type, name")
    fun getIntervalsByComponentId(componentId: Long): Flow<List<ServiceIntervalEntity>>

    @Query("SELECT * FROM service_intervals WHERE componentId = :componentId ORDER BY type, name")
    suspend fun getIntervalsByComponentIdOnce(componentId: Long): List<ServiceIntervalEntity>

    @Query("SELECT * FROM service_intervals WHERE componentId IN (:componentIds)")
    suspend fun getIntervalsByComponentIdsOnce(componentIds: List<Long>): List<ServiceIntervalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(interval: ServiceIntervalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(intervals: List<ServiceIntervalEntity>)

    @Update
    suspend fun update(interval: ServiceIntervalEntity)

    @Query("DELETE FROM service_intervals WHERE id = :id")
    suspend fun deleteById(id: Long)
}
