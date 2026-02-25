package com.you.bikecompanion.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

private val DRIVETRAIN_OPTIONS = listOf(
    "1x" to "1x",
    "single_speed" to "single_speed",
    "multi_speed" to "multi_speed",
)
private val BRAKE_OPTIONS = listOf(
    "rim" to "rim",
    "disc_mechanical" to "disc_mechanical",
    "disc_hydraulic" to "disc_hydraulic",
    "coaster" to "coaster",
    "other" to "other",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleAddBikeScreen(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
) {
    val viewModel: SimpleAddBikeViewModel = androidx.hilt.navigation.compose.hiltViewModel(
        viewModelStoreOwner = backStackEntry,
    )
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var drivetrainType by remember { mutableStateOf("1x") }
    var brakeType by remember { mutableStateOf("disc_mechanical") }

    val backContentDesc = stringResource(R.string.common_back_content_description)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.add_bike_quick_add)) },
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
    ) { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.bike_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.add_bike_drivetrain_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DRIVETRAIN_OPTIONS.forEach { (_, key) ->
                    val label = when (key) {
                        "1x" -> stringResource(R.string.add_bike_drivetrain_1x)
                        "single_speed" -> stringResource(R.string.add_bike_drivetrain_single_speed)
                        "multi_speed" -> stringResource(R.string.add_bike_drivetrain_multi_speed)
                        else -> key
                    }
                    FilterChip(
                        selected = drivetrainType == key,
                        onClick = { drivetrainType = key },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.add_bike_brake_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BRAKE_OPTIONS.forEach { (_, key) ->
                    val label = when (key) {
                        "rim" -> stringResource(R.string.add_bike_brake_rim)
                        "disc_mechanical" -> stringResource(R.string.add_bike_brake_disc_mechanical)
                        "disc_hydraulic" -> stringResource(R.string.add_bike_brake_disc_hydraulic)
                        "coaster" -> stringResource(R.string.add_bike_brake_coaster)
                        "other" -> stringResource(R.string.add_bike_brake_other)
                        else -> key
                    }
                    FilterChip(
                        selected = brakeType == key,
                        onClick = { brakeType = key },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.add_bike_quick_add_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    viewModel.saveBike(name, drivetrainType, brakeType)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.trim().isNotEmpty(),
            ) {
                Text(stringResource(R.string.bike_save))
            }
            LaunchedEffect(uiState.saveOutcome) {
                when (val outcome = uiState.saveOutcome) {
                    is SaveOutcome.NewBike -> {
                        navController.navigate(Screen.BikeDetail.withId(outcome.id)) {
                            popUpTo(Screen.AddBike.route) { inclusive = true }
                        }
                        viewModel.clearSaveOutcome()
                    }
                    null -> { }
                    is SaveOutcome.Updated -> { }
                }
            }
        }
    }
}
