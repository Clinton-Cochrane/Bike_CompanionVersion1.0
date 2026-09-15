package com.clintoncochrane.bikecompanion.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionActionTest {

    @Test
    fun canPostMaintenanceNotifications_deniedOnAndroid13_returnsFalse() {
        assertEquals(false, canPostMaintenanceNotifications(sdkInt = 33, permissionGranted = false))
    }

    @Test
    fun canPostMaintenanceNotifications_beforeAndroid13_returnsTrue() {
        assertEquals(true, canPostMaintenanceNotifications(sdkInt = 32, permissionGranted = false))
    }

    @Test
    fun notificationPermissionAction_beforeAndroid13_returnsNone() {
        assertEquals(
            NotificationPermissionAction.NONE,
            notificationPermissionAction(sdkInt = 32, permissionGranted = false, hasRequestedPermission = false),
        )
    }

    @Test
    fun notificationPermissionAction_permissionGranted_returnsNone() {
        assertEquals(
            NotificationPermissionAction.NONE,
            notificationPermissionAction(sdkInt = 33, permissionGranted = true, hasRequestedPermission = false),
        )
    }

    @Test
    fun notificationPermissionAction_firstContextualOptIn_requestsPermission() {
        assertEquals(
            NotificationPermissionAction.REQUEST,
            notificationPermissionAction(sdkInt = 33, permissionGranted = false, hasRequestedPermission = false),
        )
    }

    @Test
    fun notificationPermissionAction_deniedPreviously_showsSettingsGuidance() {
        assertEquals(
            NotificationPermissionAction.OPEN_SETTINGS,
            notificationPermissionAction(sdkInt = 33, permissionGranted = false, hasRequestedPermission = true),
        )
    }
}
