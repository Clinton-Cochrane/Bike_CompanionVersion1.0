package com.you.bikecompanion.ui.garage

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.component.ComponentRepository
import com.you.bikecompanion.data.image.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Result of saving a bike: navigate to new bike detail (after seeding) or back (edit). */
sealed class SaveOutcome {
    data class NewBike(val id: Long) : SaveOutcome()
    data object Updated : SaveOutcome()
}

data class AddEditBikeUiState(
    val bike: BikeEntity? = null,
    val saveOutcome: SaveOutcome? = null,
    /** User-picked image URI; shown before save. Cleared after save. */
    val pickedImageUri: Uri? = null,
    /** User requested removal of image; show placeholder until save. */
    val removeImageRequested: Boolean = false,
)

@HiltViewModel
class AddEditBikeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bikeRepository: BikeRepository,
    private val componentRepository: ComponentRepository,
    private val imageRepository: ImageRepository,
) : ViewModel() {

    private val bikeId: Long? = savedStateHandle.get<String>("bikeId")?.toLongOrNull()?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(AddEditBikeUiState())
    val uiState: StateFlow<AddEditBikeUiState> = _uiState.asStateFlow()

    init {
        bikeId?.let { id ->
            viewModelScope.launch {
                val bike = bikeRepository.getBikeById(id)
                _uiState.update { it.copy(bike = bike) }
            }
        }
    }

    fun setPickedImageUri(uri: Uri?) {
        _uiState.update {
            it.copy(pickedImageUri = uri, removeImageRequested = false)
        }
    }

    fun setRemoveImageRequested() {
        _uiState.update {
            it.copy(pickedImageUri = null, removeImageRequested = true)
        }
    }

    fun saveBike(bike: BikeEntity) {
        viewModelScope.launch {
            val state = _uiState.value
            var bikeToSave = bike

            if (bike.id > 0) {
                if (state.removeImageRequested) {
                    imageRepository.deleteImageAtPath(bike.thumbnailUri)
                    bikeToSave = bike.copy(thumbnailUri = null)
                } else if (state.pickedImageUri != null) {
                    val path = imageRepository.saveBikeImage(bike.id, state.pickedImageUri)
                    bikeToSave = bike.copy(thumbnailUri = path)
                }
                bikeRepository.updateBike(bikeToSave)
                _uiState.update {
                    it.copy(saveOutcome = SaveOutcome.Updated, pickedImageUri = null, removeImageRequested = false)
                }
            } else {
                val newId = bikeRepository.insertBike(bikeToSave)
                if (state.pickedImageUri != null) {
                    val path = imageRepository.saveBikeImage(newId, state.pickedImageUri)
                    bikeRepository.updateBike(bikeToSave.copy(id = newId, thumbnailUri = path))
                }
                componentRepository.seedDefaultComponentsIfEmpty(newId)
                _uiState.update {
                    it.copy(saveOutcome = SaveOutcome.NewBike(newId), pickedImageUri = null, removeImageRequested = false)
                }
            }
        }
    }

    fun clearSaveOutcome() {
        _uiState.update { it.copy(saveOutcome = null) }
    }
}
