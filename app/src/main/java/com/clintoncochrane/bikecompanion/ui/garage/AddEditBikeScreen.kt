package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.clintoncochrane.bikecompanion.R
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.bike.BikeDeletionComponentDisposition
import com.clintoncochrane.bikecompanion.ui.garage.SaveOutcome
import com.clintoncochrane.bikecompanion.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBikeScreen(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    bikeId: Long?,
) {
    val viewModel: AddEditBikeViewModel = androidx.hilt.navigation.compose.hiltViewModel(
        viewModelStoreOwner = backStackEntry,
    )
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.bikeDeletionPrompt) {
        BikeDeletionPrompt.WithoutInstalledComponents -> {
            AlertDialog(
                onDismissRequest = viewModel::cancelBikeDeletion,
                title = { Text(stringResource(R.string.bike_delete_confirm_title)) },
                text = { Text(stringResource(R.string.bike_delete_confirm_without_components)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.confirmBikeDeletion(BikeDeletionComponentDisposition.MOVE_TO_GARAGE)
                        },
                    ) {
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelBikeDeletion) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
        BikeDeletionPrompt.WithInstalledComponents -> {
            AlertDialog(
                onDismissRequest = viewModel::cancelBikeDeletion,
                title = { Text(stringResource(R.string.bike_delete_components_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.bike_delete_components_message))
                        TextButton(
                            onClick = {
                                viewModel.confirmBikeDeletion(BikeDeletionComponentDisposition.RETIRE)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(R.string.bike_delete_retire_components),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.confirmBikeDeletion(BikeDeletionComponentDisposition.MOVE_TO_GARAGE)
                        },
                    ) {
                        Text(stringResource(R.string.bike_delete_move_components_to_garage))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelBikeDeletion) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
        null -> Unit
    }

    var name by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var drivetrainType by remember { mutableStateOf("") }
    var brakeType by remember { mutableStateOf("") }
    var startingOdometerInput by remember { mutableStateOf("0") }
    LaunchedEffect(uiState.bike) {
        uiState.bike?.let { b ->
            name = b.name
            make = b.make
            model = b.model
            year = b.year
            description = b.description
            notes = b.notes
            drivetrainType = b.drivetrainType
            brakeType = b.brakeType
            startingOdometerInput = b.baselineDistanceKm.toString().removeSuffix(".0")
        }
    }

    val backContentDesc = stringResource(R.string.common_back_content_description)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (bikeId != null) stringResource(R.string.common_edit)
                        else stringResource(R.string.garage_add_bike),
                    )
                },
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
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.bike_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = make,
                onValueChange = { make = it },
                label = { Text(stringResource(R.string.bike_make)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text(stringResource(R.string.bike_model)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text(stringResource(R.string.bike_year)) },
                modifier = Modifier.fillMaxWidth(),
            )
            val startingOdometerKm = parseStartingOdometerKm(startingOdometerInput)
            OutlinedTextField(
                value = startingOdometerInput,
                onValueChange = { startingOdometerInput = it },
                label = { Text(stringResource(R.string.bike_starting_odometer)) },
                supportingText = {
                    Text(
                        stringResource(
                            if (startingOdometerKm == null) R.string.bike_starting_odometer_invalid
                            else R.string.bike_starting_odometer_help,
                        ),
                    )
                },
                isError = startingOdometerKm == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.bike_description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.bike_notes)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
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
                listOf(
                    "1x" to R.string.add_bike_drivetrain_1x,
                    "single_speed" to R.string.add_bike_drivetrain_single_speed,
                    "multi_speed" to R.string.add_bike_drivetrain_multi_speed,
                ).forEach { (key, resId) ->
                    FilterChip(
                        selected = drivetrainType == key,
                        onClick = { drivetrainType = key },
                        label = { Text(stringResource(resId)) },
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
                listOf(
                    "rim" to R.string.add_bike_brake_rim,
                    "disc_mechanical" to R.string.add_bike_brake_disc_mechanical,
                    "disc_hydraulic" to R.string.add_bike_brake_disc_hydraulic,
                    "coaster" to R.string.add_bike_brake_coaster,
                    "other" to R.string.add_bike_brake_other,
                ).forEach { (key, resId) ->
                    FilterChip(
                        selected = brakeType == key,
                        onClick = { brakeType = key },
                        label = { Text(stringResource(resId)) },
                    )
                }
            }
            Button(
                onClick = {
                    val bike = uiState.bike?.copy(
                        name = name,
                        make = make,
                        model = model,
                        year = year,
                        description = description,
                        notes = notes,
                        drivetrainType = drivetrainType,
                        brakeType = brakeType,
                    ) ?: BikeEntity(
                        name = name,
                        make = make,
                        model = model,
                        year = year,
                        description = description,
                        notes = notes,
                        drivetrainType = drivetrainType,
                        brakeType = brakeType,
                        createdAt = System.currentTimeMillis(),
                    )
                    viewModel.saveBike(bike, startingOdometerInput)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.trim().isNotEmpty() && startingOdometerKm != null,
            ) {
                Text(stringResource(R.string.bike_save))
            }
            if (uiState.bike != null) {
                OutlinedButton(
                    onClick = viewModel::requestBikeDeletion,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.bike_delete), color = MaterialTheme.colorScheme.error)
                }
            }
            LaunchedEffect(uiState.saveOutcome) {
                when (val outcome = uiState.saveOutcome) {
                    is SaveOutcome.NewBike -> {
                        navController.navigate(Screen.BikeDetail.withId(outcome.id)) {
                            popUpTo(Screen.AddBike.route) { inclusive = true }
                        }
                        viewModel.clearSaveOutcome()
                    }
                    is SaveOutcome.Updated -> {
                        navController.navigateUp()
                        viewModel.clearSaveOutcome()
                    }
                    null -> { }
                }
            }
            LaunchedEffect(uiState.bikeDeleted) {
                if (uiState.bikeDeleted) {
                    navController.navigate(Screen.Garage.route) {
                        popUpTo(Screen.Garage.route) { inclusive = false }
                    }
                    viewModel.clearBikeDeleted()
                }
            }
        }
    }
}
