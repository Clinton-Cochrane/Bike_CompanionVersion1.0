package com.clintoncochrane.bikecompanion.data.bike

import com.clintoncochrane.bikecompanion.data.component.ComponentDao
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentLifecycleTransaction
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalDao
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BikeMileageCorrectionRepositoryTest {

    private lateinit var bikeDao: BikeDao
    private lateinit var componentDao: ComponentDao
    private lateinit var serviceIntervalDao: ServiceIntervalDao
    private lateinit var transaction: ComponentLifecycleTransaction
    private lateinit var repository: BikeMileageCorrectionRepository

    @Before
    fun setUp() {
        bikeDao = mockk(relaxed = true)
        componentDao = mockk(relaxed = true)
        serviceIntervalDao = mockk(relaxed = true)
        transaction = mockk()
        coEvery { transaction.run<MileageCorrectionResult>(any()) } coAnswers {
            firstArg<suspend () -> MileageCorrectionResult>().invoke()
        }
        repository = BikeMileageCorrectionRepository(
            bikeDao = bikeDao,
            componentDao = componentDao,
            serviceIntervalDao = serviceIntervalDao,
            transaction = transaction,
        )
    }

    @Test
    fun correctMileage_noComponentsSelected_updatesBikeBaselineOnly() = runTest {
        val bike = bike(totalDistanceKm = 0.0)
        coEvery { bikeDao.getBikeById(BIKE_ID) } returns bike

        val result = repository.correctMileage(BIKE_ID, 1_000.0, emptySet())

        assertEquals(MileageCorrectionResult.APPLIED, result)
        coVerify(exactly = 1) {
            bikeDao.update(match { it.baselineDistanceKm == 1_000.0 && it.totalDistanceKm == 1_000.0 })
        }
        coVerify(exactly = 0) { componentDao.getComponentsByIdsOnce(any()) }
        coVerify(exactly = 0) { componentDao.update(any()) }
        coVerify(exactly = 0) { serviceIntervalDao.update(any()) }
    }

    @Test
    fun correctMileage_selectedComponent_appliesSameDeltaToComponentAndServiceProgress() = runTest {
        val component = component(id = 10L, baselineKm = 100.0, distanceUsedKm = 200.0)
        val interval = ServiceIntervalEntity(
            id = 20L,
            componentId = component.id,
            name = "Inspect",
            intervalKm = 1_000.0,
            trackedKm = 300.0,
        )
        coEvery { bikeDao.getBikeById(BIKE_ID) } returns bike(totalDistanceKm = 500.0)
        coEvery { componentDao.getComponentsByIdsOnce(listOf(component.id)) } returns listOf(component)
        coEvery { serviceIntervalDao.getIntervalsByComponentIdOnce(component.id) } returns listOf(interval)

        val result = repository.correctMileage(BIKE_ID, 1_000.0, setOf(component.id))

        assertEquals(MileageCorrectionResult.APPLIED, result)
        coVerify { componentDao.update(match { it.id == component.id && it.lifetimeDistanceKm == 800.0 }) }
        coVerify { serviceIntervalDao.update(match { it.id == interval.id && it.trackedKm == 800.0 }) }
    }

    @Test
    fun correctMileage_onlySelectedInstalledComponentChanges() = runTest {
        val selected = component(id = 10L, baselineKm = 50.0, distanceUsedKm = 50.0)
        val unselected = component(id = 11L, baselineKm = 25.0, distanceUsedKm = 75.0)
        coEvery { bikeDao.getBikeById(BIKE_ID) } returns bike(totalDistanceKm = 500.0)
        coEvery { componentDao.getComponentsByIdsOnce(listOf(selected.id)) } returns listOf(selected)
        coEvery { serviceIntervalDao.getIntervalsByComponentIdOnce(selected.id) } returns emptyList()

        val result = repository.correctMileage(BIKE_ID, 600.0, setOf(selected.id))

        assertEquals(MileageCorrectionResult.APPLIED, result)
        coVerify(exactly = 1) { componentDao.update(match { it.id == selected.id && it.lifetimeDistanceKm == 200.0 }) }
        coVerify(exactly = 0) { componentDao.update(match { it.id == unselected.id }) }
    }

    @Test
    fun correctMileage_validDownwardCorrection_reducesSelectedComponentAndServiceProgress() = runTest {
        val component = component(id = 10L, baselineKm = 200.0, distanceUsedKm = 100.0)
        val interval = ServiceIntervalEntity(
            id = 20L,
            componentId = component.id,
            name = "Replace",
            intervalKm = 1_000.0,
            trackedKm = 40.0,
        )
        coEvery { bikeDao.getBikeById(BIKE_ID) } returns bike(
            baselineDistanceKm = 900.0,
            totalDistanceKm = 1_000.0,
        )
        coEvery { componentDao.getComponentsByIdsOnce(listOf(component.id)) } returns listOf(component)
        coEvery { serviceIntervalDao.getIntervalsByComponentIdOnce(component.id) } returns listOf(interval)

        val result = repository.correctMileage(BIKE_ID, 900.0, setOf(component.id))

        assertEquals(MileageCorrectionResult.APPLIED, result)
        coVerify { componentDao.update(match { it.lifetimeDistanceKm == 200.0 }) }
        coVerify { serviceIntervalDao.update(match { it.trackedKm == 0.0 }) }
    }

    @Test
    fun correctMileage_selectedComponentWouldBecomeNegative_blocksEntireCorrection() = runTest {
        val component = component(id = 10L, baselineKm = 20.0, distanceUsedKm = 30.0)
        coEvery { bikeDao.getBikeById(BIKE_ID) } returns bike(
            baselineDistanceKm = 1_000.0,
            totalDistanceKm = 1_000.0,
        )
        coEvery { componentDao.getComponentsByIdsOnce(listOf(component.id)) } returns listOf(component)

        val result = repository.correctMileage(BIKE_ID, 900.0, setOf(component.id))

        assertEquals(MileageCorrectionResult.COMPONENT_WOULD_BECOME_NEGATIVE, result)
        coVerify(exactly = 0) { bikeDao.update(any()) }
        coVerify(exactly = 0) { componentDao.update(any()) }
        coVerify(exactly = 0) { serviceIntervalDao.update(any()) }
        confirmVerified(serviceIntervalDao)
    }

    @Test
    fun correctMileage_belowRecordedRideDistance_blocksEntireCorrection() = runTest {
        coEvery { bikeDao.getBikeById(BIKE_ID) } returns bike(
            baselineDistanceKm = 100.0,
            totalDistanceKm = 500.0,
        )

        val result = repository.correctMileage(BIKE_ID, 350.0, emptySet())

        assertEquals(MileageCorrectionResult.BELOW_RECORDED_RIDE_DISTANCE, result)
        coVerify(exactly = 0) { bikeDao.update(any()) }
        coVerify(exactly = 0) { componentDao.update(any()) }
    }

    private fun bike(
        baselineDistanceKm: Double = 0.0,
        totalDistanceKm: Double,
    ) = BikeEntity(
        id = BIKE_ID,
        name = "Test bike",
        baselineDistanceKm = baselineDistanceKm,
        totalDistanceKm = totalDistanceKm,
        createdAt = 1L,
    )

    private fun component(
        id: Long,
        baselineKm: Double,
        distanceUsedKm: Double,
    ) = ComponentEntity(
        id = id,
        bikeId = BIKE_ID,
        type = "chain",
        name = "Chain",
        lifespanKm = 2_000.0,
        baselineKm = baselineKm,
        distanceUsedKm = distanceUsedKm,
        installedAt = 1L,
    )

    private companion object {
        const val BIKE_ID = 42L
    }
}
