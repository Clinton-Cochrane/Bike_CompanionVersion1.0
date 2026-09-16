package com.clintoncochrane.bikecompanion.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayFormatHelperTest {

    @Test
    fun bikeLabels_displayNameWinsAndMakeModelIsSecondary() {
        assertEquals(
            DisplayLabels(primary = "Rory", secondary = "Trek Domane AL 2"),
            DisplayFormatHelper.bikeLabels(" Rory ", " Trek ", " Domane AL 2 "),
        )
    }

    @Test
    fun bikeLabels_makeModelWinsWhenDisplayNameIsBlank() {
        assertEquals(
            DisplayLabels(primary = "Trek Domane AL 2", secondary = "Bike"),
            DisplayFormatHelper.bikeLabels(" ", "Trek", "Domane AL 2"),
        )
    }

    @Test
    fun componentLabels_typeWinsWhenIdentificationIsBlank() {
        assertEquals(
            DisplayLabels(primary = "Front Derailleur", secondary = null),
            DisplayFormatHelper.componentLabels("", "", "", "front_derailleur"),
        )
    }

    @Test
    fun makeModel_joinsNonBlankValuesWithoutExtraSpaces() {
        assertEquals("Shimano", DisplayFormatHelper.makeModel(" Shimano ", " "))
        assertEquals("FD-7600", DisplayFormatHelper.makeModel("", " FD-7600 "))
        assertEquals("Shimano FD-7600", DisplayFormatHelper.makeModel(" Shimano ", " FD-7600 "))
    }
}
