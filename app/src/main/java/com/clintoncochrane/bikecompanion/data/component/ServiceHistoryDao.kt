package com.clintoncochrane.bikecompanion.data.component

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ServiceHistoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(history: ServiceHistoryEntity): Long

    @Query(
        "SELECT * FROM service_history " +
            "WHERE sessionId = :sessionId AND serviceIntervalId = :serviceIntervalId LIMIT 1",
    )
    suspend fun getBySessionAndInterval(sessionId: String, serviceIntervalId: Long): ServiceHistoryEntity?

    @Query("SELECT * FROM service_history WHERE sessionId = :sessionId ORDER BY id")
    suspend fun getBySessionId(sessionId: String): List<ServiceHistoryEntity>
}
