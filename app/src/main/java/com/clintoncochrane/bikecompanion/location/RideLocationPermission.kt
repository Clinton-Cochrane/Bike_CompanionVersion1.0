package com.clintoncochrane.bikecompanion.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

enum class RideLocationPermissionAction {
    START_RIDE,
    SHOW_RATIONALE,
    OPEN_SETTINGS,
}

object RideLocationPermission {
    const val REQUIRED_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    val REQUEST_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, REQUIRED_PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun isFineLocationGranted(grants: Map<String, Boolean>): Boolean =
        grants[REQUIRED_PERMISSION] == true

    fun nextAction(
        isGranted: Boolean,
        hasRequestedPermission: Boolean,
        shouldShowRationale: Boolean,
    ): RideLocationPermissionAction = when {
        isGranted -> RideLocationPermissionAction.START_RIDE
        !hasRequestedPermission || shouldShowRationale -> RideLocationPermissionAction.SHOW_RATIONALE
        else -> RideLocationPermissionAction.OPEN_SETTINGS
    }
}
