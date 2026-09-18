package com.clintoncochrane.bikecompanion.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

class BottomNavigationLiftController(
    val onLiftChanged: (Float) -> Unit,
) {
    var navigationBarHeightPx by mutableFloatStateOf(0f)
        private set

    fun updateNavigationBarHeight(heightPx: Int) {
        navigationBarHeightPx = heightPx.coerceAtLeast(0).toFloat()
    }
}

val LocalBottomNavigationLiftController = staticCompositionLocalOf {
    BottomNavigationLiftController(onLiftChanged = {})
}

internal fun bottomNavigationTranslationY(sheetLiftPx: Float): Float =
    -sheetLiftPx.coerceAtLeast(0f)
