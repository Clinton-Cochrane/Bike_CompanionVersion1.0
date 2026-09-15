package com.clintoncochrane.bikecompanion.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clintoncochrane.bikecompanion.R
import com.clintoncochrane.bikecompanion.util.DurationFormatHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    onStartRide: () -> Unit,
) {
    val viewModel = androidx.hilt.navigation.compose.hiltViewModel<StatsViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                actions = {
                    com.clintoncochrane.bikecompanion.ui.StartRideAction(onStartRide)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { paddingValues ->
        StatsContent(
            uiState = uiState,
            onPreviousBike = viewModel::selectPreviousBike,
            onNextBike = viewModel::selectNextBike,
            onBikeSelected = viewModel::selectBike,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        )
    }
}

@Composable
fun StatsContent(
    uiState: StatsUiState,
    onPreviousBike: () -> Unit,
    onNextBike: () -> Unit,
    onBikeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatsCard(
            title = stringResource(R.string.stats_all_bikes),
            stats = uiState.allBikesStats,
            prominent = true,
        )

        if (uiState.bikesWithStats.isEmpty()) {
            Text(
                text = stringResource(R.string.stats_no_bikes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            BikeStatsPager(
                bikesWithStats = uiState.bikesWithStats,
                selectedBikeIndex = uiState.selectedBikeIndex,
                onPreviousBike = onPreviousBike,
                onNextBike = onNextBike,
                onBikeSelected = onBikeSelected,
            )
        }
    }
}

@Composable
private fun BikeStatsPager(
    bikesWithStats: List<BikeWithStats>,
    selectedBikeIndex: Int,
    onPreviousBike: () -> Unit,
    onNextBike: () -> Unit,
    onBikeSelected: (Int) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { bikesWithStats.size })

    LaunchedEffect(selectedBikeIndex) {
        if (pagerState.currentPage != selectedBikeIndex) {
            pagerState.animateScrollToPage(selectedBikeIndex)
        }
    }
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != selectedBikeIndex) {
            onBikeSelected(pagerState.currentPage)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            key = { bikesWithStats[it].bike.id },
        ) { page ->
            val bikeWithStats = bikesWithStats[page]
            StatsCard(
                title = bikeWithStats.bike.name,
                stats = bikeWithStats.stats,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPreviousBike,
                enabled = selectedBikeIndex > 0,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.stats_previous_bike),
                )
            }
            Text(
                text = stringResource(
                    R.string.stats_bike_position,
                    selectedBikeIndex + 1,
                    bikesWithStats.size,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
            IconButton(
                onClick = onNextBike,
                enabled = selectedBikeIndex < bikesWithStats.lastIndex,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.stats_next_bike),
                )
            }
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    stats: StatsSummary,
    prominent: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (prominent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = if (prominent) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.stats_distance, stats.totalDistanceKm),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(
                    R.string.stats_ride_time,
                    DurationFormatHelper.formatDurationMs(stats.totalRideDurationMs),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.stats_rides, stats.rideCount),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.stats_services, stats.completedServiceCount),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
