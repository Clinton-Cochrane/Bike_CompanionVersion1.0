package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clintoncochrane.bikecompanion.R
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.bike.recordedDistanceKm
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.util.DisplayFormatHelper
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun BikeActionsMenu(
    onAddComponent: () -> Unit,
    onCorrectMileage: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val contentDescription = stringResource(R.string.bike_actions_content_description)

    IconButton(
        onClick = { expanded = true },
        modifier = Modifier.semantics { this.contentDescription = contentDescription },
    ) {
        Icon(Icons.Filled.MoreVert, contentDescription = null)
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bike_add_component)) },
            onClick = {
                expanded = false
                onAddComponent()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bike_correct_mileage)) },
            onClick = {
                expanded = false
                onCorrectMileage()
            },
        )
    }
}

@Composable
internal fun CorrectMileageSheet(
    bike: BikeEntity,
    components: List<ComponentEntity>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onApply: (correctedMileageKm: Double, selectedComponentIds: Set<Long>) -> Unit,
) {
    var correctedMileageText by remember(bike.id) {
        mutableStateOf(formatMileageInput(bike.totalDistanceKm))
    }
    var selectedComponentIds by remember(bike.id) { mutableStateOf(emptySet<Long>()) }
    val correctedMileageKm = correctedMileageText.toDoubleOrNull()
    val deltaKm = correctedMileageKm?.minus(bike.totalDistanceKm)
    val selectedComponentWouldBeNegative = deltaKm != null && components.any { component ->
        component.id in selectedComponentIds && component.lifetimeDistanceKm + deltaKm < 0.0
    }
    val validationMessage = when {
        correctedMileageKm == null || !correctedMileageKm.isFinite() || correctedMileageKm < 0.0 ->
            stringResource(R.string.bike_correct_mileage_invalid)
        correctedMileageKm < bike.recordedDistanceKm ->
            stringResource(R.string.bike_correct_mileage_below_recorded, bike.recordedDistanceKm)
        selectedComponentWouldBeNegative ->
            stringResource(R.string.bike_correct_mileage_component_negative)
        else -> null
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.bike_correct_mileage),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.bike_correct_mileage_current, bike.totalDistanceKm),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = correctedMileageText,
                onValueChange = { correctedMileageText = it },
                label = { Text(stringResource(R.string.bike_correct_mileage_new)) },
                suffix = { Text(stringResource(R.string.bike_correct_mileage_unit)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = validationMessage != null,
            )
            if (deltaKm != null && deltaKm.isFinite()) {
                Text(
                    text = stringResource(
                        R.string.bike_correct_mileage_difference,
                        formatSignedMileage(deltaKm),
                    ),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (validationMessage != null) {
                Text(
                    text = validationMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (components.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.bike_correct_mileage_components_prompt),
                    style = MaterialTheme.typography.titleSmall,
                )
                components.forEach { component ->
                    val componentName = DisplayFormatHelper.componentLabels(
                        component.name,
                        component.make,
                        component.model,
                        component.type,
                    ).primary
                    val checkboxDescription = stringResource(
                        R.string.bike_correct_mileage_component_content_description,
                        componentName,
                    )
                    val previewMileageKm = deltaKm
                        ?.takeIf { it.isFinite() }
                        ?.let { component.lifetimeDistanceKm + it }
                        ?: component.lifetimeDistanceKm
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !saving) {
                                selectedComponentIds = selectedComponentIds.toggle(component.id)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = component.id in selectedComponentIds,
                            onCheckedChange = {
                                selectedComponentIds = selectedComponentIds.toggle(component.id)
                            },
                            enabled = !saving,
                            modifier = Modifier.semantics {
                                contentDescription = checkboxDescription
                            },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(componentName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(
                                    R.string.bike_correct_mileage_component_preview,
                                    component.lifetimeDistanceKm,
                                    previewMileageKm,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (component.id in selectedComponentIds && previewMileageKm < 0.0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss, enabled = !saving) {
                    Text(stringResource(R.string.common_cancel))
                }
                TextButton(
                    onClick = {
                        correctedMileageKm?.let { onApply(it, selectedComponentIds) }
                    },
                    enabled = correctedMileageKm != null && validationMessage == null && !saving,
                ) {
                    Text(stringResource(R.string.bike_correct_mileage_apply))
                }
            }
        }
    }
}

private fun Set<Long>.toggle(id: Long): Set<Long> = if (id in this) this - id else this + id

private fun formatMileageInput(value: Double): String = if (value % 1.0 == 0.0) {
    value.toLong().toString()
} else {
    value.toString()
}

private fun formatSignedMileage(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
        isGroupingUsed = false
    }
    val sign = if (value > 0.0) "+" else ""
    return sign + formatter.format(value)
}
