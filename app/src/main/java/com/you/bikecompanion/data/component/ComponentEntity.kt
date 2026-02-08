package com.you.bikecompanion.data.component

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.you.bikecompanion.data.bike.BikeEntity

@Entity(
    tableName = "components",
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
data class ComponentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Null when component is in garage (not installed on any bike). */
    val bikeId: Long?,
    /** Component type for display and grouping (e.g. chain, cassette, tires). */
    val type: String,
    val name: String,
    val makeModel: String = "",
    /** Expected lifespan in km. Used to compute health %. */
    val lifespanKm: Double,
    /** Distance used since install (or since last replacement). */
    val distanceUsedKm: Double = 0.0,
    /** Total ride time in seconds on this bike, rolled up from completed trips. Denormalized for fast reads. */
    val totalTimeSeconds: Long = 0L,
    /**
     * Position for display and filtering: "none", "front", or "rear".
     * Used for paired components (tires, brake pads, etc.).
     */
    val position: String = "none",
    /** Odometer/kilometer reading when component was installed (for future use). */
    val baselineKm: Double = 0.0,
    /** Time-based baseline when component was installed, in seconds (for future use). */
    val baselineTimeSeconds: Long = 0L,
    /** Alert when remaining % is at or below this (e.g. 10 = alert when 90% used). */
    val alertThresholdPercent: Int = 10,
    val alertSnoozeUntilKm: Double? = null,
    val alertSnoozeUntilTime: Long? = null,
    val alertsEnabled: Boolean = true,
    val installedAt: Long,
    val notes: String = "",
    /** Thumbnail URI for display; null uses type icon. */
    val thumbnailUri: String? = null,
    /** Average speed in km/h across all rides. Denormalized for fast reads. */
    val avgSpeedKmh: Double = 0.0,
    /** Max speed in km/h across all rides. */
    val maxSpeedKmh: Double = 0.0,
    /** Bike id that achieved max speed; null if none. */
    val maxSpeedBikeId: Long? = null,
)
