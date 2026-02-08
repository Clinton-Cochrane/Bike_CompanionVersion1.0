package com.you.bikecompanion.data.ride

import com.you.bikecompanion.data.bike.BikeDao
import com.you.bikecompanion.data.bike.BikeEntity
import com.you.bikecompanion.data.component.ComponentDao
import com.you.bikecompanion.data.component.ComponentEntity
import com.you.bikecompanion.data.component.ServiceIntervalDao
import com.you.bikecompanion.notifications.ComponentAlertNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [RideRepository] duration and totals roll-up.
 * Verifies that bike and component totalTimeSeconds are incremented on trip completion.
 */
class RideRepositoryTotalsTest {

    private lateinit var rideDao: RideDao
    private lateinit var bikeDao: BikeDao
    private lateinit var componentDao: ComponentDao
    private lateinit var serviceIntervalDao: ServiceIntervalDao
    private lateinit var componentAlertNotifier: ComponentAlertNotifier
    private lateinit var repository: RideRepository

    @Before
    fun setUp() {
        rideDao = mockk()
        bikeDao = mockk()
        componentDao = mockk()
        serviceIntervalDao = mockk(relaxed = true)
        coEvery { serviceIntervalDao.getIntervalsByComponentIdOnce(any()) } returns emptyList()
        componentAlertNotifier = mockk(relaxed = true)
        repository = RideRepository(rideDao, bikeDao, componentDao, serviceIntervalDao, componentAlertNotifier)
    }

    @Test
    fun saveRideAndUpdateBikeAndComponents_incrementsBikeTotalTimeSeconds() = runBlocking {
        val bikeId = 1L
        val ride = RideEntity(
            bikeId = bikeId,
            distanceKm = 5.0,
            durationMs = 3600_000, // 1 hour
            startedAt = 1000L,
            endedAt = 3_601_000L,
        )
        val bike = BikeEntity(
            id = bikeId,
            name = "Test Bike",
            totalDistanceKm = 10.0,
            totalTimeSeconds = 1200L, // 20 min
            createdAt = 0L,
        )
        val component = ComponentEntity(
            id = 1L,
            bikeId = bikeId,
            type = "chain",
            name = "Chain",
            lifespanKm = 3000.0,
            distanceUsedKm = 10.0,
            totalTimeSeconds = 1200L,
            installedAt = 0L,
        )

        coEvery { rideDao.insert(ride) } returns 1L
        coEvery { bikeDao.getBikeById(bikeId) } returns bike
        coEvery { bikeDao.update(any()) } coAnswers { }
        coEvery { componentDao.getComponentsByBikeIdOnce(bikeId) } returns listOf(component)
        coEvery { componentDao.update(any()) } coAnswers { }

        repository.saveRideAndUpdateBikeAndComponents(ride)

        coVerify {
            bikeDao.update(match { updatedBike ->
                updatedBike.totalTimeSeconds == bike.totalTimeSeconds + 3600L &&
                    updatedBike.totalDistanceKm == bike.totalDistanceKm + 5.0
            })
        }
    }

    @Test
    fun saveRideAndUpdateBikeAndComponents_incrementsComponentTotalTimeSeconds() = runBlocking {
        val bikeId = 1L
        val ride = RideEntity(
            bikeId = bikeId,
            distanceKm = 2.5,
            durationMs = 900_000, // 15 min
            startedAt = 1000L,
            endedAt = 901_000L,
        )
        val bike = BikeEntity(
            id = bikeId,
            name = "Test Bike",
            totalDistanceKm = 0.0,
            totalTimeSeconds = 0L,
            createdAt = 0L,
        )
        val component = ComponentEntity(
            id = 1L,
            bikeId = bikeId,
            type = "tires",
            name = "Tires",
            lifespanKm = 5000.0,
            distanceUsedKm = 0.0,
            totalTimeSeconds = 0L,
            installedAt = 0L,
        )

        coEvery { rideDao.insert(ride) } returns 1L
        coEvery { bikeDao.getBikeById(bikeId) } returns bike
        coEvery { bikeDao.update(any()) } coAnswers { }
        coEvery { componentDao.getComponentsByBikeIdOnce(bikeId) } returns listOf(component)
        coEvery { componentDao.update(any()) } coAnswers { invocation ->
            val updated = invocation.invocation.args[0] as ComponentEntity
            assertEquals(900L, updated.totalTimeSeconds)
            assertEquals(2.5, updated.distanceUsedKm, 1e-9)
        }

        repository.saveRideAndUpdateBikeAndComponents(ride)

        coVerify(exactly = 1) { componentDao.update(any()) }
    }

    @Test
    fun saveRideAndUpdateBikeAndComponents_nullBikeId_doesNotUpdateBikeOrComponents() = runBlocking {
        val ride = RideEntity(
            bikeId = null,
            distanceKm = 5.0,
            durationMs = 3600_000,
            startedAt = 1000L,
            endedAt = 3_601_000L,
        )

        coEvery { rideDao.insert(ride) } returns 1L

        repository.saveRideAndUpdateBikeAndComponents(ride)

        coVerify(exactly = 1) { rideDao.insert(ride) }
        coVerify(exactly = 0) { bikeDao.getBikeById(any()) }
        coVerify(exactly = 0) { bikeDao.update(any()) }
        coVerify(exactly = 0) { componentDao.getComponentsByBikeIdOnce(any()) }
        coVerify(exactly = 0) { componentDao.update(any()) }
    }
}
