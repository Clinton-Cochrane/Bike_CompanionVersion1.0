package com.clintoncochrane.bikecompanion.util

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

    fun makeModel(make: String, model: String): String =
        listOf(make, model)
            .map(String::trim)
            .filter(String::isNotBlank)
            .joinToString(" ")

    fun bikeLabels(displayName: String, make: String, model: String): DisplayLabels =
        labels(displayName, makeModel(make, model), "Bike")

    fun componentLabels(
        displayName: String,
        make: String,
        model: String,
        type: String,
    ): DisplayLabels = labels(
        displayName,
        makeModel(make, model),
        formatComponentTypeForDisplay(type),
    )

    private fun labels(displayName: String, makeModel: String, fallback: String): DisplayLabels = when {
        displayName.isNotBlank() -> DisplayLabels(
            primary = displayName.trim(),
            secondary = makeModel.ifBlank { fallback },
        )
        makeModel.isNotBlank() -> DisplayLabels(primary = makeModel, secondary = fallback)
        else -> DisplayLabels(primary = fallback, secondary = null)
    }
}

data class DisplayLabels(
    val primary: String,
    val secondary: String?,
)
