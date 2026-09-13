package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.clintoncochrane.bikecompanion.R
import com.clintoncochrane.bikecompanion.util.IntervalTimeConstants
import com.clintoncochrane.bikecompanion.util.ServiceIntervalHelper

@Composable
internal fun serviceIntervalDescriptionText(
    description: ServiceIntervalHelper.IntervalDescription,
): String {
    if (description.expectedIntervalReached) {
        return stringResource(R.string.component_interval_expected_reached)
    }

    val distanceText = description.remainingKm?.let {
        stringResource(R.string.component_interval_distance_until_expected, it)
    }
    val timeText = description.remainingTimeSeconds?.let {
        stringResource(
            R.string.component_interval_time_until_expected,
            IntervalTimeConstants.formatRemainingSeconds(it),
        )
    }
    return when {
        distanceText != null && timeText != null -> stringResource(
            R.string.component_interval_distance_or_time,
            distanceText,
            timeText,
        )
        distanceText != null -> distanceText
        timeText != null -> timeText
        else -> ""
    }
}
