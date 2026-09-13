package com.clintoncochrane.bikecompanion.data.component

import androidx.room.TypeConverter

enum class PriorUsageCertainty {
    KNOWN,
    APPROXIMATE,
    UNKNOWN,
}

object PriorUsageCertaintyConverters {
    @TypeConverter
    fun fromPriorUsageCertainty(value: PriorUsageCertainty): String = value.name

    @TypeConverter
    fun toPriorUsageCertainty(value: String): PriorUsageCertainty =
        PriorUsageCertainty.valueOf(value)
}
