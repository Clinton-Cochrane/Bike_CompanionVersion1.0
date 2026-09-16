package com.clintoncochrane.bikecompanion.data.bike

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bikes")
data class BikeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Optional human-readable display name, persisted in the legacy `name` column. */
    val name: String = "",
    val make: String = "",
    val model: String = "",
    val year: String = "",
    /** Odometer distance already on the bike before Bike Companion began tracking it. */
    val baselineDistanceKm: Double = 0.0,
    /** Starting baseline plus distance recorded by Bike Companion. */
    val totalDistanceKm: Double = 0.0,
    /** Total ride time in seconds, rolled up from completed trips. Denormalized for fast reads. */
    val totalTimeSeconds: Long = 0L,
    val lastRideAt: Long? = null,
    val description: String = "",
    val createdAt: Long,
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
    /** Drivetrain: "1x", "single_speed", "multi_speed", or "". */
    val drivetrainType: String = "",
    /** Brakes: "rim", "disc_mechanical", "disc_hydraulic", "coaster", "other", or "". */
    val brakeType: String = "",
    /** Free-form notes for the bike. */
    val notes: String = "",
)

val BikeEntity.displayName: String
    get() = name

/** Distance represented by persisted ride accounting rather than the starting odometer. */
val BikeEntity.recordedDistanceKm: Double
    get() = (totalDistanceKm - baselineDistanceKm).coerceAtLeast(0.0)

/** Returns this bike with a corrected baseline while preserving its recorded ride distance. */
fun BikeEntity.withBaselineDistanceKm(newBaselineDistanceKm: Double): BikeEntity {
    require(newBaselineDistanceKm.isFinite() && newBaselineDistanceKm >= 0.0) {
        "Bike baseline distance must be a non-negative finite value"
    }
    return copy(
        baselineDistanceKm = newBaselineDistanceKm,
        totalDistanceKm = recordedDistanceKm + newBaselineDistanceKm,
    )
}
