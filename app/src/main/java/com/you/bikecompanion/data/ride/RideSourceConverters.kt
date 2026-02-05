package com.you.bikecompanion.data.ride

import androidx.room.TypeConverter

object RideSourceConverters {
    @TypeConverter
    @JvmStatic
    fun fromSource(source: RideSource): String = source.name

    @TypeConverter
    @JvmStatic
    fun toSource(value: String): RideSource = RideSource.entries.find { it.name == value } ?: RideSource.APP
}
