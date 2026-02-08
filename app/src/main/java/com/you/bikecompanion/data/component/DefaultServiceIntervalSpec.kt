package com.you.bikecompanion.data.component

import com.you.bikecompanion.util.IntervalTimeConstants as T

/**
 * Static specification for default service intervals by component type.
 * Used when creating new components (auto-seed or manual add) to populate
 * [ServiceIntervalEntity] rows with industry-standard maintenance schedules.
 *
 * Skips "Every Ride" (future pre-ride splash) and "On Failure" (no scheduled interval).
 */
data class DefaultServiceIntervalSpec(
    val type: String,
    val serviceName: String,
    val intervalKm: Double,
    val intervalTimeSeconds: Long? = null,
    val intervalType: String = SERVICE_INTERVAL_TYPE_REPLACE,
    val notes: String? = null,
)

object DefaultServiceIntervalSpecs {
    private val specs: List<DefaultServiceIntervalSpec> = listOf(
        // Drivetrain
        DefaultServiceIntervalSpec("chain", "Inspect / Clean / Lube", 250.0, T.TWO_WEEKS, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("chain", "Replace", 3_500.0, null, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("cassette", "Clean", 500.0, T.ONE_MONTH, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("cassette", "Replace", 10_000.0, null, SERVICE_INTERVAL_TYPE_REPLACE, "recommended every 4th chain"),
        DefaultServiceIntervalSpec("freewheel", "Replace", 10_000.0, null, SERVICE_INTERVAL_TYPE_REPLACE, "recommended every 4th chain"),
        DefaultServiceIntervalSpec("chainring", "Replace", 20_000.0, null, SERVICE_INTERVAL_TYPE_REPLACE, "recommended every 4th chain"),
        DefaultServiceIntervalSpec("bottom_bracket", "Inspect / Clean", 2_000.0, T.SIX_MONTHS, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("bottom_bracket", "Replace", 15_000.0, null, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("cranks", "Inspect (Torque check)", 5_000.0, T.TWELVE_MONTHS, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("pedals", "Service (Grease)", 5_000.0, T.TWELVE_MONTHS, SERVICE_INTERVAL_TYPE_GREASE),
        DefaultServiceIntervalSpec("front_derailleur", "Inspect / Clean", 500.0, T.ONE_MONTH, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("rear_derailleur", "Inspect / Clean (Pulleys)", 500.0, T.ONE_MONTH, SERVICE_INTERVAL_TYPE_INSPECTION),

        // Wheels & Tires (skip Tire Inspect Every Ride, Tube On Failure)
        DefaultServiceIntervalSpec("tire", "Replace", 4_500.0, T.TWENTY_FOUR_MONTHS, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("tubeless_sealant", "Top-up", 0.0, T.THREE_MONTHS, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("front_wheel", "True / Tension", 2_000.0, T.SIX_MONTHS, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("rear_wheel", "True / Tension", 2_000.0, T.SIX_MONTHS, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("hub", "Service (Bearings)", 10_000.0, T.TWELVE_MONTHS, SERVICE_INTERVAL_TYPE_GREASE),
        DefaultServiceIntervalSpec("spokes", "Inspect (Tension)", 2_000.0, T.SIX_MONTHS, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("rim", "Inspect (Wear/Cracks)", 5_000.0, T.TWELVE_MONTHS, SERVICE_INTERVAL_TYPE_INSPECTION),

        // Brakes
        DefaultServiceIntervalSpec("brake_pads", "Inspect", 500.0, T.ONE_MONTH, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("brake_pads", "Replace", 2_000.0, null, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("brake_rotor", "Inspect (Thickness/True)", 1_000.0, T.THREE_MONTHS, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("brake_rotor", "Replace", 15_000.0, null, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("brake_caliper", "Clean / Piston Lube", 5_000.0, T.TWELVE_MONTHS, SERVICE_INTERVAL_TYPE_GREASE),
        DefaultServiceIntervalSpec("brake_fluid", "Bleed / Flush", 5_000.0, T.TWELVE_MONTHS, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("brake_cables", "Replace Housing/Wire", 6_000.0, T.EIGHTEEN_MONTHS, SERVICE_INTERVAL_TYPE_REPLACE),

        // Cockpit
        DefaultServiceIntervalSpec("handlebars", "Inspect (Fatigue/Cracks)", 5_000.0, T.TWELVE_MONTHS, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("stem", "Inspect (Torque)", 2_000.0, T.SIX_MONTHS, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("headset", "Inspect (Play)", 1_000.0, T.THREE_MONTHS, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("headset_bearings", "Service (Grease)", 5_000.0, T.TWELVE_MONTHS, SERVICE_INTERVAL_TYPE_GREASE),
        DefaultServiceIntervalSpec("headset_bearings", "Replace", 15_000.0, null, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("grips", "Replace", 5_000.0, T.TWELVE_MONTHS, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("shift_levers", "Flush / Lube", 10_000.0, T.TWENTY_FOUR_MONTHS, SERVICE_INTERVAL_TYPE_GREASE),

        // Frame & Seating
        DefaultServiceIntervalSpec("frame", "Inspect (Cracks/Damage)", 1_000.0, T.THREE_MONTHS, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("fork", "Lower Leg Service", 1_000.0, T.FIFTY_HOURS, SERVICE_INTERVAL_TYPE_GREASE),
        DefaultServiceIntervalSpec("fork", "Full Rebuild", 4_000.0, T.TWO_HUNDRED_HOURS, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("rear_shock", "Air Can Service", 1_000.0, T.FIFTY_HOURS, SERVICE_INTERVAL_TYPE_GREASE),
        DefaultServiceIntervalSpec("rear_shock", "Full Rebuild", 4_000.0, T.TWO_HUNDRED_HOURS, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("suspension_pivots", "Replace Bearings", 10_000.0, T.TWENTY_FOUR_MONTHS, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("saddle", "Inspect (Rails)", 5_000.0, T.TWELVE_MONTHS, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("seat_post", "Clean / Re-grease", 2_000.0, T.SIX_MONTHS, SERVICE_INTERVAL_TYPE_GREASE),
        DefaultServiceIntervalSpec("dropper_post", "Service (Collar/Lube)", 1_000.0, T.FIFTY_HOURS, SERVICE_INTERVAL_TYPE_GREASE),
        DefaultServiceIntervalSpec("dropper_post", "Full Rebuild", 4_000.0, T.TWO_HUNDRED_HOURS, SERVICE_INTERVAL_TYPE_REPLACE),

        // Cables & Power
        DefaultServiceIntervalSpec("shift_cables", "Replace Housing/Wire", 6_000.0, T.EIGHTEEN_MONTHS, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("cable_seat_dropper", "Replace Housing/Wire", 8_000.0, T.TWENTY_FOUR_MONTHS, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("cable_front_derailleur", "Replace Housing/Wire", 6_000.0, T.EIGHTEEN_MONTHS, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("cable_rear_derailleur", "Replace Housing/Wire", 6_000.0, T.EIGHTEEN_MONTHS, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("cable_front_brake", "Replace Housing/Wire", 6_000.0, T.EIGHTEEN_MONTHS, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("cable_rear_brake", "Replace Housing/Wire", 6_000.0, T.EIGHTEEN_MONTHS, SERVICE_INTERVAL_TYPE_REPLACE),
        DefaultServiceIntervalSpec("battery", "Inspect / Charge Check", 500.0, T.ONE_MONTH, SERVICE_INTERVAL_TYPE_INSPECTION),
        DefaultServiceIntervalSpec("battery", "Replace", 0.0, 3 * 365 * 24 * 3600L, SERVICE_INTERVAL_TYPE_REPLACE, "approx 500 charge cycles"),
    )

    private val byType: Map<String, List<DefaultServiceIntervalSpec>> = specs.groupBy { it.type }

    /** Returns default service interval specs for the given component type, or empty list if none. */
    fun byType(type: String): List<DefaultServiceIntervalSpec> = byType[type] ?: emptyList()

    /** Returns the primary Replace interval km for lifespan, or null if no Replace spec exists. */
    fun replaceIntervalKmForType(type: String): Double? =
        byType[type]?.firstOrNull { it.intervalType == SERVICE_INTERVAL_TYPE_REPLACE && it.intervalKm > 0 }?.intervalKm
}
