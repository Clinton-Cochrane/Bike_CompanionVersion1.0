package com.you.bikecompanion.data.component

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.you.bikecompanion.data.bike.BikeEntity

/**
 * Records when a component was installed on or uninstalled from a bike.
 * Null [uninstalledAt] means the component is currently installed on that bike.
 */
@Entity(
    tableName = "component_swaps",
    foreignKeys = [
        ForeignKey(
            entity = ComponentEntity::class,
            parentColumns = ["id"],
            childColumns = ["componentId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BikeEntity::class,
            parentColumns = ["id"],
            childColumns = ["bikeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("componentId"), Index("bikeId")],
)
data class ComponentSwapEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val componentId: Long,
    val bikeId: Long,
    val installedAt: Long,
    /** Null when component is currently installed. Set when uninstalled. */
    val uninstalledAt: Long? = null,
)
