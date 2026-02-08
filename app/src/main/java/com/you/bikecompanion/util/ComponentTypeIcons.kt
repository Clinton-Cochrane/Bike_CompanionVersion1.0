package com.you.bikecompanion.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps component type slugs to Material icons for visual identification.
 */
fun componentTypeIcon(type: String): ImageVector = when (type) {
    "chain" -> Icons.Filled.Link
    "cassette", "freewheel", "chainring", "front_derailleur", "rear_derailleur", "shift_levers" -> Icons.Filled.Settings
    "tire", "tires", "front_wheel", "rear_wheel" -> Icons.Filled.RadioButtonUnchecked
    "brake_pads", "brake_rotor", "front_brake", "rear_brake", "brake_levers" -> Icons.Filled.Stop
    "bottom_bracket", "cranks", "frame", "fork", "headset", "headset_bearings", "stem" -> Icons.Filled.Build
    "cables", "shift_cables", "brake_cables", "brake_fluid" -> Icons.Filled.SwapHoriz
    "handlebars", "bar_ends", "grips", "saddle", "seat_post", "pedals" -> Icons.Filled.DirectionsBike
    "battery" -> Icons.Filled.BatteryChargingFull
    "dropper_post", "rear_shock" -> Icons.Filled.Build
    "derailleur" -> Icons.Filled.Settings
    else -> Icons.Filled.Build
}
