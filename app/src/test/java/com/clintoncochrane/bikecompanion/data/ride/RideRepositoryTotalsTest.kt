package com.clintoncochrane.bikecompanion.data.ride

import com.clintoncochrane.bikecompanion.data.bike.BikeDao
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentDao
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentSwapDao
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
    private lateinit var componentSwapDao: ComponentSwapDao
    private lateinit var componentAlertNotifier: ComponentAlertNotifier
    private lateinit var ridePersistenceTransaction: RidePersistenceTransaction
    private lateinit var repository: RideRepository

    @Before
    fun setUp() {
        rideDao = mockk()
        bikeDao = mockk()
        componentDao = mockk()
        serviceIntervalDao = mockk(relaxed = true)
        componentSwapDao = mockk(relaxed = true)
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
            componentSwapDao,
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
    fun saveManualRide_validDistance_createsNormalRideAndUpdatesAccountingOnce() = runBlocking {
        val bikeId = 1L
        val bike = BikeEntity(
            id = bikeId,
            name = "Test Bike",
            totalDistanceKm = 10.0,
            totalTimeSeconds = 600L,
            createdAt = 0L,
        )
        val component = ComponentEntity(
            id = 2L,
            bikeId = bikeId,
            type = "chain",
            name = "Chain",
            lifespanKm = 3_000.0,
            distanceUsedKm = 10.0,
            totalTimeSeconds = 600L,
            installedAt = 0L,
        )

        coEvery { rideDao.insert(any()) } returns 3L
        coEvery { bikeDao.getBikeById(bikeId) } returns bike
        coEvery { bikeDao.update(any()) } returns Unit
        coEvery { componentDao.getComponentsByBikeIdOnce(bikeId) } returns listOf(component)
        coEvery { componentDao.update(any()) } returns Unit

        repository.saveManualRide(bikeId = bikeId, distanceKm = 5.0, occurredAt = 1_000L)

        coVerify(exactly = 1) {
            rideDao.insert(
                match {
                    it.bikeId == bikeId &&
                        it.distanceKm == 5.0 &&
                        it.durationMs == 0L &&
                        it.avgSpeedKmh == 0.0 &&
                        it.maxSpeedKmh == 0.0 &&
                        it.elevGainM == 0.0 &&
                        it.elevLossM == 0.0 &&
                        it.startedAt == 1_000L &&
                        it.endedAt == 1_000L &&
                        it.source == RideSource.MANUAL
                },
            )
        }
        coVerify(exactly = 1) { bikeDao.update(match { it.totalDistanceKm == 15.0 }) }
        coVerify(exactly = 1) { componentDao.update(match { it.distanceUsedKm == 15.0 }) }
        coVerify(exactly = 1) { componentAlertNotifier.notifyIfNeeded(bikeId) }
    }

    @Test
    fun saveManualRide_invalidDistance_doesNotStartPersistence() = runBlocking {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { distanceKm ->
            assertTrue(runCatching { repository.saveManualRide(1L, distanceKm) }.isFailure)
        }

        coVerify(exactly = 0) { ridePersistenceTransaction.run(any()) }
        coVerify(exactly = 0) { rideDao.insert(any()) }
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
    fun saveRideAndUpdateBikeAndComponents_nullBikeId_rejectsBeforePersistence() = runBlocking {
        val ride = RideEntity(
            bikeId = null,
            distanceKm = 5.0,
            durationMs = 3600_000,
            startedAt = 1000L,
            endedAt = 3_601_000L,
        )

        assertTrue(runCatching { repository.saveRideAndUpdateBikeAndComponents(ride) }.isFailure)

        coVerify(exactly = 0) { rideDao.insert(any()) }
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
    fun saveRideAndUpdateBikeAndComponents_zeroDistanceGpsRide_isDiscardedBeforeTransaction() = runBlocking {
        val ride = RideEntity(
            bikeId = 1L,
            distanceKm = 0.0,
            durationMs = 60_000L,
            startedAt = 1_000L,
            endedAt = 61_000L,
            source = RideSource.APP,
        )

        val result = repository.saveRideAndUpdateBikeAndComponents(ride)

        assertEquals(RideSaveResult.DISCARDED_EMPTY, result)
        coVerify(exactly = 0) { ridePersistenceTransaction.run(any()) }
        coVerify(exactly = 0) { rideDao.insert(any()) }
        coVerify(exactly = 0) { bikeDao.update(any()) }
        coVerify(exactly = 0) { componentDao.update(any()) }
    }

    @Test
    fun saveRideAndUpdateBikeAndComponents_zeroDistanceManualRide_isDiscardedBeforeTransaction() = runBlocking {
        val ride = RideEntity(
            bikeId = 1L,
            distanceKm = 0.0,
            durationMs = 60_000L,
            startedAt = 1_000L,
            endedAt = 61_000L,
            source = RideSource.MANUAL,
        )

        val result = repository.saveRideAndUpdateBikeAndComponents(ride)

        assertEquals(RideSaveResult.DISCARDED_EMPTY, result)
        coVerify(exactly = 0) { ridePersistenceTransaction.run(any()) }
    }

    @Test
    fun saveRideAndUpdateBikeAndComponents_negativeDistance_isRejectedBeforeTransaction() = runBlocking {
        val ride = RideEntity(
            bikeId = 1L,
            distanceKm = -0.1,
            durationMs = 60_000L,
            startedAt = 1_000L,
            endedAt = 61_000L,
        )

        val result = repository.saveRideAndUpdateBikeAndComponents(ride)

        assertEquals(RideSaveResult.REJECTED_INVALID, result)
        coVerify(exactly = 0) { ridePersistenceTransaction.run(any()) }
    }

    @Test
    fun saveRideAndUpdateBikeAndComponents_negativeDuration_isRejectedBeforeTransaction() = runBlocking {
        val ride = RideEntity(
            bikeId = 1L,
            distanceKm = 1.0,
            durationMs = -1L,
            startedAt = 1_000L,
            endedAt = 61_000L,
        )

        val result = repository.saveRideAndUpdateBikeAndComponents(ride)

        assertEquals(RideSaveResult.REJECTED_INVALID, result)
        coVerify(exactly = 0) { ridePersistenceTransaction.run(any()) }
    }

    @Test
    fun saveRideAndUpdateBikeAndComponents_nonFiniteValues_areRejectedBeforeTransaction() = runBlocking {
        listOf(
            RideEntity(1L, 1L, Double.NaN, 60_000L, startedAt = 1_000L, endedAt = 61_000L),
            RideEntity(2L, 1L, 1.0, 60_000L, avgSpeedKmh = Double.POSITIVE_INFINITY, startedAt = 1_000L, endedAt = 61_000L),
            RideEntity(3L, 1L, 1.0, 60_000L, maxSpeedKmh = Double.NEGATIVE_INFINITY, startedAt = 1_000L, endedAt = 61_000L),
        ).forEach { ride ->
            val result = repository.saveRideAndUpdateBikeAndComponents(ride)
            assertEquals(RideSaveResult.REJECTED_INVALID, result)
        }

        coVerify(exactly = 0) { ridePersistenceTransaction.run(any()) }
    }

    @Test
    fun saveRideAndUpdateBikeAndComponents_tinyPositiveDistance_isSaved() = runBlocking {
        val ride = RideEntity(
            bikeId = 1L,
            distanceKm = Double.MIN_VALUE,
            durationMs = 1L,
            startedAt = 1_000L,
            endedAt = 1_001L,
        )
        val bike = BikeEntity(id = 1L, name = "Test Bike", createdAt = 0L)
        coEvery { rideDao.insert(ride) } returns 1L
        coEvery { bikeDao.getBikeById(1L) } returns bike
        coEvery { bikeDao.update(any()) } coAnswers { }
        coEvery { componentDao.getComponentsByBikeIdOnce(1L) } returns emptyList()

        val result = repository.saveRideAndUpdateBikeAndComponents(ride)

        assertEquals(RideSaveResult.SAVED, result)
        coVerify(exactly = 1) { rideDao.insert(ride) }
        coVerify(exactly = 1) { bikeDao.update(any()) }
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
