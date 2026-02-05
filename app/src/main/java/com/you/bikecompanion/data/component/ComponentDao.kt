package com.you.bikecompanion.data.component

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ComponentDao {
    @Query("SELECT * FROM components WHERE bikeId = :bikeId ORDER BY type, name")
    fun getComponentsByBikeId(bikeId: Long): Flow<List<ComponentEntity>>

    @Query("SELECT * FROM components WHERE id = :id")
    suspend fun getComponentById(id: Long): ComponentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(component: ComponentEntity): Long

    @Update
    suspend fun update(component: ComponentEntity)

    @Query("DELETE FROM components WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM components WHERE bikeId = :bikeId")
    suspend fun getComponentsByBikeIdOnce(bikeId: Long): List<ComponentEntity>

    @Query("SELECT * FROM components")
    suspend fun getAllComponents(): List<ComponentEntity>
}
