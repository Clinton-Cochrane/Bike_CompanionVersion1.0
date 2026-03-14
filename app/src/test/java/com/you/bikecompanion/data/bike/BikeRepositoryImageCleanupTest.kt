package com.you.bikecompanion.data.bike

import com.you.bikecompanion.data.image.ImageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying [BikeRepository] cleans up images when deleting a bike.
 */
class BikeRepositoryImageCleanupTest {

    private lateinit var bikeDao: BikeDao
    private lateinit var imageRepository: ImageRepository
    private lateinit var repository: BikeRepository

    @Before
    fun setUp() {
        bikeDao = mockk()
        imageRepository = mockk(relaxed = true)
        repository = BikeRepository(bikeDao, imageRepository)
    }

    @Test
    fun deleteBike_deletesImageBeforeDeletingFromDao() = runTest {
        val bike = BikeEntity(id = 1L, name = "Test", createdAt = 1000L)
        coEvery { bikeDao.deleteById(1L) } coAnswers { }

        repository.deleteBike(bike)

        coVerify { imageRepository.deleteBikeImage(1L) }
        coVerify { bikeDao.deleteById(1L) }
    }
}
