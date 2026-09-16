package com.clintoncochrane.bikecompanion.data.component

import com.clintoncochrane.bikecompanion.data.bike.BikeDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ComponentRepositoryPriorUsageTest {

    private lateinit var componentDao: ComponentDao
    private lateinit var serviceIntervalDao: ServiceIntervalDao
    private lateinit var repository: ComponentRepository

    @Before
    fun setUp() {
        componentDao = mockk()
        serviceIntervalDao = mockk()
        repository = ComponentRepository(
            componentDao = componentDao,
            serviceIntervalDao = serviceIntervalDao,
            componentSwapDao = mockk(),
            bikeDao = mockk<BikeDao>(),
            lifecycleTransaction = mockk(),
            serviceHistoryDao = mockk(),
        )
    }

    @Test
    fun insertComponent_knownBaseline_initializesMaintenanceFromLifetimeDistance() = runTest {
        val component = ComponentEntity(
            bikeId = 1L,
            type = "custom",
            name = "Used component",
            lifespanKm = 1_000.0,
            baselineKm = 120.0,
            distanceUsedKm = 30.0,
            priorUsageCertainty = PriorUsageCertainty.KNOWN,
            installedAt = 0L,
        )
        coEvery { componentDao.insert(component) } returns 7L
        coEvery { serviceIntervalDao.insert(any()) } returns 1L

        repository.insertComponent(component)

        coVerify {
            serviceIntervalDao.insert(match { interval ->
                interval.componentId == 7L && interval.trackedKm == 150.0
            })
        }
    }

    @Test
    fun insertComponent_unknownBaseline_initializesMaintenanceFromTrackedDistanceOnly() = runTest {
        val component = ComponentEntity(
            bikeId = 1L,
            type = "custom",
            name = "Unknown component",
            lifespanKm = 1_000.0,
            baselineKm = 0.0,
            distanceUsedKm = 30.0,
            priorUsageCertainty = PriorUsageCertainty.UNKNOWN,
            installedAt = 0L,
        )
        coEvery { componentDao.insert(component) } returns 8L
        coEvery { serviceIntervalDao.insert(any()) } returns 1L

        repository.insertComponent(component)

        coVerify {
            serviceIntervalDao.insert(match { interval ->
                interval.componentId == 8L && interval.trackedKm == 30.0
            })
        }
    }
}
