package com.you.bikecompanion.data.component

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ComponentSwapDao {
    @Query("SELECT * FROM component_swaps WHERE componentId = :componentId ORDER BY installedAt DESC")
    fun getSwapsByComponentId(componentId: Long): Flow<List<ComponentSwapEntity>>

    @Query("SELECT * FROM component_swaps WHERE componentId = :componentId ORDER BY installedAt DESC")
    suspend fun getSwapsByComponentIdOnce(componentId: Long): List<ComponentSwapEntity>

    @Insert
    suspend fun insert(swap: ComponentSwapEntity): Long

    @Update
    suspend fun update(swap: ComponentSwapEntity)

    @Query("SELECT * FROM component_swaps WHERE componentId = :componentId AND uninstalledAt IS NULL LIMIT 1")
    suspend fun getCurrentSwap(componentId: Long): ComponentSwapEntity?
}
