package com.clintoncochrane.bikecompanion.location

import android.Manifest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class RideLocationPermissionTest {

    @Test
    fun requestPermissions_requestsFineAndCoarseLocationOnly() {
        assertArrayEquals(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            RideLocationPermission.REQUEST_PERMISSIONS,
        )
    }

    @Test
    fun permissionResult_fineLocationGranted_allowsTracking() {
        val grants = mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to true,
            Manifest.permission.ACCESS_COARSE_LOCATION to true,
        )

        assertEquals(true, RideLocationPermission.isFineLocationGranted(grants))
    }

    @Test
    fun permissionResult_onlyCoarseLocationGranted_doesNotAllowTracking() {
        val grants = mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.ACCESS_COARSE_LOCATION to true,
        )

        assertEquals(false, RideLocationPermission.isFineLocationGranted(grants))
    }

    @Test
    fun nextAction_permissionGranted_startsRide() {
        val action = RideLocationPermission.nextAction(
            isGranted = true,
            hasRequestedPermission = false,
            shouldShowRationale = false,
        )

        assertEquals(RideLocationPermissionAction.START_RIDE, action)
    }

    @Test
    fun nextAction_firstRequest_showsContextualRationale() {
        val action = RideLocationPermission.nextAction(
            isGranted = false,
            hasRequestedPermission = false,
            shouldShowRationale = false,
        )

        assertEquals(RideLocationPermissionAction.SHOW_RATIONALE, action)
    }

    @Test
    fun nextAction_deniedButRequestable_showsContextualRationaleAgain() {
        val action = RideLocationPermission.nextAction(
            isGranted = false,
            hasRequestedPermission = true,
            shouldShowRationale = true,
        )

        assertEquals(RideLocationPermissionAction.SHOW_RATIONALE, action)
    }

    @Test
    fun nextAction_permanentlyDenied_opensSettingsGuidance() {
        val action = RideLocationPermission.nextAction(
            isGranted = false,
            hasRequestedPermission = true,
            shouldShowRationale = false,
        )

        assertEquals(RideLocationPermissionAction.OPEN_SETTINGS, action)
    }
}
