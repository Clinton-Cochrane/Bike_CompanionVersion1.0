package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clintoncochrane.bikecompanion.R
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty

@Composable
fun PriorUsageDialog(
    componentName: String,
    onDismiss: () -> Unit,
    onSave: (PriorUsageCertainty, Double) -> Unit,
) {
    var certainty by remember { mutableStateOf(PriorUsageCertainty.KNOWN) }
    var baselineKmText by remember { mutableStateOf("0") }
    var validationError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.component_prior_usage_title, componentName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.component_prior_usage_prompt))
                PriorUsageCertainty.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = certainty == option,
                            onClick = {
                                certainty = option
                                validationError = false
                            },
                        )
                        Text(stringResource(option.labelResource()))
                    }
                }
                if (certainty != PriorUsageCertainty.UNKNOWN) {
                    OutlinedTextField(
                        value = baselineKmText,
                        onValueChange = {
                            baselineKmText = it
                            validationError = false
                        },
                        label = { Text(stringResource(R.string.component_prior_usage_km)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = validationError,
                        supportingText = if (validationError) {
                            { Text(stringResource(R.string.component_prior_usage_invalid)) }
                        } else {
                            null
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (certainty == PriorUsageCertainty.UNKNOWN) {
                        onSave(certainty, 0.0)
                        return@TextButton
                    }
                    val baselineKm = baselineKmText.trim().toDoubleOrNull()
                    if (baselineKm == null || !baselineKm.isFinite() || baselineKm < 0.0) {
                        validationError = true
                    } else {
                        onSave(certainty, baselineKm)
                    }
                },
            ) {
                Text(stringResource(R.string.component_context_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
fun PriorUsageFields(
    certainty: PriorUsageCertainty,
    baselineKmText: String,
    onCertaintyChange: (PriorUsageCertainty) -> Unit,
    onBaselineKmChange: (String) -> Unit,
) {
    Text(
        stringResource(R.string.component_prior_usage_prompt),
        style = MaterialTheme.typography.bodyMedium,
    )
    PriorUsageCertainty.entries.forEach { option ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = certainty == option,
                onClick = { onCertaintyChange(option) },
            )
            Text(stringResource(option.labelResource()))
        }
    }
    if (certainty != PriorUsageCertainty.UNKNOWN) {
        OutlinedTextField(
            value = baselineKmText,
            onValueChange = onBaselineKmChange,
            label = { Text(stringResource(R.string.component_prior_usage_km)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}

private fun PriorUsageCertainty.labelResource(): Int = when (this) {
    PriorUsageCertainty.KNOWN -> R.string.component_prior_usage_known
    PriorUsageCertainty.APPROXIMATE -> R.string.component_prior_usage_approximate
    PriorUsageCertainty.UNKNOWN -> R.string.component_prior_usage_unknown
}
