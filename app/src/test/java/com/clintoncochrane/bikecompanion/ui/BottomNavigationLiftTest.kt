package com.clintoncochrane.bikecompanion.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomNavigationLiftTest {
    @Test
    fun bottomNavigationTranslationY_movesUpBySheetTravel() {
        assertEquals(-240f, bottomNavigationTranslationY(240f), 0f)
        assertEquals(0f, bottomNavigationTranslationY(0f), 0f)
        assertEquals(0f, bottomNavigationTranslationY(-10f), 0f)
    }
}
