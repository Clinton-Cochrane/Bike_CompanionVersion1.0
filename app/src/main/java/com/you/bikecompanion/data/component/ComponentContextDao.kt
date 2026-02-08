package com.you.bikecompanion.data.component

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ComponentContextDao {

    @Query("SELECT * FROM component_context WHERE componentId = :componentId")
    suspend fun getByComponentId(componentId: Long): ComponentContextEntity?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ComponentContextEntity)

    @Update
    suspend fun update(entity: ComponentContextEntity)

    /** Insert or replace context for a component (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ComponentContextEntity)
}
