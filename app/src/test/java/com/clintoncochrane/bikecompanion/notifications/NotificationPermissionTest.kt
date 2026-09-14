package com.clintoncochrane.bikecompanion.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionTest {

    @Test
    fun shouldRequestNotificationPermission_android13OrNewerWhenNotGranted_returnsTrue() {
        assertTrue(shouldRequestNotificationPermission(sdkInt = 33, permissionGranted = false))
    }

    @Test
    fun shouldRequestNotificationPermission_permissionAlreadyGranted_returnsFalse() {
        assertFalse(shouldRequestNotificationPermission(sdkInt = 34, permissionGranted = true))
    }

    @Test
    fun shouldRequestNotificationPermission_beforeAndroid13_returnsFalse() {
        assertFalse(shouldRequestNotificationPermission(sdkInt = 32, permissionGranted = false))
    }
}
