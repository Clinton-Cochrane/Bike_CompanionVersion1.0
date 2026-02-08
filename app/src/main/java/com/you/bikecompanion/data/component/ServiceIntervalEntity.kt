package com.you.bikecompanion.data.component

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Type of service: replace, inspection, or grease. */
const val SERVICE_INTERVAL_TYPE_REPLACE = "replace"
const val SERVICE_INTERVAL_TYPE_INSPECTION = "inspection"
const val SERVICE_INTERVAL_TYPE_GREASE = "grease"
const val SERVICE_INTERVAL_TYPE_ON_FAILURE = "on_failure"

/**
 * Service interval for a component (e.g. "Max life", "Inspection", "Grease").
 * Health considers both distance and time: whichever limit is reached first.
 * - Distance: (intervalKm - trackedKm) / intervalKm when intervalKm > 0.
 * - Time: (intervalTimeSeconds - trackedTimeSeconds) / intervalTimeSeconds when intervalTimeSeconds != null.
 */
@Entity(
    tableName = "service_intervals",
    foreignKeys = [
        ForeignKey(
            entity = ComponentEntity::class,
            parentColumns = ["id"],
            childColumns = ["componentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("componentId")],
)
data class ServiceIntervalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val componentId: Long,
    val name: String,
    /** Interval distance in km. Use 0 when time-only (e.g. tubeless sealant top-up). */
    val intervalKm: Double,
    val trackedKm: Double = 0.0,
    val type: String = SERVICE_INTERVAL_TYPE_REPLACE,
    /** Interval time in seconds. Null when distance-only. */
    val intervalTimeSeconds: Long? = null,
    /** Tracked time in seconds. Null when not tracking time. */
    val trackedTimeSeconds: Long? = null,
)
