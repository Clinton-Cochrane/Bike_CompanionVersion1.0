package com.you.bikecompanion.ui.garage

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.bike.BikeRepository
import com.you.bikecompanion.data.component.ComponentRepository
import com.you.bikecompanion.data.image.ImageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AddEditBikeViewModel] image handling.
 */
class AddEditBikeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var bikeRepository: BikeRepository
    private lateinit var componentRepository: ComponentRepository
    private lateinit var imageRepository: ImageRepository
    private lateinit var viewModel: AddEditBikeViewModel

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        bikeRepository = mockk()
        componentRepository = mockk()
        imageRepository = mockk()
    }

    @Test
    fun setPickedImageUri_updatesState() = runTest(testDispatcher) {
        viewModel = AddEditBikeViewModel(
            SavedStateHandle(mapOf()),
            bikeRepository,
            componentRepository,
            imageRepository,
        )
        val uri = Uri.parse("content://test/1")

        viewModel.setPickedImageUri(uri)

        assertEquals(uri, viewModel.uiState.value.pickedImageUri)
        assertEquals(false, viewModel.uiState.value.removeImageRequested)
    }

    @Test
    fun setRemoveImageRequested_clearsPickedAndSetsFlag() = runTest(testDispatcher) {
        viewModel = AddEditBikeViewModel(
            SavedStateHandle(mapOf()),
            bikeRepository,
            componentRepository,
            imageRepository,
        )
        viewModel.setPickedImageUri(Uri.parse("content://test/1"))

        viewModel.setRemoveImageRequested()

        assertNull(viewModel.uiState.value.pickedImageUri)
        assertEquals(true, viewModel.uiState.value.removeImageRequested)
    }

    @Test
    fun saveBike_newBike_copiesImageAndUpdatesEntity() = runTest(testDispatcher) {
        coEvery { bikeRepository.insertBike(any()) } returns 42L
        coEvery { bikeRepository.updateBike(any()) } coAnswers { }
        coEvery { componentRepository.seedDefaultComponentsIfEmpty(42L) } coAnswers { }
        coEvery { imageRepository.saveBikeImage(42L, any()) } returns "/path/to/bike_42.jpg"

        viewModel = AddEditBikeViewModel(
            SavedStateHandle(mapOf()),
            bikeRepository,
            componentRepository,
            imageRepository,
        )
        viewModel.setPickedImageUri(Uri.parse("content://test/pick"))

        viewModel.saveBike(
            BikeEntity(name = "Test", createdAt = 1000L),
        )
        advanceUntilIdle()

        coVerify { imageRepository.saveBikeImage(42L, any()) }
        coVerify { bikeRepository.updateBike(match { it.thumbnailUri == "/path/to/bike_42.jpg" }) }
    }

    @Test
    fun saveBike_editWithRemove_deletesImageAndClearsThumbnail() = runTest(testDispatcher) {
        val bike = BikeEntity(
            id = 1L,
            name = "Test",
            createdAt = 1000L,
            thumbnailUri = "/old/path.jpg",
        )
        coEvery { bikeRepository.getBikeById(1L) } returns bike
        coEvery { bikeRepository.updateBike(any()) } coAnswers { }
        coEvery { imageRepository.deleteImageAtPath("/old/path.jpg") } coAnswers { }

        viewModel = AddEditBikeViewModel(
            SavedStateHandle(mapOf("bikeId" to "1")),
            bikeRepository,
            componentRepository,
            imageRepository,
        )
        advanceUntilIdle()
        viewModel.setRemoveImageRequested()

        viewModel.saveBike(bike)
        advanceUntilIdle()

        coVerify { imageRepository.deleteImageAtPath("/old/path.jpg") }
        coVerify { bikeRepository.updateBike(match { it.thumbnailUri == null }) }
    }

    @Test
    fun saveBike_editWithNewImage_replacesThumbnail() = runTest(testDispatcher) {
        val bike = BikeEntity(id = 1L, name = "Test", createdAt = 1000L)
        coEvery { bikeRepository.getBikeById(1L) } returns bike
        coEvery { bikeRepository.updateBike(any()) } coAnswers { }
        coEvery { imageRepository.saveBikeImage(1L, any()) } returns "/new/path.jpg"

        viewModel = AddEditBikeViewModel(
            SavedStateHandle(mapOf("bikeId" to "1")),
            bikeRepository,
            componentRepository,
            imageRepository,
        )
        advanceUntilIdle()
        viewModel.setPickedImageUri(Uri.parse("content://test/new"))

        viewModel.saveBike(bike)
        advanceUntilIdle()

        coVerify { imageRepository.saveBikeImage(1L, any()) }
        coVerify { bikeRepository.updateBike(match { it.thumbnailUri == "/new/path.jpg" }) }
    }
}
