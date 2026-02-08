package com.you.bikecompanion.data.component

/**
 * Static list of default parts to seed when a new bike is created.
 * Kept in one place so it can be replaced later (e.g. AI or DB lookup by bike type).
 *
 * Each entry defines type, display name, position (none/front/rear), and default lifespan in km.
 * Seeded instances get baselineKm = 0 and baselineTimeSeconds = 0.
 * Names use "Default " + item type/description so they are easy to identify and cheap to access
 * when creating new bikes; later we can allow users to edit default definitions.
 *
 * Aligned with [ComponentCategory.defaultBikeCategories]: cockpit, frame, drivetrain, wheels, brakes, cables only.
 * POWER (e.g. battery) and OTHER are not seeded; can be added when we know more (e.g. eBike, single-speed, 1x).
 *
 * Order: cockpit, frame, drivetrain, wheels, brakes, cables.
 * Cassette seeded by default; freewheel/single-speed can be handled in a future bike-type flow.
 */
data class DefaultSeedComponent(
    val type: String,
    val name: String,
    val position: String,
    val defaultLifespanKm: Double,
)

object DefaultSeedComponents {
    /** Default parts to create for every new bike (basic bike: tubes, mechanical brakes, no tubeless/suspension). */
    val LIST: List<DefaultSeedComponent> = listOf(
        // Cockpit
        DefaultSeedComponent("handlebars", "Default handlebars", "none", 50_000.0),
        DefaultSeedComponent("stem", "Default stem", "none", 50_000.0),
        DefaultSeedComponent("headset", "Default headset", "none", 15_000.0),
        DefaultSeedComponent("headset_bearings", "Default headset_bearings", "none", 15_000.0),
        DefaultSeedComponent("brake_levers", "Default brake_levers (front)", "front", 50_000.0),
        DefaultSeedComponent("brake_levers", "Default brake_levers (rear)", "rear", 50_000.0),
        DefaultSeedComponent("shift_levers", "Default shift_levers (front)", "front", 40_000.0),
        DefaultSeedComponent("shift_levers", "Default shift_levers (rear)", "rear", 40_000.0),
        DefaultSeedComponent("bar_ends", "Default bar_ends", "none", 50_000.0),
        DefaultSeedComponent("grips", "Default grips", "none", 5_000.0),

        // Frame (includes fork)
        DefaultSeedComponent("frame", "Default frame", "none", 100_000.0),
        DefaultSeedComponent("fork", "Default fork", "none", 40_000.0),
        DefaultSeedComponent("seat_post", "Default seat post", "none", 30_000.0),
        DefaultSeedComponent("saddle", "Default saddle", "none", 25_000.0),

        // Drivetrain
        DefaultSeedComponent("cranks", "Default cranks", "none", 40_000.0),
        DefaultSeedComponent("chainring", "Default chainring", "none", 20_000.0),
        DefaultSeedComponent("chain", "Default chain", "none", 3_500.0),
        DefaultSeedComponent("front_derailleur", "Default front derailleur", "front", 25_000.0),
        DefaultSeedComponent("rear_derailleur", "Default rear derailleur", "rear", 25_000.0),
        DefaultSeedComponent("bottom_bracket", "Default bottom bracket", "none", 15_000.0),
        DefaultSeedComponent("pedals", "Default pedals", "none", 25_000.0),
        DefaultSeedComponent("cassette", "Default cassette", "none", 10_000.0),

        // Wheels: front (hub, spokes, tire, tube, rim)
        DefaultSeedComponent("hub", "Default hub (front)", "front", 50_000.0),
        DefaultSeedComponent("spokes", "Default spokes (front)", "front", 30_000.0),
        DefaultSeedComponent("tire", "Default tire (front)", "front", 4_500.0),
        DefaultSeedComponent("tube", "Default tube (front)", "front", 5_000.0),
        DefaultSeedComponent("rim", "Default rim (front)", "front", 40_000.0),
        // Wheels: rear
        DefaultSeedComponent("hub", "Default hub (rear)", "rear", 50_000.0),
        DefaultSeedComponent("spokes", "Default spokes (rear)", "rear", 30_000.0),
        DefaultSeedComponent("tire", "Default tire (rear)", "rear", 4_500.0),
        DefaultSeedComponent("tube", "Default tube (rear)", "rear", 5_000.0),
        DefaultSeedComponent("rim", "Default rim (rear)", "rear", 40_000.0),

        // Brakes: front and rear (caliper, pads, rotor)
        DefaultSeedComponent("brake_caliper", "Default brake caliper (front)", "front", 50_000.0),
        DefaultSeedComponent("brake_pads", "Default brake pads (front)", "front", 2_000.0),
        DefaultSeedComponent("brake_rotor", "Default brake rotor (front)", "front", 15_000.0),
        DefaultSeedComponent("brake_caliper", "Default brake caliper (rear)", "rear", 50_000.0),
        DefaultSeedComponent("brake_pads", "Default brake pads (rear)", "rear", 2_000.0),
        DefaultSeedComponent("brake_rotor", "Default brake rotor (rear)", "rear", 15_000.0),

        // Cables
        DefaultSeedComponent("cable_front_derailleur", "Default cable front derailleur", "none", 6_000.0),
        DefaultSeedComponent("cable_rear_derailleur", "Default cable rear derailleur", "none", 6_000.0),
        DefaultSeedComponent("cable_front_brake", "Default cable front brake", "none", 6_000.0),
        DefaultSeedComponent("cable_rear_brake", "Default cable rear brake", "none", 6_000.0),
        DefaultSeedComponent("cable_seat_dropper", "Default cable seat dropper", "none", 8_000.0),
    )

    /** Lookup by type for cheap access when creating new bikes (e.g. filter or extend defaults by type). */
    private val listByType: Map<String, List<DefaultSeedComponent>> = LIST.groupBy { it.type }

    /** Returns all default seed components for a given type, or empty list if none. */
    fun byType(type: String): List<DefaultSeedComponent> = listByType[type] ?: emptyList()
}
