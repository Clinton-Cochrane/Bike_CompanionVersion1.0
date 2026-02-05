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
    val lastRideAt: Long? = null,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
