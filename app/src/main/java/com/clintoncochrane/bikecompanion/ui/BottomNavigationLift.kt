package com.clintoncochrane.bikecompanion.ui

import androidx.compose.runtime.staticCompositionLocalOf

class BottomNavigationLiftController(
    val onLiftChanged: (Float) -> Unit,
)

val LocalBottomNavigationLiftController = staticCompositionLocalOf {
    BottomNavigationLiftController(onLiftChanged = {})
}

internal fun bottomNavigationTranslationY(sheetLiftPx: Float): Float =
    -sheetLiftPx.coerceAtLeast(0f)
