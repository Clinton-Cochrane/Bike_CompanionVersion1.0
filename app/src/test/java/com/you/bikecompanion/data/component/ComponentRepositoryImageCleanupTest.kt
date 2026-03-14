package com.you.bikecompanion.data.component

import com.you.bikecompanion.data.bike.BikeDao
import com.you.bikecompanion.data.image.ImageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying [ComponentRepository] cleans up images when deleting a component.
 */
class ComponentRepositoryImageCleanupTest {

    private lateinit var componentDao: ComponentDao
    private lateinit var serviceIntervalDao: ServiceIntervalDao
    private lateinit var componentSwapDao: ComponentSwapDao
    private lateinit var bikeDao: BikeDao
    private lateinit var imageRepository: ImageRepository
    private lateinit var repository: ComponentRepository

    @Before
    fun setUp() {
        componentDao = mockk()
        serviceIntervalDao = mockk()
        componentSwapDao = mockk()
        bikeDao = mockk()
        imageRepository = mockk(relaxed = true)
        repository = ComponentRepository(
            componentDao,
            serviceIntervalDao,
            componentSwapDao,
            bikeDao,
            imageRepository,
        )
    }

    @Test
    fun deleteComponent_deletesImageBeforeDeletingFromDao() = runTest {
        val component = ComponentEntity(
            id = 5L,
            bikeId = 1L,
            type = "chain",
            name = "Chain 1",
            lifespanKm = 3000.0,
            installedAt = 1000L,
        )
        coEvery { componentDao.deleteById(5L) } coAnswers { }

        repository.deleteComponent(component)

        coVerify { imageRepository.deleteComponentImage(5L) }
        coVerify { componentDao.deleteById(5L) }
    }
}
