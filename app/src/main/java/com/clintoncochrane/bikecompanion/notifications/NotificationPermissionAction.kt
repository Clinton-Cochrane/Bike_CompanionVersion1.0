package com.clintoncochrane.bikecompanion.notifications

/** The next user-visible action for an optional maintenance notification opt-in. */
internal enum class NotificationPermissionAction {
    NONE,
    REQUEST,
    OPEN_SETTINGS,
}

/** Keeps the platform prompt contextual and avoids showing it again after a denial. */
internal fun notificationPermissionAction(
    sdkInt: Int,
    permissionGranted: Boolean,
    hasRequestedPermission: Boolean,
): NotificationPermissionAction = when {
    sdkInt < 33 || permissionGranted -> NotificationPermissionAction.NONE
    hasRequestedPermission -> NotificationPermissionAction.OPEN_SETTINGS
    else -> NotificationPermissionAction.REQUEST
}

/** Whether Android allows an optional maintenance alert to be posted. */
internal fun canPostMaintenanceNotifications(sdkInt: Int, permissionGranted: Boolean): Boolean =
    sdkInt < 33 || permissionGranted
