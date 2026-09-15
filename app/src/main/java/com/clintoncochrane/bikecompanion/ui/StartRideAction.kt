package com.clintoncochrane.bikecompanion.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.clintoncochrane.bikecompanion.R

@Composable
fun StartRideAction(onStartRide: () -> Unit) {
    IconButton(onClick = onStartRide) {
        Icon(
            imageVector = Icons.Filled.DirectionsBike,
            contentDescription = stringResource(R.string.start_ride_content_description),
        )
    }
}
