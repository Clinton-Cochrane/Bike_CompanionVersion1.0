package com.you.bikecompanion.data.bike

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bikes")
data class BikeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val totalDistanceKm: Double = 0.0,
    /** Total ride time in seconds, rolled up from completed trips. Denormalized for fast reads. */
    val totalTimeSeconds: Long = 0L,
    val lastRideAt: Long? = null,
    val description: String = "",
    val createdAt: Long,
    /** Thumbnail URI for display; null uses placeholder. */
    val thumbnailUri: String? = null,
    /** Average speed in km/h across all rides. Denormalized for fast reads. */
    val avgSpeedKmh: Double = 0.0,
    /** Max speed in km/h across all rides. Denormalized for fast reads. */
    val maxSpeedKmh: Double = 0.0,
    /** Total elevation gain in meters across all rides. */
    val totalElevGainM: Double = 0.0,
    /** Total elevation loss in meters across all rides. */
    val totalElevLossM: Double = 0.0,
    /**
     * Number of chain replacements on this bike. After 3 replacements, recommend
     * inspection/replacement of cassette, freewheel, and chainrings.
     */
    val chainReplacementCount: Int = 0,
)
