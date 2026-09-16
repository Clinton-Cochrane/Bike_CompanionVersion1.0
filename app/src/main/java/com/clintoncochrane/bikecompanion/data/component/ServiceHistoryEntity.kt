package com.clintoncochrane.bikecompanion.data.component

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Immutable snapshot of one completed service requirement. */
@Entity(
    tableName = "service_history",
    indices = [
        Index(value = ["sessionId", "serviceIntervalId"], unique = true),
        Index("componentId"),
        Index("bikeId"),
    ],
)
data class ServiceHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val serviceIntervalId: Long?,
    val serviceName: String,
    val serviceType: String,
    val componentId: Long,
    val replacementComponentId: Long? = null,
    val bikeId: Long,
    val completedAt: Long,
    val bikeOdometerKm: Double,
)
