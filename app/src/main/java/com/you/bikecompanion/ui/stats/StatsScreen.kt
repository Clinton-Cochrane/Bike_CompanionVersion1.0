package com.you.bikecompanion.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.you.bikecompanion.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
) {
    val viewModel = androidx.hilt.navigation.compose.hiltViewModel<StatsViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
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
                .padding(16.dp),
        ) {
            if (uiState.bikesWithStats.isEmpty()) {
                Text(
                    text = stringResource(R.string.garage_no_bikes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "all") {
                        FilterChip(
                            selected = uiState.filterBikeId == null,
                            onClick = { viewModel.setFilterBikeId(null) },
                            label = { Text(stringResource(R.string.stats_filter_all)) },
                        )
                    }
                    items(uiState.bikesWithStats, key = { it.bike.id }) { bikeWithStats ->
                        FilterChip(
                            selected = uiState.filterBikeId == bikeWithStats.bike.id,
                            onClick = { viewModel.setFilterBikeId(bikeWithStats.bike.id) },
                            label = { Text(bikeWithStats.bike.name) },
                        )
                    }
                }

                val visibleBikes = if (uiState.filterBikeId == null) {
                    uiState.bikesWithStats
                } else {
                    uiState.bikesWithStats.filter { it.bike.id == uiState.filterBikeId }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visibleBikes, key = { it.bike.id }) { bikeWithStats ->
                        StatsCard(
                            bikeName = bikeWithStats.bike.name,
                            stats = bikeWithStats.stats,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(
    bikeName: String,
    stats: BikeStats?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = bikeName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (stats != null) {
                Text(
                    stringResource(R.string.stats_total_distance, stats.totalDistanceKm),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(R.string.stats_ride_count, stats.rideCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.stats_avg_distance, stats.avgDistanceKm),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.stats_avg_speed, stats.avgSpeedKmh),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.stats_elevation_gain, stats.totalElevGainM),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = stringResource(R.string.stats_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
