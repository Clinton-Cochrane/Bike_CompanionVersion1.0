package com.you.bikecompanion.ui.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Checkbox
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.you.bikecompanion.R
import com.you.bikecompanion.data.component.ComponentContext
import com.you.bikecompanion.data.component.ComponentContextValidation
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.component.ComponentSwapEntity
import com.you.bikecompanion.data.component.ServiceIntervalEntity
import com.you.bikecompanion.util.DisplayFormatHelper
import com.you.bikecompanion.util.DurationFormatHelper
import com.you.bikecompanion.util.IntervalTimeConstants
import com.you.bikecompanion.util.ServiceIntervalHelper
import com.you.bikecompanion.util.componentTypeIcon
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentDetailScreen(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
) {
    val viewModel: ComponentDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel(
        viewModelStoreOwner = backStackEntry,
    )
    val uiState by viewModel.uiState.collectAsState()
    val backContentDesc = stringResource(R.string.common_back_content_description)

    var showContextEdit by remember { mutableStateOf(false) }
    var contextValidationError by remember { mutableStateOf<String?>(null) }
    var showComponentEdit by remember { mutableStateOf(false) }
    var componentEditValidationError by remember { mutableStateOf<String?>(null) }
    var showInstallPicker by remember { mutableStateOf(false) }
    var showAddIntervalDialog by remember { mutableStateOf(false) }
    var intervalMenuExpanded by remember { mutableStateOf<Long?>(null) }

    val nameEmptyMsg = stringResource(R.string.component_edit_validation_name_empty)
    val mileageInvalidMsg = stringResource(R.string.component_edit_validation_mileage_invalid)
    val timeInvalidMsg = stringResource(R.string.component_edit_validation_time_invalid)
    if (showComponentEdit && uiState.component != null) {
        ComponentEditDialog(
            component = uiState.component!!,
            validationError = componentEditValidationError,
            onDismiss = {
                showComponentEdit = false
                componentEditValidationError = null
            },
            onSave = { name, mileageStr, timeStr, resetSpeeds ->
                componentEditValidationError = null
                val nameTrimmed = name.trim()
                val mileage = mileageStr.trim().toDoubleOrNull()
                val timeSeconds = timeStr.trim().let { t ->
                    if (t.isEmpty()) 0L else DurationFormatHelper.parseDurationToSeconds(t)
                }
                when {
                    nameTrimmed.isBlank() -> componentEditValidationError = nameEmptyMsg
                    mileage == null || mileage < 0 -> componentEditValidationError = mileageInvalidMsg
                    timeStr.isNotBlank() && timeSeconds == null -> componentEditValidationError = timeInvalidMsg
                    else -> {
                        viewModel.updateComponent(
                            nameTrimmed,
                            mileage ?: 0.0,
                            timeSeconds ?: 0L,
                            resetSpeeds,
                        )
                        showComponentEdit = false
                    }
                }
            },
        )
    }

    if (showContextEdit && uiState.component != null) {
        ComponentContextEditDialog(
            componentName = DisplayFormatHelper.formatForDisplay(uiState.component!!.name),
            initialContext = uiState.context,
            componentId = uiState.component!!.id,
            validationError = contextValidationError,
            onDismiss = {
                showContextEdit = false
                contextValidationError = null
            },
            onSave = { payload ->
                contextValidationError = null
                viewModel.saveComponentContext(payload) { result ->
                    when (result) {
                        is ComponentContextValidation.Ok -> showContextEdit = false
                        is ComponentContextValidation.Error -> contextValidationError = result.message
                    }
                }
            },
        )
    }

    uiState.component?.let { component ->
        if (showInstallPicker) {
            val bikesForPicker = uiState.bikes
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showInstallPicker = false },
                title = { Text(stringResource(R.string.component_install_picker_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bikesForPicker.forEach { bike ->
                            TextButton(
                                onClick = {
                                    viewModel.installComponent(bike.id)
                                    showInstallPicker = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(bike.name)
                            }
                        }
                        if (bikesForPicker.isEmpty()) {
                            Text(stringResource(R.string.component_install_no_bikes), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInstallPicker = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                },
            )
        }
    }

    val editContentDesc = stringResource(R.string.common_edit)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.component_detail_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.semantics { contentDescription = backContentDesc },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (uiState.component != null) {
                        IconButton(
                            onClick = { showComponentEdit = true },
                            modifier = Modifier.semantics { contentDescription = editContentDesc },
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = editContentDesc)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { paddingValues ->
        if (uiState.component == null && !uiState.loading) {
            Text(
                stringResource(R.string.common_error),
                modifier = Modifier.padding(paddingValues).padding(16.dp),
            )
            return@Scaffold
        }
        val component = uiState.component ?: return@Scaffold

        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = componentTypeIcon(component.type),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    DisplayFormatHelper.formatComponentTypeForDisplay(component.type),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    DisplayFormatHelper.formatForDisplay(component.name),
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val bikeName = component.bikeId?.let { bid ->
                                    uiState.bikes.find { it.id == bid }?.name
                                }
                                Text(
                                    bikeName ?: stringResource(R.string.component_in_garage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(stringResource(R.string.bike_stat_km, component.distanceUsedKm), style = MaterialTheme.typography.labelMedium)
                            Text(DurationFormatHelper.formatDurationSeconds(component.totalTimeSeconds), style = MaterialTheme.typography.labelMedium)
                            Text(stringResource(R.string.bike_stat_kmh, component.avgSpeedKmh), style = MaterialTheme.typography.labelMedium)
                            Text(stringResource(R.string.bike_stat_kmh, component.maxSpeedKmh), style = MaterialTheme.typography.labelMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (component.bikeId == null) {
                                OutlinedButton(onClick = { showInstallPicker = true }) {
                                    Text(stringResource(R.string.component_install))
                                }
                            } else {
                                OutlinedButton(onClick = { viewModel.uninstallComponent() }) {
                                    Text(stringResource(R.string.component_uninstall))
                                }
                            }
                        }
                    }
                }
            }
            item {
                var purchaseExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { purchaseExpanded = !purchaseExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.component_purchase_section), style = MaterialTheme.typography.titleMedium)
                            Icon(
                                if (purchaseExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        if (purchaseExpanded) {
                            val ctx = uiState.context
                            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ctx?.purchaseLink?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                ctx?.purchasePrice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                ctx?.purchaseDateMs?.let { Text(SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it)), style = MaterialTheme.typography.bodySmall) }
                                ctx?.notes?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3) }
                                TextButton(onClick = { showContextEdit = true }) {
                                    Text(if (ctx != null) stringResource(R.string.component_context_view_edit) else stringResource(R.string.component_context_add))
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.component_service_intervals), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showAddIntervalDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.component_add_interval))
                    }
                }
            }
            items(uiState.serviceIntervals, key = { it.id }) { interval ->
                val healthPercent = ServiceIntervalHelper.healthPercent(interval)
                val desc = ServiceIntervalHelper.description(interval)
                val subtitle = when {
                    desc.kmText != null && desc.timeText != null -> "${desc.kmText}, or ${desc.timeText}"
                    desc.kmText != null -> desc.kmText
                    desc.timeText != null -> desc.timeText
                    else -> ""
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LinearProgressIndicator(
                            progress = { healthPercent / 100f },
                            modifier = Modifier
                                .size(40.dp, 40.dp)
                                .semantics { contentDescription = "Health $healthPercent%" },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(interval.name, style = MaterialTheme.typography.titleSmall)
                            if (subtitle.isNotBlank()) {
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Box {
                            IconButton(onClick = { intervalMenuExpanded = if (intervalMenuExpanded == interval.id) null else interval.id }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.common_more_options))
                            }
                            DropdownMenu(
                                expanded = intervalMenuExpanded == interval.id,
                                onDismissRequest = { intervalMenuExpanded = null },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        viewModel.deleteServiceInterval(interval.id)
                                        intervalMenuExpanded = null
                                    },
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(stringResource(R.string.component_swaps_section), style = MaterialTheme.typography.titleMedium)
            }
            items(uiState.swaps, key = { it.id }) { swap ->
                val bikeName = uiState.bikes.find { it.id == swap.bikeId }?.name ?: ""
                val dateFormat = SimpleDateFormat("M/d/yyyy 'at' h:mm a", Locale.getDefault())
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.component_swap_bike, bikeName), style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (swap.uninstalledAt == null) {
                                stringResource(R.string.component_swap_since, dateFormat.format(Date(swap.installedAt)))
                            } else {
                                stringResource(R.string.component_swap_until, dateFormat.format(Date(swap.uninstalledAt!!)))
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (swap.uninstalledAt == null) {
                            Text(stringResource(R.string.component_swap_currently_installed), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (showAddIntervalDialog) {
        var name by remember { mutableStateOf("") }
        var intervalKm by remember { mutableStateOf("") }
        var intervalTimeStr by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddIntervalDialog = false },
            title = { Text(stringResource(R.string.component_add_interval)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.component_interval_name)) },
                        placeholder = { Text("Inspection") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = intervalKm,
                        onValueChange = { intervalKm = it },
                        label = { Text(stringResource(R.string.component_interval_km_label)) },
                        placeholder = { Text("5000") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = intervalTimeStr,
                        onValueChange = { intervalTimeStr = it },
                        label = { Text(stringResource(R.string.component_interval_time_label)) },
                        placeholder = { Text(stringResource(R.string.component_interval_time_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val km = intervalKm.toDoubleOrNull() ?: 0.0
                    val timeSeconds = intervalTimeStr.trim().takeIf { it.isNotEmpty() }?.let {
                        IntervalTimeConstants.parseIntervalTime(it)
                    }
                    if (name.isNotBlank() && (km > 0 || timeSeconds != null)) {
                        viewModel.addServiceInterval(name.trim(), km, "inspection", timeSeconds)
                        showAddIntervalDialog = false
                    }
                }) {
                    Text(stringResource(R.string.component_context_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddIntervalDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun ComponentEditDialog(
    component: ComponentEntity,
    validationError: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, mileageStr: String, timeStr: String, resetAvgMaxSpeed: Boolean) -> Unit,
) {
    var name by remember(component.id) { mutableStateOf(component.name) }
    var mileageStr by remember(component.id) { mutableStateOf(component.distanceUsedKm.toString()) }
    var timeStr by remember(component.id) {
        mutableStateOf(DurationFormatHelper.formatDurationSeconds(component.totalTimeSeconds))
    }
    var resetSpeeds by remember(component.id) { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.component_edit_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.component_edit_display_name)) },
                    placeholder = { Text(stringResource(R.string.component_edit_display_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = mileageStr,
                    onValueChange = { mileageStr = it },
                    label = { Text(stringResource(R.string.component_edit_mileage)) },
                    placeholder = { Text(stringResource(R.string.component_edit_mileage_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = timeStr,
                    onValueChange = { timeStr = it },
                    label = { Text(stringResource(R.string.component_edit_time)) },
                    placeholder = { Text(stringResource(R.string.component_edit_time_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = resetSpeeds,
                        onCheckedChange = { resetSpeeds = it },
                    )
                    Text(
                        stringResource(R.string.component_edit_reset_speeds),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    stringResource(R.string.component_edit_reset_speeds_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, mileageStr, timeStr, resetSpeeds) }) {
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

