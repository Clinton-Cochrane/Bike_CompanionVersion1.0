package com.clintoncochrane.bikecompanion.data.component

import androidx.room.TypeConverter

/** Describes whether a component is installed, reusable in the garage, or historical only. */
enum class ComponentLifecycleStatus {
    INSTALLED,
    IN_GARAGE,
    RETIRED,
}

class ComponentLifecycleStatusConverters {
    @TypeConverter
    fun fromStatus(status: ComponentLifecycleStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ComponentLifecycleStatus = ComponentLifecycleStatus.valueOf(value)
}
