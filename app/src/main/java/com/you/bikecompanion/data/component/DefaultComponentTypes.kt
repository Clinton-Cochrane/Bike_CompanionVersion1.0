package com.you.bikecompanion.data.component

/**
 * Default component types offered when adding components to a bike.
 * Lifespan values are in km; used for health % and alerts.
 */
data class DefaultComponentType(
    val type: String,
    val defaultLifespanKm: Double,
)

object DefaultComponentTypes {
    val SUGGESTED: List<DefaultComponentType> = listOf(
        DefaultComponentType("chain", 5_000.0),
        DefaultComponentType("cassette", 8_000.0),
        DefaultComponentType("chainring", 10_000.0),
        DefaultComponentType("tires", 4_000.0),
        DefaultComponentType("brake_pads", 3_000.0),
        DefaultComponentType("bottom_bracket", 15_000.0),
        DefaultComponentType("cables", 10_000.0),
    )
}
