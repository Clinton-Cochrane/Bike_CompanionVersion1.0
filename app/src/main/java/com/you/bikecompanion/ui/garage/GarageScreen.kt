package com.you.bikecompanion.ui.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.you.bikecompanion.R
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.component.ComponentCategory
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.component.DefaultComponentTypes
import com.you.bikecompanion.ui.navigation.Screen
import com.you.bikecompanion.util.ComponentSortOrder
import com.you.bikecompanion.util.componentHealthPercent
import com.you.bikecompanion.util.componentTypeIcon
import com.you.bikecompanion.util.DisplayFormatHelper
import com.you.bikecompanion.util.DurationFormatHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    navController: NavController,
) {
    val viewModel = androidx.hilt.navigation.compose.hiltViewModel<GarageViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    var showAddComponentDialog by remember { mutableStateOf(false) }

    val fabContentDesc = when (uiState.selectedTab) {
        GarageTab.Bikes -> stringResource(R.string.garage_add_bike_content_description)
        GarageTab.Components -> stringResource(R.string.garage_add_component_content_description)
    }
    val onFabClick = when (uiState.selectedTab) {
        GarageTab.Bikes -> ({ navController.navigate(Screen.AddBike.route) })
        GarageTab.Components -> ({ showAddComponentDialog = true })
    }

    if (showAddComponentDialog) {
        AlertDialog(
            onDismissRequest = { showAddComponentDialog = false },
            title = { Text(stringResource(R.string.component_suggested_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DefaultComponentTypes.SUGGESTED.forEach { suggested ->
                        TextButton(
                            onClick = {
                                viewModel.addComponentToGarage(
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
            var garageMenuExpanded by remember { mutableStateOf(false) }
            val garageMenuContentDesc = stringResource(R.string.settings_content_description)
            TopAppBar(
                title = { Text(stringResource(R.string.garage_title)) },
                actions = {
                    Box {
                        IconButton(
                            onClick = { garageMenuExpanded = true },
                            modifier = Modifier.semantics { contentDescription = garageMenuContentDesc },
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = garageMenuExpanded,
                            onDismissRequest = { garageMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.service_list)) },
                                onClick = {
                                    garageMenuExpanded = false
                                    navController.navigate(Screen.ServiceList.route)
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Build, contentDescription = null)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.garage_settings)) },
                                onClick = {
                                    garageMenuExpanded = false
                                    navController.navigate(Screen.Settings.route)
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Settings, contentDescription = null)
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick,
                modifier = Modifier.semantics {
                    contentDescription = fabContentDesc
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.selectedTab == GarageTab.Bikes,
                    onClick = { viewModel.setSelectedTab(GarageTab.Bikes) },
                    label = { Text(stringResource(R.string.garage_tab_bikes)) },
                )
                FilterChip(
                    selected = uiState.selectedTab == GarageTab.Components,
                    onClick = { viewModel.setSelectedTab(GarageTab.Components) },
                    label = { Text(stringResource(R.string.garage_tab_components)) },
                )
            }

            when (uiState.selectedTab) {
                GarageTab.Bikes -> BikesContent(
                    bikes = uiState.bikes,
                    bikeHealth = uiState.bikeHealth,
                    bikeHasAlert = uiState.bikeHasAlert,
                    navController = navController,
                )
                GarageTab.Components -> ComponentsContent(
                    components = uiState.garageComponents,
                    bikes = uiState.bikes,
                    typeFilter = uiState.componentTypeFilter,
                    onTypeFilterChange = viewModel::setComponentTypeFilter,
                    bikeFilterId = uiState.componentBikeFilter,
                    onBikeFilterChange = viewModel::setComponentBikeFilter,
                    componentSortOrder = uiState.componentSortOrder,
                    onSortOrderChange = viewModel::setComponentSortOrder,
                    navController = navController,
                )
            }
        }
    }
}

@Composable
private fun BikesContent(
    bikes: List<BikeEntity>,
    bikeHealth: Map<Long, Int>,
    bikeHasAlert: Set<Long>,
    navController: NavController,
) {
    if (bikes.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.DirectionsBike,
                contentDescription = null,
                modifier = Modifier.padding(24.dp),
            )
            Text(
                text = stringResource(R.string.garage_no_bikes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
        ) {
            items(bikes, key = { it.id }) { bike ->
                BikeCard(
                    bike = bike,
                    healthPercent = bikeHealth[bike.id] ?: 100,
                    hasAlert = bike.id in bikeHasAlert,
                    onClick = { navController.navigate(Screen.BikeDetail.withId(bike.id)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComponentsContent(
    components: List<ComponentEntity>,
    bikes: List<BikeEntity>,
    typeFilter: String?,
    onTypeFilterChange: (String?) -> Unit,
    bikeFilterId: Long?,
    onBikeFilterChange: (Long?) -> Unit,
    componentSortOrder: ComponentSortOrder,
    onSortOrderChange: (ComponentSortOrder) -> Unit,
    navController: NavController,
) {
    val filteredByType = if (typeFilter == null) {
        components
    } else {
        components.filter { it.type == typeFilter }
    }
    val filteredComponents = if (bikeFilterId == null) {
        filteredByType
    } else {
        filteredByType.filter { it.bikeId == bikeFilterId }
    }
    val distinctTypes = components.map { it.type }.distinct().sorted()
    val componentsByCategory = remember(filteredComponents) {
        filteredComponents
            .groupBy { ComponentCategory.fromComponentType(it.type) }
            .mapValues { (_, comps) -> comps }
    }
    val categoriesWithComponents = ComponentCategory.displayOrder.filter {
        (componentsByCategory[it]?.size ?: 0) > 0
    }
    var expandedCategories by remember { mutableStateOf(setOf<ComponentCategory>()) }
    var filterMenuExpanded by remember { mutableStateOf(false) }

    val typeSummary = typeFilter?.let { DisplayFormatHelper.formatComponentTypeForDisplay(it) }
        ?: stringResource(R.string.garage_filter_all)
    val bikeSummary = bikeFilterId?.let { id ->
        bikes.find { it.id == id }?.name
    } ?: stringResource(R.string.garage_filter_bike_all)
    val sortSummary = when (componentSortOrder) {
        ComponentSortOrder.TYPE_AZ -> stringResource(R.string.component_sort_type_az)
        ComponentSortOrder.NEXT_SERVICE -> stringResource(R.string.component_sort_next_service)
        ComponentSortOrder.HEALTH -> stringResource(R.string.component_sort_health)
    }
    val filterSortSummary = stringResource(
        R.string.garage_filter_sort_summary,
        typeSummary,
        bikeSummary,
        sortSummary,
    )
    val filterSortContentDescription = stringResource(R.string.garage_filter_sort_content_description)

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(vertical = 8.dp)) {
            Card(
                onClick = { filterMenuExpanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = filterSortContentDescription },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = filterSortSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DropdownMenu(
                expanded = filterMenuExpanded,
                onDismissRequest = { filterMenuExpanded = false },
                modifier = Modifier.fillMaxWidth(0.92f),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.garage_filter_type_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = typeFilter == null,
                            onClick = { onTypeFilterChange(null) },
                            label = { Text(stringResource(R.string.garage_filter_all)) },
                        )
                        distinctTypes.forEach { type ->
                            FilterChip(
                                selected = typeFilter == type,
                                onClick = { onTypeFilterChange(type) },
                                label = { Text(DisplayFormatHelper.formatComponentTypeForDisplay(type)) },
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.garage_filter_bike_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = bikeFilterId == null,
                            onClick = { onBikeFilterChange(null) },
                            label = { Text(stringResource(R.string.garage_filter_bike_all)) },
                        )
                        bikes.forEach { bike ->
                            FilterChip(
                                selected = bikeFilterId == bike.id,
                                onClick = { onBikeFilterChange(bike.id) },
                                label = { Text(bike.name) },
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.garage_filter_sort_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = componentSortOrder == ComponentSortOrder.TYPE_AZ,
                            onClick = { onSortOrderChange(ComponentSortOrder.TYPE_AZ) },
                            label = { Text(stringResource(R.string.component_sort_type_az)) },
                        )
                        FilterChip(
                            selected = componentSortOrder == ComponentSortOrder.NEXT_SERVICE,
                            onClick = { onSortOrderChange(ComponentSortOrder.NEXT_SERVICE) },
                            label = { Text(stringResource(R.string.component_sort_next_service)) },
                        )
                        FilterChip(
                            selected = componentSortOrder == ComponentSortOrder.HEALTH,
                            onClick = { onSortOrderChange(ComponentSortOrder.HEALTH) },
                            label = { Text(stringResource(R.string.component_sort_health)) },
                        )
                    }
                }
            }
        }

        if (filteredComponents.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.padding(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.garage_no_components),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
            ) {
                items(categoriesWithComponents, key = { it.name }) { category ->
                    val categoryComponents = componentsByCategory[category] ?: emptyList()
                    val minHealth = categoryComponents.minOfOrNull { componentHealthPercent(it) } ?: 100
                    val isExpanded = category in expandedCategories
                    GarageCategorySection(
                        category = category,
                        components = categoryComponents,
                        bikes = bikes,
                        minHealth = minHealth,
                        isExpanded = isExpanded,
                        onToggleExpanded = {
                            expandedCategories = if (isExpanded) {
                                expandedCategories - category
                            } else {
                                expandedCategories + category
                            }
                        },
                        onComponentClick = { navController.navigate(Screen.ComponentDetail.withId(it.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GarageCategorySection(
    category: ComponentCategory,
    components: List<ComponentEntity>,
    bikes: List<BikeEntity>,
    minHealth: Int,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onComponentClick: (ComponentEntity) -> Unit,
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
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { minHealth / 100f },
                    modifier = Modifier.size(32.dp, 32.dp),
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded,
                enter = androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    components.forEach { component ->
                        val assignedTo = component.bikeId?.let { bid ->
                            bikes.find { it.id == bid }?.name
                        } ?: stringResource(R.string.garage_assigned_none)
                        GarageComponentCard(
                            component = component,
                            assignedTo = assignedTo,
                            onClick = { onComponentClick(component) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GarageComponentCard(
    component: ComponentEntity,
    assignedTo: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = componentTypeIcon(component.type),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DisplayFormatHelper.formatForDisplay(component.name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = DisplayFormatHelper.formatComponentTypeForDisplay(component.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.garage_component_assigned_to, assignedTo),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.bike_stat_km, component.distanceUsedKm),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = DurationFormatHelper.formatDurationSeconds(component.totalTimeSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BikeCard(
    bike: BikeEntity,
    healthPercent: Int,
    hasAlert: Boolean,
    onClick: () -> Unit,
) {
    val alertContentDesc = stringResource(R.string.garage_bike_alert_content_description)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text(
                        text = "${bike.name.firstOrNull()?.uppercaseChar() ?: "?"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bike.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (bike.make.isNotEmpty() || bike.model.isNotEmpty() || bike.year.isNotEmpty()) {
                        Text(
                            text = listOf(bike.make, bike.model, bike.year).filter { it.isNotEmpty() }.joinToString(" "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.bike_stat_km, bike.totalDistanceKm),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = DurationFormatHelper.formatDurationSeconds(bike.totalTimeSeconds),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.bike_stat_kmh, bike.avgSpeedKmh),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.garage_bike_health_score, healthPercent),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (hasAlert) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = alertContentDesc,
                            modifier = Modifier
                                .size(24.dp)
                                .semantics { contentDescription = alertContentDesc },
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
