package com.you.bikecompanion.util

/**
 * Formats internal identifiers (e.g. component types, seeded names) for user-facing display.
 * Replaces underscores with spaces so values like "brake_rotor" display as "brake rotor".
 */
object DisplayFormatHelper {

    /**
     * Formats a string for display by replacing underscores with spaces.
     * Used for component names and other identifiers that may use internal naming conventions.
     *
     * @param text The raw text (e.g. "brake_rotor", "Default brake_rotor (front)")
     * @return Display-friendly text with underscores replaced by spaces
     */
    fun formatForDisplay(text: String): String =
        text.replace('_', ' ')

    /**
     * Formats a component type identifier for display: underscores to spaces plus title case.
     * E.g. "brake_rotor" -> "Brake Rotor".
     *
     * @param type Internal component type slug (e.g. "brake_rotor", "front_derailleur")
     * @return Title-cased display string
     */
    fun formatComponentTypeForDisplay(type: String): String =
        type.replace('_', ' ')
            .split(' ')
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercaseChar() }
            }
}
