package com.you.bikecompanion.data.component

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Extra context for a component instance (notes, install date, purchase link, etc.).
 * One-to-one with [ComponentEntity]; scoped per bike component instance.
 */
@Entity(
    tableName = "component_context",
    primaryKeys = ["componentId"],
    indices = [Index("componentId")],
    foreignKeys = [
        ForeignKey(
            entity = ComponentEntity::class,
            parentColumns = ["id"],
            childColumns = ["componentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ComponentContextEntity(
    val componentId: Long,
    /** User notes; required to have at least one character when saving. */
    val notes: String,
    /** Install date as epoch millis, or null if not set. */
    val installDateMs: Long? = null,
    /** Purchase/product URL; must be valid URL if present. */
    val purchaseLink: String? = null,
    val serialNumber: String? = null,
    /** Last service notes (multiline). */
    val lastServiceNotes: String? = null,
    /** Purchase price as user-typed (e.g. "$125"). */
    val purchasePrice: String? = null,
    /** Purchase date as epoch millis, or null if not set. */
    val purchaseDateMs: Long? = null,
)
