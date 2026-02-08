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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(components: List<ComponentEntity>)

    @Update
    suspend fun update(component: ComponentEntity)

    @Query("DELETE FROM components WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM components WHERE bikeId = :bikeId")
    suspend fun getComponentsByBikeIdOnce(bikeId: Long): List<ComponentEntity>

    @Query("SELECT COUNT(*) FROM components WHERE bikeId = :bikeId")
    suspend fun getComponentCountByBikeId(bikeId: Long): Int

    @Query("SELECT * FROM components WHERE bikeId IS NULL ORDER BY type, name")
    suspend fun getComponentsInGarageOnce(): List<ComponentEntity>

    @Query("SELECT * FROM components WHERE bikeId IS NULL ORDER BY type, name")
    fun getComponentsInGarage(): Flow<List<ComponentEntity>>

    @Query("SELECT * FROM components")
    suspend fun getAllComponents(): List<ComponentEntity>

    @Query("SELECT * FROM components ORDER BY type, name")
    fun getAllComponentsFlow(): Flow<List<ComponentEntity>>
}
