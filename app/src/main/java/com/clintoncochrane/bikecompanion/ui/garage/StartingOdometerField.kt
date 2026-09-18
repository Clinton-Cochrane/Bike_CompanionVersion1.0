package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.clintoncochrane.bikecompanion.R

internal const val INITIAL_STARTING_ODOMETER_INPUT = ""
internal const val STARTING_ODOMETER_FIELD_TAG = "starting_odometer_field"

@Composable
internal fun StartingOdometerField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val startingOdometerKm = parseStartingOdometerKm(value)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.bike_starting_odometer)) },
        placeholder = { Text(stringResource(R.string.bike_starting_odometer_placeholder)) },
        supportingText = {
            Text(
                stringResource(
                    if (startingOdometerKm == null) R.string.bike_starting_odometer_invalid
                    else R.string.bike_starting_odometer_help,
                ),
            )
        },
        isError = startingOdometerKm == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier.testTag(STARTING_ODOMETER_FIELD_TAG),
    )
}
