package com.clintoncochrane.bikecompanion.util

import com.clintoncochrane.bikecompanion.data.component.ComponentEntity

/** Returns whether a component currently has an alert that can be acted on. */
fun isComponentAlertActionable(
    component: ComponentEntity,
    nowMillis: Long,
    alertsEnabled: Boolean = component.alertsEnabled,
): Boolean {
    if (!alertsEnabled) return false
    if (component.alertSnoozeUntilKm?.let { component.lifetimeDistanceKm < it } == true) return false
    if (component.alertSnoozeUntilTime?.let { nowMillis < it } == true) return false
    return componentHealthPercent(component)?.let { health ->
        health <= component.alertThresholdPercent
    } == true
}
