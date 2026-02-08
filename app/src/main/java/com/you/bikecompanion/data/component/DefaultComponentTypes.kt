package com.you.bikecompanion.data.component

/**
 * Default component types offered when adding components to a bike or garage.
 * Lifespan values in km; used for health % and alerts.
 *
 * Includes freewheel (alternative to cassette) and granular options like brake pads/rotors.
 */
data class DefaultComponentType(
    val type: String,
    val displayName: String,
    val defaultLifespanKm: Double,
)

object DefaultComponentTypes {
    val SUGGESTED: List<DefaultComponentType> = listOf(
        // Drivetrain (lifespan from Replace intervals in DefaultServiceIntervalSpecs)
        DefaultComponentType("chain", "Chain", 3_500.0),
        DefaultComponentType("cassette", "Cassette", 10_000.0),
        DefaultComponentType("freewheel", "Freewheel", 10_000.0),
        DefaultComponentType("chainring", "Chainring(s)", 20_000.0),
        DefaultComponentType("bottom_bracket", "Bottom Bracket", 15_000.0),
        DefaultComponentType("cranks", "Cranks", 40_000.0),
        DefaultComponentType("pedals", "Pedals", 25_000.0),
        DefaultComponentType("front_derailleur", "Front Derailleur", 25_000.0),
        DefaultComponentType("rear_derailleur", "Rear Derailleur", 25_000.0),

        // Wheels & tires
        DefaultComponentType("tire", "Tire", 4_500.0),
        DefaultComponentType("front_wheel", "Front Wheel", 20_000.0),
        DefaultComponentType("rear_wheel", "Rear Wheel", 20_000.0),
        DefaultComponentType("tubeless_sealant", "Tubeless Sealant", 0.0),

        // Brakes
        DefaultComponentType("front_brake", "Front Brake", 5_000.0),
        DefaultComponentType("rear_brake", "Rear Brake", 5_000.0),
        DefaultComponentType("brake_pads", "Brake Pads", 2_000.0),
        DefaultComponentType("brake_rotor", "Brake Rotor", 15_000.0),
        DefaultComponentType("brake_cables", "Brake Cables / Housing", 6_000.0),
        DefaultComponentType("brake_fluid", "Brake Fluid", 12_000.0),

        // Cockpit
        DefaultComponentType("handlebars", "Handlebars", 50_000.0),
        DefaultComponentType("stem", "Stem", 50_000.0),
        DefaultComponentType("headset", "Headset", 15_000.0),
        DefaultComponentType("headset_bearings", "Headset Bearings", 15_000.0),
        DefaultComponentType("brake_levers", "Brake Levers", 50_000.0),
        DefaultComponentType("shift_levers", "Shift Levers", 40_000.0),
        DefaultComponentType("grips", "Grips", 5_000.0),

        // Frame
        DefaultComponentType("saddle", "Saddle", 25_000.0),
        DefaultComponentType("seat_post", "Seat Post", 30_000.0),
        DefaultComponentType("frame", "Bike Frame", 100_000.0),
        DefaultComponentType("fork", "Fork", 40_000.0),

        // Cables & power
        DefaultComponentType("shift_cables", "Shift Cables / Housing", 6_000.0),
        DefaultComponentType("battery", "Battery (Shifting / eBike)", 0.0),

        // Extra (manual-add: suspension, dropper, tubeless)
        DefaultComponentType("bar_ends", "Bar Ends", 50_000.0),
        DefaultComponentType("dropper_post", "Dropper Post", 30_000.0),
        DefaultComponentType("rear_shock", "Rear Shock", 30_000.0),
        DefaultComponentType("suspension_pivots", "Suspension Pivots", 10_000.0),
    )
}
