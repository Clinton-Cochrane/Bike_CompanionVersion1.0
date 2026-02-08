package com.you.bikecompanion.ui.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.you.bikecompanion.R
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.ui.navigation.Screen
import com.you.bikecompanion.util.ComponentSortOrder
import com.you.bikecompanion.util.DisplayFormatHelper
import com.you.bikecompanion.util.componentTypeIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceListScreen(
    navController: NavController,
) {
    val viewModel = androidx.hilt.navigation.compose.hiltViewModel<ServiceListViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    var filterMenuExpanded by remember { mutableStateOf(false) }

    val backContentDesc = stringResource(R.string.common_back_content_description)
    val filterSortContentDesc = stringResource(R.string.garage_filter_sort_content_description)
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.service_list_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.semantics { contentDescription = backContentDesc },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.service_list_search_hint)) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )

            Box(modifier = Modifier.padding(vertical = 8.dp)) {
                Card(
                    onClick = { filterMenuExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = filterSortContentDesc },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val typeSummary = uiState.componentTypeFilter?.let { DisplayFormatHelper.formatComponentTypeForDisplay(it) }
                            ?: stringResource(R.string.service_list_filter_all)
                        val bikeSummary = uiState.bikeFilterId?.let { id ->
                            uiState.bikes.find { it.id == id }?.name
                        } ?: stringResource(R.string.garage_filter_bike_all)
                        val sortSummary = when (uiState.sortOrder) {
                            ComponentSortOrder.TYPE_AZ -> stringResource(R.string.component_sort_type_az)
                            ComponentSortOrder.NEXT_SERVICE -> stringResource(R.string.component_sort_next_service)
                            ComponentSortOrder.HEALTH -> stringResource(R.string.component_sort_health)
                        }
                        Text(
                            text = "$typeSummary · $bikeSummary · $sortSummary",
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
                            .heightIn(max = 400.dp)
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
                                selected = uiState.componentTypeFilter == null,
                                onClick = { viewModel.setComponentTypeFilter(null); filterMenuExpanded = false },
                                label = { Text(stringResource(R.string.service_list_filter_all)) },
                            )
                            uiState.allDueItems.map { it.component.type }.distinct().sorted().forEach { type ->
                                FilterChip(
                                    selected = uiState.componentTypeFilter == type,
                                    onClick = { viewModel.setComponentTypeFilter(type); filterMenuExpanded = false },
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
                                selected = uiState.bikeFilterId == null,
                                onClick = { viewModel.setBikeFilter(null); filterMenuExpanded = false },
                                label = { Text(stringResource(R.string.garage_filter_bike_all)) },
                            )
                            uiState.bikes.forEach { bike ->
                                FilterChip(
                                    selected = uiState.bikeFilterId == bike.id,
                                    onClick = { viewModel.setBikeFilter(bike.id); filterMenuExpanded = false },
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
                                selected = uiState.sortOrder == ComponentSortOrder.TYPE_AZ,
                                onClick = { viewModel.setSortOrder(ComponentSortOrder.TYPE_AZ); filterMenuExpanded = false },
                                label = { Text(stringResource(R.string.component_sort_type_az)) },
                            )
                            FilterChip(
                                selected = uiState.sortOrder == ComponentSortOrder.NEXT_SERVICE,
                                onClick = { viewModel.setSortOrder(ComponentSortOrder.NEXT_SERVICE); filterMenuExpanded = false },
                                label = { Text(stringResource(R.string.component_sort_next_service)) },
                            )
                            FilterChip(
                                selected = uiState.sortOrder == ComponentSortOrder.HEALTH,
                                onClick = { viewModel.setSortOrder(ComponentSortOrder.HEALTH); filterMenuExpanded = false },
                                label = { Text(stringResource(R.string.component_sort_health)) },
                            )
                        }
                    }
                }
            }

            val selectAllNoneLabel = if (uiState.selectedIds.isNotEmpty()) {
                stringResource(R.string.service_list_select_none)
            } else {
                stringResource(R.string.service_list_select_all)
            }
            OutlinedButton(
                onClick = {
                    if (uiState.selectedIds.isNotEmpty()) viewModel.selectNone()
                    else viewModel.selectAll()
                },
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Text(selectAllNoneLabel)
            }

            if (uiState.dueItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        modifier = Modifier.padding(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.service_list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = if (uiState.selectedIds.isNotEmpty()) 80.dp else 24.dp, top = 8.dp),
                ) {
                    items(uiState.dueItems, key = { it.component.id }) { item ->
                        ServiceListRow(
                            item = item,
                            isSelected = item.component.id in uiState.selectedIds,
                            onToggleSelect = { viewModel.toggleSelection(item.component.id) },
                            onReplace = { viewModel.replaceComponent(item.component.id) },
                            onInspect = { viewModel.completeInspection(item.component.id) },
                            onRowClick = { navController.navigate(Screen.ComponentDetail.withId(item.component.id)) },
                        )
                    }
                }
            }

            if (uiState.selectedIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.service_list_selected_count, uiState.selectedIds.size),
                        modifier = Modifier.align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(
                        onClick = { viewModel.replaceSelected() },
                    ) {
                        Text(stringResource(R.string.service_list_replace_selected))
                    }
                    OutlinedButton(
                        onClick = { viewModel.completeInspectionSelected() },
                    ) {
                        Text(stringResource(R.string.service_list_inspect_selected))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceListRow(
    item: DueServiceItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onReplace: () -> Unit,
    onInspect: () -> Unit,
    onRowClick: () -> Unit,
) {
    Card(
        onClick = onRowClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = componentTypeIcon(item.component.type),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DisplayFormatHelper.formatForDisplay(item.component.name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = DisplayFormatHelper.formatComponentTypeForDisplay(item.component.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.bikeName.ifEmpty { "-" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.nextDueText.isNotBlank()) {
                    Text(
                        text = item.nextDueText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.bike_component_health, item.healthPercent),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedButton(
                        onClick = onReplace,
                        modifier = Modifier.padding(0.dp),
                    ) {
                        Text(stringResource(R.string.service_list_replace), style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onInspect,
                        modifier = Modifier.padding(0.dp),
                    ) {
                        Text(stringResource(R.string.service_list_inspect), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
