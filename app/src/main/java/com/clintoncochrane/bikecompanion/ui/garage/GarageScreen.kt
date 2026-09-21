package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.clintoncochrane.bikecompanion.R
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentCategory
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import com.clintoncochrane.bikecompanion.ui.navigation.Screen
import com.clintoncochrane.bikecompanion.ui.LocalBottomNavigationLiftController
import com.clintoncochrane.bikecompanion.util.ComponentSortOrder
import com.clintoncochrane.bikecompanion.util.minimumComponentHealthPercent
import com.clintoncochrane.bikecompanion.util.componentTypeIcon
import com.clintoncochrane.bikecompanion.util.DisplayFormatHelper
import com.clintoncochrane.bikecompanion.util.DurationFormatHelper
import com.clintoncochrane.bikecompanion.ui.garage.ThumbnailAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    navController: NavController,
    onStartRide: () -> Unit,
) {
    val viewModel = androidx.hilt.navigation.compose.hiltViewModel<GarageViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val bottomNavigationLiftController = LocalBottomNavigationLiftController.current
    val navigationBarClearance = with(LocalDensity.current) {
        bottomNavigationLiftController.navigationBarHeightPx.toDp()
    }
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
        AddComponentDialog(
            onDismiss = { showAddComponentDialog = false },
            onAdd = { request ->
                viewModel.addComponentToGarage(request)
                showAddComponentDialog = false
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            GarageTopBar(
                hasDueServiceItems = uiState.hasDueServiceItems,
                onStartRide = onStartRide,
                onServiceListClick = { navController.navigate(Screen.ServiceList.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
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
                    state = uiState.bikesOverview,
                    onBikeSelected = viewModel::selectBike,
                    onServiceDueClick = viewModel::openServiceSheet,
                    navController = navController,
                )
                GarageTab.Components -> PartsDirectoryContent(
                    state = uiState.partsDirectory,
                    onFilterChange = viewModel::setPartsDirectoryFilter,
                    onPartClick = { componentId ->
                        navController.navigate(Screen.ComponentDetail.withId(componentId))
                    },
                )
            }
        }
        }
        GarageServiceSheetHost(
            state = uiState.serviceSheet,
            onDismiss = viewModel::dismissServiceSheet,
            onToggle = viewModel::toggleServiceRequirement,
            onShowConfirmation = viewModel::showServiceConfirmation,
            onShowChecklist = viewModel::showServiceChecklist,
            onConfirm = viewModel::completeSelectedServiceRequirements,
            onRetry = viewModel::retryFailedServiceRequirements,
            onLiftChanged = bottomNavigationLiftController.onLiftChanged,
            navigationBarClearance = navigationBarClearance,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GarageTopBar(
    hasDueServiceItems: Boolean,
    onStartRide: () -> Unit,
    onServiceListClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    var garageMenuExpanded by remember { mutableStateOf(false) }
    val garageMenuContentDesc = stringResource(R.string.settings_content_description)

    TopAppBar(
        title = { Text(stringResource(R.string.garage_title)) },
        actions = {
            if (hasDueServiceItems) {
                IconButton(
                    onClick = onServiceListClick,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = stringResource(
                            R.string.garage_service_due_indicator_content_description,
                        ),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            com.clintoncochrane.bikecompanion.ui.StartRideAction(onStartRide)
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
                            onServiceListClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Build, contentDescription = null)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.garage_settings)) },
                        onClick = {
                            garageMenuExpanded = false
                            onSettingsClick()
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
}

@Composable
private fun PartsDirectoryContent(
    state: PartsDirectoryUiState,
    onFilterChange: (PartsDirectoryFilter) -> Unit,
    onPartClick: (Long) -> Unit,
) {
    var filterMenuExpanded by remember { mutableStateOf(false) }
    val selectedFilterLabel = partsFilterLabel(state.filter)
    val filterContentDescription = stringResource(R.string.garage_parts_filter_content_description)

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(vertical = 8.dp)) {
            FilterChip(
                selected = true,
                onClick = { filterMenuExpanded = true },
                label = {
                    Text(
                        text = selectedFilterLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingIcon = {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                },
                modifier = Modifier.semantics {
                    contentDescription = filterContentDescription
                },
            )
            DropdownMenu(
                expanded = filterMenuExpanded,
                onDismissRequest = { filterMenuExpanded = false },
            ) {
                PartsDirectoryFilter.entries.forEach { filter ->
                    DropdownMenuItem(
                        text = { Text(partsFilterLabel(filter)) },
                        onClick = {
                            onFilterChange(filter)
                            filterMenuExpanded = false
                        },
                    )
                }
            }
        }

        if (state.sections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (state.filter == PartsDirectoryFilter.ALL_PARTS) {
                            R.string.garage_parts_empty
                        } else {
                            R.string.garage_parts_filter_empty
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                state.sections.forEach { section ->
                    item(key = "parts-heading-${section.typeKey}") {
                        Text(
                            text = section.typeHeading,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 6.dp),
                        )
                    }
                    items(
                        items = section.rows,
                        key = { row -> "part-${row.componentId}" },
                    ) { row ->
                        PartDirectoryListRow(
                            row = row,
                            onClick = { onPartClick(row.componentId) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun partsFilterLabel(filter: PartsDirectoryFilter): String = when (filter) {
    PartsDirectoryFilter.ALL_PARTS -> stringResource(R.string.garage_parts_filter_all)
    PartsDirectoryFilter.ACTIVE -> stringResource(R.string.garage_parts_filter_active)
    PartsDirectoryFilter.RETIRED -> stringResource(R.string.garage_parts_filter_retired)
    PartsDirectoryFilter.WITHOUT_BIKES -> stringResource(R.string.garage_parts_filter_without_bikes)
}

@Composable
private fun PartDirectoryListRow(
    row: PartsDirectoryRow,
    onClick: () -> Unit,
) {
    val association = when (val value = row.association) {
        is PartAssociation.CurrentBike -> value.bikeName
        is PartAssociation.LastBike -> stringResource(R.string.garage_parts_last_bike_retired, value.bikeName)
        PartAssociation.NoBike -> stringResource(R.string.garage_parts_no_bike)
        PartAssociation.Retired -> stringResource(R.string.garage_parts_retired)
    }
    val distance = stringResource(
        if (row.isTrackedDistanceOnly) {
            R.string.garage_parts_distance_tracked
        } else {
            R.string.garage_parts_distance
        },
        row.lifetimeDistanceKm,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            row.secondaryLabel?.let { secondaryLabel ->
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = association,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = distance,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun BikesContent(
    state: GarageBikesUiState,
    onBikeSelected: (Int) -> Unit,
    onServiceDueClick: () -> Unit,
    navController: NavController,
) {
    if (state.bikes.isEmpty()) {
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
        BikeOverviewPager(
            state = state,
            onBikeSelected = onBikeSelected,
            onServiceDueClick = onServiceDueClick,
            onBikeClick = { navController.navigate(Screen.BikeDetail.withId(it.id)) },
            onRideClick = { navController.navigate(Screen.RideDetail.withId(it.id)) },
        )
    }
}

@Composable
private fun BikeOverviewPager(
    state: GarageBikesUiState,
    onBikeSelected: (Int) -> Unit,
    onServiceDueClick: () -> Unit,
    onBikeClick: (BikeEntity) -> Unit,
    onRideClick: (com.clintoncochrane.bikecompanion.data.ride.RideEntity) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { state.bikes.size })
    val previousBikeDescription = stringResource(R.string.garage_previous_bike)
    val nextBikeDescription = stringResource(R.string.garage_next_bike)
    val positionDescription = stringResource(
        R.string.garage_bike_position,
        state.selectedBikeIndex + 1,
        state.bikes.size,
    )

    LaunchedEffect(state.selectedBikeIndex) {
        if (pagerState.currentPage != state.selectedBikeIndex) {
            pagerState.animateScrollToPage(state.selectedBikeIndex)
        }
    }
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != state.selectedBikeIndex) {
            onBikeSelected(pagerState.currentPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            key = { state.bikes[it].id },
        ) { page ->
            val bike = state.bikes[page]
            BikeOverviewCard(bike = bike, onClick = { onBikeClick(bike) })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onBikeSelected(state.selectedBikeIndex - 1) },
                enabled = state.selectedBikeIndex > 0,
                modifier = Modifier.semantics {
                    contentDescription = previousBikeDescription
                },
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Text(
                text = positionDescription,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics {
                    contentDescription = positionDescription
                },
            )
            IconButton(
                onClick = { onBikeSelected(state.selectedBikeIndex + 1) },
                enabled = state.selectedBikeIndex < state.bikes.lastIndex,
                modifier = Modifier.semantics {
                    contentDescription = nextBikeDescription
                },
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }

        GarageBikeStatusCard(
            status = state.status,
            isActionable = state.dueServiceRequirements.isNotEmpty(),
            onClick = onServiceDueClick,
        )

        Text(
            text = stringResource(R.string.garage_recent_rides),
            style = MaterialTheme.typography.titleMedium,
        )
        if (state.recentRides.isEmpty()) {
            Text(
                text = stringResource(R.string.garage_recent_rides_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.recentRides.forEach { ride ->
                RecentRideRow(ride = ride, onClick = { onRideClick(ride) })
            }
        }
    }
}

@Composable
private fun BikeOverviewCard(bike: BikeEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = DisplayFormatHelper.bikeLabels(bike.name, bike.make, bike.model).primary,
                style = MaterialTheme.typography.headlineMedium,
            )
            val details = listOf(bike.make, bike.model, bike.year).filter(String::isNotBlank)
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.bike_odometer_km, bike.totalDistanceKm),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
internal fun GarageBikeStatusCard(
    status: GarageBikeStatusSummary,
    isActionable: Boolean,
    onClick: () -> Unit,
) {
    val (label, color) = when (status.level) {
        GarageBikeStatus.ReadyToRide -> stringResource(R.string.garage_status_ready_to_ride) to MaterialTheme.colorScheme.primary
        GarageBikeStatus.InspectSoon -> stringResource(R.string.garage_status_inspect_soon) to Color(0xFFB26A00)
        GarageBikeStatus.ServiceDue -> stringResource(R.string.garage_status_service_due) to MaterialTheme.colorScheme.error
    }
    val description = if (status.affectedComponentNames.isEmpty()) {
        label
    } else {
        stringResource(
            R.string.garage_status_affected_components,
            label,
            status.affectedComponentNames.joinToString(),
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isActionable, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium, color = color)
            if (status.affectedComponentNames.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.garage_status_components,
                        status.affectedComponentNames.joinToString(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun RecentRideRow(
    ride: com.clintoncochrane.bikecompanion.data.ride.RideEntity,
    onClick: () -> Unit,
) {
    val date = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ride.endedAt))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = date,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.trip_ride_distance, ride.distanceKm),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun GarageSpecSummaryCard(totalDistanceKm: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.garage_spec_total_distance, totalDistanceKm),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
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
        bikes.find { it.id == id }?.let { DisplayFormatHelper.bikeLabels(it.name, it.make, it.model).primary }
    } ?: stringResource(R.string.garage_filter_bike_all)
    val sortSummary = when (componentSortOrder) {
        ComponentSortOrder.TYPE_AZ -> stringResource(R.string.component_sort_type_az)
        ComponentSortOrder.NEXT_SERVICE -> stringResource(R.string.component_sort_next_service)
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
                                label = { Text(DisplayFormatHelper.bikeLabels(bike.name, bike.make, bike.model).primary) },
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
                    val minHealth = minimumComponentHealthPercent(categoryComponents)
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
    minHealth: Int?,
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
                        text = minHealth?.let { stringResource(R.string.bike_component_health, it) }
                            ?: stringResource(R.string.component_health_unavailable_short),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (minHealth != null) {
                    val healthDescription = stringResource(R.string.bike_component_health, minHealth)
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { minHealth / 100f },
                        modifier = Modifier
                            .size(32.dp, 32.dp)
                            .semantics { contentDescription = healthDescription },
                    )
                }
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
                            bikes.find { it.id == bid }?.let {
                                DisplayFormatHelper.bikeLabels(it.name, it.make, it.model).primary
                            }
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
            ThumbnailAvatar(
                size = 40.dp,
                placeholder = {
                    Icon(
                        imageVector = componentTypeIcon(component.type),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DisplayFormatHelper.componentLabels(
                        component.name, component.make, component.model, component.type,
                    ).primary,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = DisplayFormatHelper.componentLabels(
                        component.name, component.make, component.model, component.type,
                    ).secondary ?: DisplayFormatHelper.formatComponentTypeForDisplay(component.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.garage_component_assigned_to, assignedTo),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (component.priorUsageCertainty == PriorUsageCertainty.UNKNOWN) {
                    Text(
                        text = stringResource(R.string.component_health_unavailable),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (component.priorUsageCertainty == PriorUsageCertainty.UNKNOWN) {
                        stringResource(R.string.component_tracked_distance, component.distanceUsedKm)
                    } else {
                        stringResource(R.string.bike_stat_km, component.lifetimeDistanceKm)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = DurationFormatHelper.formatDurationBreakdownSeconds(component.totalTimeSeconds),
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
    healthPercent: Int?,
    hasAlert: Boolean,
    isLastRidden: Boolean,
    onClick: () -> Unit,
) {
    val alertContentDesc = stringResource(R.string.garage_bike_alert_content_description)
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val cardModifier = Modifier
        .fillMaxWidth()
        .then(
            if (isLastRidden) Modifier.border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            ) else Modifier,
        )
    Card(
        onClick = onClick,
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isLastRidden) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                ThumbnailAvatar(
                    size = 40.dp,
                    placeholder = {
                        Text(
                            text = "${DisplayFormatHelper.bikeLabels(bike.name, bike.make, bike.model).primary.firstOrNull()?.uppercaseChar() ?: "?"}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = DisplayFormatHelper.bikeLabels(bike.name, bike.make, bike.model).primary,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (isLastRidden && bike.lastRideAt != null) {
                            Text(
                                text = stringResource(R.string.garage_bike_last_ride, dateFormat.format(Date(bike.lastRideAt))),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
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
                    text = stringResource(R.string.bike_odometer_km, bike.totalDistanceKm),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = DurationFormatHelper.formatDurationBreakdownSeconds(bike.totalTimeSeconds),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.bike_stat_kmh, bike.avgSpeedKmh),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = healthPercent?.let { stringResource(R.string.garage_bike_health_score, it) }
                        ?: stringResource(R.string.component_health_unavailable_short),
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
