package com.you.bikecompanion.data.ride

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.you.bikecompanion.data.bike.BikeEntity

/**
 * Source of the ride: in-app GPS, Health Connect import, or manual entry.
 */
enum class RideSource {
    APP,
    HEALTH_CONNECT,
    MANUAL,
}

@Entity(
    tableName = "rides",
    foreignKeys = [
        ForeignKey(
            entity = BikeEntity::class,
            parentColumns = ["id"],
            childColumns = ["bikeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("bikeId")],
)
data class RideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bikeId: Long?,
    val distanceKm: Double,
    val durationMs: Long,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val elevGainM: Double = 0.0,
    val elevLossM: Double = 0.0,
    val startedAt: Long,
    val endedAt: Long,
    val source: RideSource = RideSource.APP,
)
