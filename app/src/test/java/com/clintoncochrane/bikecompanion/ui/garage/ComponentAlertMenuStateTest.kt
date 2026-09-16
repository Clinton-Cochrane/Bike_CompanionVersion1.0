package com.clintoncochrane.bikecompanion.ui.garage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentAlertMenuStateTest {

    @Test
    fun shouldPersist_returnsFalseWhenFinalValueMatchesInitialValue() {
        assertFalse(ComponentAlertMenuState(componentId = 1L, initialAlertsEnabled = true).shouldPersist)
        assertFalse(ComponentAlertMenuState(componentId = 1L, initialAlertsEnabled = false).shouldPersist)
        assertFalse(
            ComponentAlertMenuState(componentId = 1L, initialAlertsEnabled = true)
                .copy(pendingAlertsEnabled = false)
                .copy(pendingAlertsEnabled = true)
                .shouldPersist,
        )
        assertFalse(
            ComponentAlertMenuState(componentId = 1L, initialAlertsEnabled = false)
                .copy(pendingAlertsEnabled = true)
                .copy(pendingAlertsEnabled = false)
                .shouldPersist,
        )
    }

    @Test
    fun shouldPersist_returnsTrueWhenFinalValueDiffersFromInitialValue() {
        assertTrue(
            ComponentAlertMenuState(componentId = 1L, initialAlertsEnabled = true)
                .copy(pendingAlertsEnabled = false)
                .shouldPersist,
        )
        assertTrue(
            ComponentAlertMenuState(componentId = 1L, initialAlertsEnabled = false)
                .copy(pendingAlertsEnabled = true)
                .shouldPersist,
        )
    }
}
