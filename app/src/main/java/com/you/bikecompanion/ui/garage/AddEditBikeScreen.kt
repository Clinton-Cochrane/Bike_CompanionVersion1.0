package com.you.bikecompanion.ui.garage

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.you.bikecompanion.R
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.ui.garage.SaveOutcome
import com.you.bikecompanion.ui.navigation.Screen

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

    var name by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var drivetrainType by remember { mutableStateOf("") }
    var brakeType by remember { mutableStateOf("") }
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
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.setPickedImageUri(uri) }

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
            BikeImagePickerRow(
                thumbnailUri = uiState.bike?.thumbnailUri,
                pickedImageUri = uiState.pickedImageUri,
                removeImageRequested = uiState.removeImageRequested,
                onAddPhoto = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemovePhoto = { viewModel.setRemoveImageRequested() },
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
                    viewModel.saveBike(bike)
                },
                modifier = Modifier.fillMaxWidth(),
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
                    is SaveOutcome.Updated -> {
                        navController.navigateUp()
                        viewModel.clearSaveOutcome()
                    }
                    null -> { }
                }
            }
        }
    }
}

@Composable
private fun BikeImagePickerRow(
    thumbnailUri: String?,
    pickedImageUri: Uri?,
    removeImageRequested: Boolean,
    onAddPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    val hasImage = !removeImageRequested && (pickedImageUri != null || !thumbnailUri.isNullOrBlank())
    val imageModel = when {
        removeImageRequested -> null
        pickedImageUri != null -> pickedImageUri
        !thumbnailUri.isNullOrBlank() -> java.io.File(thumbnailUri)
        else -> null
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (imageModel) {
                null -> Icon(
                    imageVector = Icons.Filled.DirectionsBike,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (hasImage) {
                OutlinedButton(onClick = onAddPhoto) {
                    Text(stringResource(R.string.garage_bike_change_photo))
                }
                OutlinedButton(onClick = onRemovePhoto) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.garage_bike_remove_photo))
                }
            } else {
                OutlinedButton(onClick = onAddPhoto) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.garage_bike_add_photo))
                }
            }
        }
    }
}
