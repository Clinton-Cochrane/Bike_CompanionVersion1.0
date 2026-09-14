package com.clintoncochrane.bikecompanion.notifications

/** Returns whether the app should launch the Android notification permission request. */
internal fun shouldRequestNotificationPermission(
    sdkInt: Int,
    permissionGranted: Boolean,
): Boolean = sdkInt >= 33 && !permissionGranted
