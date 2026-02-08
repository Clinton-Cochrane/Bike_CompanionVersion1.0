package com.you.bikecompanion.data.component

/**
 * Component categories for grouping in the UI.
 * Display order matches bike anatomy: cockpit → frame → drivetrain → wheels → brakes → cables → power.
 *
 * New bikes use [defaultBikeCategories] only (cockpit, frame, drivetrain, wheels, brakes, cables).
 * POWER and OTHER can be added later when we know more about the bike (e.g. eBike, single-speed).
 */
enum class ComponentCategory(val displayOrder: Int) {
    COCKPIT(0),
    FRAME(1),
    DRIVETRAIN(2),
    WHEELS(3),
    BRAKES(4),
    CABLES(5),
    POWER(6),
    OTHER(7),
    ;

    companion object {
        private val typeToCategory = mapOf(
            // Cockpit / controls
            "handlebars" to COCKPIT,
            "stem" to COCKPIT,
            "headset" to COCKPIT,
            "headset_bearings" to COCKPIT,
            "brake_levers" to COCKPIT,
            "shift_levers" to COCKPIT,
            "bar_ends" to COCKPIT,
            "grips" to COCKPIT,

            // Frame
            "frame" to FRAME,
            "fork" to FRAME,
            "seat_post" to FRAME,
            "saddle" to FRAME,
            "dropper_post" to FRAME,
            "rear_shock" to FRAME,
            "suspension_pivots" to FRAME,

            // Drivetrain
            "chainring" to DRIVETRAIN,
            "front_derailleur" to DRIVETRAIN,
            "rear_derailleur" to DRIVETRAIN,
            "derailleur" to DRIVETRAIN,
            "bottom_bracket" to DRIVETRAIN,
            "cranks" to DRIVETRAIN,
            "crankset" to DRIVETRAIN,
            "pedals" to DRIVETRAIN,
            "chain" to DRIVETRAIN,
            "cassette" to DRIVETRAIN,
            "freewheel" to DRIVETRAIN,

            // Wheels
            "front_wheel" to WHEELS,
            "rear_wheel" to WHEELS,
            "tire" to WHEELS,
            "tires" to WHEELS,
            "hub" to WHEELS,
            "spokes" to WHEELS,
            "tube" to WHEELS,
            "rim" to WHEELS,
            "tubeless_sealant" to WHEELS,

            // Brakes
            "front_brake" to BRAKES,
            "rear_brake" to BRAKES,
            "brake_caliper" to BRAKES,
            "brake_pads" to BRAKES,
            "brake_rotor" to BRAKES,

            // Cables / fluid
            "brake_cables" to CABLES,
            "brake_fluid" to CABLES,
            "shift_cables" to CABLES,
            "cables" to CABLES,
            "cable_front_derailleur" to CABLES,
            "cable_rear_derailleur" to CABLES,
            "cable_front_brake" to CABLES,
            "cable_rear_brake" to CABLES,
            "cable_seat_dropper" to CABLES,

            // Power
            "battery" to POWER,
        )

        fun fromComponentType(type: String): ComponentCategory =
            typeToCategory[type] ?: OTHER

        /** Display order for any list: cockpit → frame → drivetrain → wheels → brakes → cables → power → other. */
        val displayOrder: List<ComponentCategory> = entries.sortedBy { it.displayOrder }

        /**
         * Categories every new bike gets by default (KISS).
         * Excludes POWER and OTHER; can be refined later (e.g. single-speed, 1x, disc vs v-brake).
         */
        val defaultBikeCategories: List<ComponentCategory> = listOf(
            COCKPIT,
            FRAME,
            DRIVETRAIN,
            WHEELS,
            BRAKES,
            CABLES,
        )
    }
}
