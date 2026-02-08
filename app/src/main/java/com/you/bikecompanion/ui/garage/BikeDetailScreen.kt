package com.you.bikecompanion.ui.garage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Error
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.you.bikecompanion.R
import com.you.bikecompanion.ui.navigation.Screen
import com.you.bikecompanion.data.component.ComponentCategory
import com.you.bikecompanion.data.component.ComponentContext
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.util.DisplayFormatHelper
import com.you.bikecompanion.util.componentTypeIcon
import com.you.bikecompanion.data.component.DefaultComponentTypes
import com.you.bikecompanion.util.ComponentSortOrder
import com.you.bikecompanion.util.componentHealthPercent
import com.you.bikecompanion.data.ride.RideEntity
import com.you.bikecompanion.util.DurationFormatHelper
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class BikeAlertLevel { NONE, MILD, DANGER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeDetailScreen(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
) {
    var showAddComponentDialog by remember { mutableStateOf(false) }
    var componentIdForInstallPicker by remember { mutableStateOf<ComponentEntity?>(null) }
    var componentIdForDeleteConfirm by remember { mutableStateOf<ComponentEntity?>(null) }
    var componentContextMenuExpanded by remember { mutableStateOf<Long?>(null) }
    val viewModel: BikeDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel(
        viewModelStoreOwner = backStackEntry,
    )
    val uiState by viewModel.uiState.collectAsState()
    val backContentDesc = stringResource(R.string.common_back_content_description)
    val editContentDesc = stringResource(R.string.common_edit)

    if (showAddComponentDialog) {
        AlertDialog(
            onDismissRequest = { showAddComponentDialog = false },
            title = { Text(stringResource(R.string.component_suggested_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DefaultComponentTypes.SUGGESTED.forEach { suggested ->
                        TextButton(
                            onClick = {
                                viewModel.addComponent(
                                    suggested.type,
                                    suggested.displayName,
                                    suggested.defaultLifespanKm,
                                )
                                showAddComponentDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "${suggested.displayName} — ${stringResource(R.string.component_lifespan_km, suggested.defaultLifespanKm)}",
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddComponentDialog = false }) {
                    Text(stringResource(R.string.component_done))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(uiState.bike?.name ?: stringResource(R.string.garage_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.semantics { contentDescription = backContentDesc },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (uiState.bike != null) {
                        IconButton(
                            onClick = { navController.navigate(Screen.EditBike.withId(uiState.bike!!.id)) },
                            modifier = Modifier.semantics { contentDescription = editContentDesc },
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
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
        if (uiState.bike == null && !uiState.loading) {
            Text(
                stringResource(R.string.common_error),
                modifier = Modifier.padding(paddingValues).padding(16.dp),
            )
            return@Scaffold
        }
        val bike = uiState.bike ?: return@Scaffold
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
                        val bikeHealthPercent = remember(uiState.components) {
                            if (uiState.components.isEmpty()) 100
                            else {
                                val byCategory = uiState.components.groupBy { ComponentCategory.fromComponentType(it.type) }
                                byCategory.values.minOf { comps -> comps.minOf { componentHealthPercent(it) } }
                            }
                        }
                        val closeToServiceThreshold = uiState.closeToServiceHealthThreshold
                        val bikeAlertLevel = remember(bikeHealthPercent, uiState.components, closeToServiceThreshold) {
                            when {
                                uiState.components.isEmpty() -> BikeAlertLevel.NONE
                                bikeHealthPercent == 0 -> BikeAlertLevel.DANGER
                                bikeHealthPercent <= closeToServiceThreshold -> BikeAlertLevel.MILD
                                else -> BikeAlertLevel.NONE
                            }
                        }
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
                                Text(
                                    text = "${bike.name.firstOrNull()?.uppercaseChar() ?: "?"}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bike.name, style = MaterialTheme.typography.titleLarge)
                                if (bike.make.isNotEmpty() || bike.model.isNotEmpty() || bike.year.isNotEmpty()) {
                                    Text(
                                        listOf(bike.make, bike.model, bike.year).filter { it.isNotEmpty() }.joinToString(" "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier.size(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                when (bikeAlertLevel) {
                                    BikeAlertLevel.NONE -> { }
                                    BikeAlertLevel.MILD -> Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = stringResource(R.string.bike_health_alert_mild_content_description),
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                    )
                                    BikeAlertLevel.DANGER -> Icon(
                                        imageVector = Icons.Outlined.Error,
                                        contentDescription = stringResource(R.string.bike_health_alert_danger_content_description),
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                        val healthProgressDesc = stringResource(R.string.bike_component_health, bikeHealthPercent)
                        LinearProgressIndicator(
                            progress = { bikeHealthPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .semantics { contentDescription = healthProgressDesc },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            BikeStatChip(label = stringResource(R.string.bike_stat_distance), value = stringResource(R.string.bike_stat_km, bike.totalDistanceKm))
                            BikeStatChip(label = stringResource(R.string.bike_stat_moving_time), value = DurationFormatHelper.formatDurationSeconds(bike.totalTimeSeconds))
                            BikeStatChip(label = stringResource(R.string.bike_stat_avg_speed), value = stringResource(R.string.bike_stat_kmh, bike.avgSpeedKmh))
                            BikeStatChip(label = stringResource(R.string.bike_stat_max_speed), value = stringResource(R.string.bike_stat_kmh, bike.maxSpeedKmh))
                        }
                        if (bike.totalElevGainM > 0 || bike.totalElevLossM > 0) {
                            Text(
                                stringResource(R.string.bike_stat_elevation, bike.totalElevGainM.toInt(), bike.totalElevLossM.toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
            if ((uiState.bike?.chainReplacementCount ?: 0) >= 3) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    Icons.Filled.Build,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(24.dp),
                                )
                                Text(
                                    stringResource(R.string.bike_chain_recommendation_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                            Text(
                                stringResource(R.string.bike_chain_recommendation_message, uiState.bike!!.chainReplacementCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            TextButton(
                                onClick = { viewModel.resetChainReplacementCount() },
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                Text(stringResource(R.string.bike_chain_recommendation_reset))
                            }
                        }
                    }
                }
            }
            item {
                val componentsByCategory = remember(uiState.components) {
                    uiState.components
                        .groupBy { ComponentCategory.fromComponentType(it.type) }
                        .mapValues { (_, comps) -> comps }
                }
                val defaultCategories = ComponentCategory.defaultBikeCategories
                val otherCategoriesWithComponents = (ComponentCategory.displayOrder - defaultCategories.toSet())
                    .filter { (componentsByCategory[it]?.size ?: 0) > 0 }
                val categoriesToShow = defaultCategories + otherCategoriesWithComponents
                var expandedCategories by remember { mutableStateOf(setOf<ComponentCategory>()) }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.garage_components),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextButton(onClick = { showAddComponentDialog = true }) {
                            Text(stringResource(R.string.bike_add_component))
                        }
                    }
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.componentSortOrder == ComponentSortOrder.TYPE_AZ,
                                onClick = { viewModel.setComponentSortOrder(ComponentSortOrder.TYPE_AZ) },
                                label = { Text(stringResource(R.string.component_sort_type_az)) },
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.componentSortOrder == ComponentSortOrder.NEXT_SERVICE,
                                onClick = { viewModel.setComponentSortOrder(ComponentSortOrder.NEXT_SERVICE) },
                                label = { Text(stringResource(R.string.component_sort_next_service)) },
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.componentSortOrder == ComponentSortOrder.HEALTH,
                                onClick = { viewModel.setComponentSortOrder(ComponentSortOrder.HEALTH) },
                                label = { Text(stringResource(R.string.component_sort_health)) },
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categoriesToShow.forEach { category ->
                            val categoryComponents = componentsByCategory[category] ?: emptyList()
                            val minHealth = categoryComponents.minOfOrNull { componentHealthPercent(it) } ?: 100
                            val isExpanded = category in expandedCategories
                            ComponentCategorySection(
                                category = category,
                                components = categoryComponents,
                                minHealth = minHealth,
                                isExpanded = isExpanded,
                                onToggleExpanded = {
                                    expandedCategories = if (isExpanded) {
                                        expandedCategories - category
                                    } else {
                                        expandedCategories + category
                                    }
                                },
                                currentBikeId = bike.id,
                                componentContextMenuExpanded = componentContextMenuExpanded,
                                onContextMenuClick = { id -> componentContextMenuExpanded = if (componentContextMenuExpanded == id) null else id },
                                onMarkReplaced = viewModel::markComponentReplaced,
                                onSnooze = { viewModel.snoozeComponent(it, 500.0) },
                                onAlertsOff = viewModel::turnOffAlerts,
                                onInstall = { componentIdForInstallPicker = it },
                                onUninstall = viewModel::uninstallComponent,
                                onViewDetails = { navController.navigate(Screen.ComponentDetail.withId(it.id)) },
                                onDelete = { componentIdForDeleteConfirm = it },
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.garage_rides),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(uiState.rides, key = { "ride_${it.id}" }) { ride ->
                RideSummaryCard(ride = ride)
            }
        }
    }
}

@Composable
private fun ComponentCategorySection(
    category: ComponentCategory,
    components: List<ComponentEntity>,
    minHealth: Int,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    currentBikeId: Long,
    componentContextMenuExpanded: Long?,
    onContextMenuClick: (Long) -> Unit,
    onMarkReplaced: (ComponentEntity) -> Unit,
    onSnooze: (ComponentEntity) -> Unit,
    onAlertsOff: (ComponentEntity) -> Unit,
    onInstall: (ComponentEntity) -> Unit,
    onUninstall: (ComponentEntity) -> Unit,
    onViewDetails: (ComponentEntity) -> Unit,
    onDelete: (ComponentEntity) -> Unit,
) {
    val categoryTitle = when (category) {
        ComponentCategory.COCKPIT -> stringResource(R.string.component_category_cockpit)
        ComponentCategory.FRAME -> stringResource(R.string.component_category_frame)
        ComponentCategory.DRIVETRAIN -> stringResource(R.string.component_category_drivetrain)
        ComponentCategory.WHEELS -> stringResource(R.string.component_category_wheels)
        ComponentCategory.BRAKES -> stringResource(R.string.component_category_brakes)
        ComponentCategory.CABLES -> stringResource(R.string.component_category_cables)
        ComponentCategory.POWER -> stringResource(R.string.component_category_power)
        ComponentCategory.OTHER -> stringResource(R.string.component_category_other)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = categoryTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.bike_component_health, minHealth),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { minHealth / 100f },
                    modifier = Modifier
                        .size(32.dp, 32.dp)
                        .semantics {
                            contentDescription = "Min health $minHealth%"
                        },
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    components.forEach { component ->
                        val showContextMenu = componentContextMenuExpanded == component.id
                        ComponentHealthCard(
                            component = component,
                            currentBikeId = currentBikeId,
                            onMarkReplaced = { onMarkReplaced(component) },
                            onSnooze = { onSnooze(component) },
                            onAlertsOff = { onAlertsOff(component) },
                            onInstall = { onInstall(component) },
                            onUninstall = { onUninstall(component) },
                            onViewDetails = { onViewDetails(component) },
                            onDelete = { onDelete(component) },
                            contextMenuExpanded = showContextMenu,
                            onContextMenuClick = { onContextMenuClick(component.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BikeStatChip(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ComponentHealthCard(
    component: ComponentEntity,
    currentBikeId: Long,
    onMarkReplaced: () -> Unit,
    onSnooze: () -> Unit,
    onAlertsOff: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit,
    contextMenuExpanded: Boolean,
    onContextMenuClick: () -> Unit,
) {
    val healthPercent = (100.0 - (component.distanceUsedKm / component.lifespanKm).coerceIn(0.0, 1.0) * 100).toInt().coerceIn(0, 100)
    val healthDesc = stringResource(R.string.bike_component_health, healthPercent)
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val isInGarage = component.bikeId == null
    val isOnCurrentBike = component.bikeId == currentBikeId
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = componentTypeIcon(component.type),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 20.dp)
                .size(28.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onViewDetails() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = DisplayFormatHelper.formatForDisplay(component.name),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    val moreOptionsDesc = stringResource(R.string.common_more_options)
                    Box {
                        IconButton(
                            onClick = onContextMenuClick,
                            modifier = Modifier.semantics { contentDescription = moreOptionsDesc },
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = contextMenuExpanded,
                            onDismissRequest = onContextMenuClick,
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.bike_component_replaced)) },
                                onClick = {
                                    onMarkReplaced()
                                    onContextMenuClick()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.bike_component_snooze)) },
                                onClick = {
                                    onSnooze()
                                    onContextMenuClick()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.bike_component_alerts_off)) },
                                onClick = {
                                    onAlertsOff()
                                    onContextMenuClick()
                                },
                            )
                        }
                    }
                }
                if (component.makeModel.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.bike_component_make_model, component.makeModel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.bike_component_last_replaced, dateFormat.format(Date(component.installedAt))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.bike_component_used_km, component.distanceUsedKm, component.lifespanKm),
                    style = MaterialTheme.typography.bodySmall,
                )
                LinearProgressIndicator(
                    progress = { healthPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .semantics { contentDescription = healthDesc },
                )
                Text(
                    text = healthDesc,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
internal fun ComponentContextEditDialog(
    componentName: String,
    initialContext: ComponentContext?,
    componentId: Long,
    validationError: String?,
    onDismiss: () -> Unit,
    onSave: (ComponentContext) -> Unit,
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    var notes by remember(componentId) { mutableStateOf(initialContext?.notes ?: "") }
    var installDateMs by remember(componentId) { mutableStateOf(initialContext?.installDateMs) }
    var purchaseLink by remember(componentId) { mutableStateOf(initialContext?.purchaseLink ?: "") }
    var purchasePrice by remember(componentId) { mutableStateOf(initialContext?.purchasePrice ?: "") }
    var purchaseDateMs by remember(componentId) { mutableStateOf(initialContext?.purchaseDateMs) }
    var serialNumber by remember(componentId) { mutableStateOf(initialContext?.serialNumber ?: "") }
    var lastServiceNotes by remember(componentId) { mutableStateOf(initialContext?.lastServiceNotes ?: "") }
    val ctx = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$componentName — ${stringResource(R.string.component_context_section)}") },
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
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.component_context_notes)) },
                    placeholder = { Text(stringResource(R.string.component_context_notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = if (installDateMs != null) dateFormat.format(Date(installDateMs!!)) else "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.component_context_install_date)) },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            installDateMs?.let { cal.timeInMillis = it }
                            DatePickerDialog(
                                ctx,
                                { _, year, month, day ->
                                    cal.set(Calendar.YEAR, year)
                                    cal.set(Calendar.MONTH, month)
                                    cal.set(Calendar.DAY_OF_MONTH, day)
                                    installDateMs = cal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH),
                            ).show()
                        },
                    ) {
                        Text(if (installDateMs == null) stringResource(R.string.component_context_install_date) else stringResource(R.string.common_edit))
                    }
                }
                OutlinedTextField(
                    value = purchaseLink,
                    onValueChange = { purchaseLink = it },
                    label = { Text(stringResource(R.string.component_context_purchase_link)) },
                    placeholder = { Text(stringResource(R.string.component_context_purchase_link_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = serialNumber,
                    onValueChange = { serialNumber = it },
                    label = { Text(stringResource(R.string.component_context_serial_number)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = lastServiceNotes,
                    onValueChange = { lastServiceNotes = it },
                    label = { Text(stringResource(R.string.component_context_last_service_notes)) },
                    placeholder = { Text(stringResource(R.string.component_context_last_service_notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        ComponentContext(
                            componentId = componentId,
                            notes = notes,
                            installDateMs = installDateMs,
                            purchaseLink = purchaseLink.takeIf { it.isNotBlank() },
                            serialNumber = serialNumber.takeIf { it.isNotBlank() },
                            lastServiceNotes = lastServiceNotes.takeIf { it.isNotBlank() },
                            purchasePrice = purchasePrice.takeIf { it.isNotBlank() },
                            purchaseDateMs = purchaseDateMs,
                        ),
                    )
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
private fun RideSummaryCard(ride: RideEntity) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(dateFormat.format(Date(ride.endedAt)), style = MaterialTheme.typography.labelMedium)
            Text(stringResource(R.string.trip_ride_distance, ride.distanceKm), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
