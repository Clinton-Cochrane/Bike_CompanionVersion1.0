package com.clintoncochrane.bikecompanion.data.ride

import com.clintoncochrane.bikecompanion.data.bike.BikeDao
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentDao
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalDao
import com.clintoncochrane.bikecompanion.notifications.ComponentAlertNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    private lateinit var ridePersistenceTransaction: RidePersistenceTransaction
    private lateinit var repository: RideRepository

    @Before
    fun setUp() {
        rideDao = mockk()
        bikeDao = mockk()
        componentDao = mockk()
        serviceIntervalDao = mockk(relaxed = true)
        coEvery { serviceIntervalDao.getIntervalsByComponentIdOnce(any()) } returns emptyList()
        componentAlertNotifier = mockk(relaxed = true)
        ridePersistenceTransaction = mockk()
        coEvery { ridePersistenceTransaction.run(any()) } coAnswers {
            firstArg<suspend () -> Long?>().invoke()
        }
        repository = RideRepository(
            rideDao,
            bikeDao,
            componentDao,
            serviceIntervalDao,
            componentAlertNotifier,
            ridePersistenceTransaction,
        )
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
        coVerify(exactly = 1) { componentAlertNotifier.notifyIfNeeded(bikeId) }
    }

    @Test
    fun saveRideAndUpdateBikeAndComponents_addsRideToBaselineWithoutInflatingAverageSpeed() = runBlocking {
        val bikeId = 1L
        val ride = RideEntity(
            bikeId = bikeId,
            distanceKm = 20.0,
            durationMs = 3_600_000,
            startedAt = 1000L,
            endedAt = 3_601_000L,
        )
        val bike = BikeEntity(
            id = bikeId,
            name = "Used bike",
            baselineDistanceKm = 1_000.0,
            totalDistanceKm = 1_050.0,
            totalTimeSeconds = 3_600L,
            createdAt = 0L,
        )
        coEvery { rideDao.insert(ride) } returns 1L
        coEvery { bikeDao.getBikeById(bikeId) } returns bike
        coEvery { bikeDao.update(any()) } coAnswers { }
        coEvery { componentDao.getComponentsByBikeIdOnce(bikeId) } returns emptyList()

        repository.saveRideAndUpdateBikeAndComponents(ride)

        coVerify {
            bikeDao.update(match {
                it.baselineDistanceKm == 1_000.0 &&
                    it.totalDistanceKm == 1_070.0 &&
                    it.avgSpeedKmh == 35.0
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
        coVerify(exactly = 0) { componentAlertNotifier.notifyIfNeeded(any()) }
    }

    @Test
    fun saveRideAndUpdateBikeAndComponents_transactionFailure_doesNotUpdateOrNotify() = runBlocking {
        val ride = RideEntity(
            bikeId = 1L,
            distanceKm = 5.0,
            durationMs = 3600_000,
            startedAt = 1000L,
            endedAt = 3_601_000L,
        )
        coEvery { ridePersistenceTransaction.run(any()) } throws RuntimeException("forced rollback")

        val result = runCatching { repository.saveRideAndUpdateBikeAndComponents(ride) }

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { rideDao.insert(any()) }
        coVerify(exactly = 0) { bikeDao.update(any()) }
        coVerify(exactly = 0) { componentDao.update(any()) }
        coVerify(exactly = 0) { componentAlertNotifier.notifyIfNeeded(any()) }
    }

    @Test
    fun saveRideAndUpdateBikeAndComponents_allPriorUsageStates_incrementOnlyTrackedDistance() = runBlocking {
        PriorUsageCertainty.entries.forEachIndexed { index, certainty ->
            val bikeId = index.toLong() + 1L
            val ride = RideEntity(
                bikeId = bikeId,
                distanceKm = 5.0,
                durationMs = 60_000,
                startedAt = 1_000L,
                endedAt = 61_000L,
            )
            val bike = BikeEntity(id = bikeId, name = "Bike", createdAt = 0L)
            val baselineKm = if (certainty == PriorUsageCertainty.UNKNOWN) 0.0 else 100.0
            val component = ComponentEntity(
                id = bikeId,
                bikeId = bikeId,
                type = "chain",
                name = "Chain",
                lifespanKm = 3_000.0,
                baselineKm = baselineKm,
                distanceUsedKm = 10.0,
                priorUsageCertainty = certainty,
                installedAt = 0L,
            )

            coEvery { rideDao.insert(ride) } returns bikeId
            coEvery { bikeDao.getBikeById(bikeId) } returns bike
            coEvery { bikeDao.update(any()) } coAnswers { }
            coEvery { componentDao.getComponentsByBikeIdOnce(bikeId) } returns listOf(component)
            coEvery { componentDao.update(any()) } coAnswers { }

            repository.saveRideAndUpdateBikeAndComponents(ride)

            coVerify {
                componentDao.update(match { updated ->
                    updated.id == component.id &&
                        updated.distanceUsedKm == 15.0 &&
                        updated.baselineKm == baselineKm &&
                        updated.priorUsageCertainty == certainty
                })
            }
        }
    }
}
