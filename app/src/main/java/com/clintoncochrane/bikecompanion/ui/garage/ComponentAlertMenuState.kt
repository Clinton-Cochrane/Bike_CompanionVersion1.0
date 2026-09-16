package com.clintoncochrane.bikecompanion.ui.garage

/** Temporary Alerts edit buffer for one open component context menu. */
internal data class ComponentAlertMenuState(
    val componentId: Long,
    val initialAlertsEnabled: Boolean,
    val pendingAlertsEnabled: Boolean = initialAlertsEnabled,
) {
    val shouldPersist: Boolean
        get() = pendingAlertsEnabled != initialAlertsEnabled
}
