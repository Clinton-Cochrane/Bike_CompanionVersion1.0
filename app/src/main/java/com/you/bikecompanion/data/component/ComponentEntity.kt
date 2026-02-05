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
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bikeId")],
)
data class ComponentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bikeId: Long,
    /** Component type for display and grouping (e.g. chain, cassette, tires). */
    val type: String,
    val name: String,
    val makeModel: String = "",
    /** Expected lifespan in km. Used to compute health %. */
    val lifespanKm: Double,
    /** Distance used since install (or since last replacement). */
    val distanceUsedKm: Double = 0.0,
    /** Alert when remaining % is at or below this (e.g. 10 = alert when 90% used). */
    val alertThresholdPercent: Int = 10,
    val alertSnoozeUntilKm: Double? = null,
    val alertSnoozeUntilTime: Long? = null,
    val alertsEnabled: Boolean = true,
    val installedAt: Long = System.currentTimeMillis(),
    val notes: String = "",
)
