package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clintoncochrane.bikecompanion.R
import com.clintoncochrane.bikecompanion.data.component.DefaultComponentTypes

@Composable
fun AddComponentDialog(
    onDismiss: () -> Unit,
    onAdd: (AddComponentRequest) -> Unit,
) {
    var form by remember { mutableStateOf(AddComponentFormState()) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var invalidPriorDistance by remember { mutableStateOf(false) }
    val selectedType = DefaultComponentTypes.SUGGESTED.first { it.type == form.typeKey }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.component_add_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = typeMenuExpanded,
                    onExpandedChange = { typeMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        label = { Text(stringResource(R.string.component_add_type)) },
                        trailingIcon = {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        },
                        readOnly = true,
                        singleLine = true,
                    )
                    ExposedDropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                    ) {
                        DefaultComponentTypes.SUGGESTED.forEach { componentType ->
                            DropdownMenuItem(
                                text = { Text(componentType.displayName) },
                                onClick = {
                                    form = form.copy(typeKey = componentType.type)
                                    typeMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = form.displayName,
                    onValueChange = { form = form.copy(displayName = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.component_add_display_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = form.make,
                    onValueChange = { form = form.copy(make = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.component_make)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = form.model,
                    onValueChange = { form = form.copy(model = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.component_model)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = form.priorDistanceText,
                    onValueChange = {
                        form = form.copy(priorDistanceText = it)
                        invalidPriorDistance = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.component_add_prior_distance)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = invalidPriorDistance,
                    supportingText = if (invalidPriorDistance) {
                        { Text(stringResource(R.string.component_prior_usage_invalid)) }
                    } else {
                        null
                    },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = form.approximate,
                        onCheckedChange = { form = form.copy(approximate = it) },
                    )
                    Text(
                        text = stringResource(R.string.component_add_approximate),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (val submission = form.submit()) {
                        is AddComponentFormSubmission.Valid -> onAdd(submission.request)
                        AddComponentFormSubmission.InvalidPriorDistance -> invalidPriorDistance = true
                        AddComponentFormSubmission.InvalidType -> Unit
                    }
                },
            ) {
                Text(stringResource(R.string.component_add_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
