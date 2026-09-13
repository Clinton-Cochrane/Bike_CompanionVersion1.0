package com.clintoncochrane.bikecompanion.data.component

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ServiceIntervalRepositoryTest {

    private lateinit var serviceIntervalDao: ServiceIntervalDao
    private lateinit var repository: ServiceIntervalRepository

    @Before
    fun setUp() {
        serviceIntervalDao = mockk()
        repository = ServiceIntervalRepository(serviceIntervalDao)
    }

    @Test
    fun completeServiceInterval_distanceAndTimeInspection_resetsOnlySelectedProgress() = runTest {
        val selectedInterval = ServiceIntervalEntity(
            id = 11L,
            componentId = 4L,
            name = "Inspect bearings",
            intervalKm = 1_000.0,
            trackedKm = 725.0,
            type = SERVICE_INTERVAL_TYPE_INSPECTION,
            intervalTimeSeconds = 2_592_000L,
            trackedTimeSeconds = 1_800_000L,
        )
        val unrelatedInterval = ServiceIntervalEntity(
            id = 12L,
            componentId = 4L,
            name = "Grease bearings",
            intervalKm = 5_000.0,
            trackedKm = 3_200.0,
            type = SERVICE_INTERVAL_TYPE_GREASE,
            intervalTimeSeconds = 31_536_000L,
            trackedTimeSeconds = 20_000_000L,
        )
        coEvery { serviceIntervalDao.getIntervalById(11L) } returns selectedInterval
        coEvery { serviceIntervalDao.update(any()) } returns Unit

        val completed = repository.completeServiceInterval(11L)

        assertTrue(completed)
        coVerify(exactly = 1) {
            serviceIntervalDao.update(
                selectedInterval.copy(
                    trackedKm = 0.0,
                    trackedTimeSeconds = 0L,
                ),
            )
        }
        coVerify(exactly = 0) { serviceIntervalDao.update(unrelatedInterval) }
    }

    @Test
    fun completeServiceInterval_timeOnlyGrease_resetsOnlyConfiguredTimeProgress() = runTest {
        val interval = ServiceIntervalEntity(
            id = 21L,
            componentId = 8L,
            name = "Top up sealant",
            intervalKm = 0.0,
            trackedKm = 42.0,
            type = SERVICE_INTERVAL_TYPE_GREASE,
            intervalTimeSeconds = 7_776_000L,
            trackedTimeSeconds = 7_000_000L,
        )
        coEvery { serviceIntervalDao.getIntervalById(21L) } returns interval
        coEvery { serviceIntervalDao.update(any()) } returns Unit

        val completed = repository.completeServiceInterval(21L)

        assertTrue(completed)
        coVerify(exactly = 1) {
            serviceIntervalDao.update(interval.copy(trackedTimeSeconds = 0L))
        }
    }

    @Test
    fun completeServiceInterval_replacementInterval_isNotChanged() = runTest {
        val interval = ServiceIntervalEntity(
            id = 31L,
            componentId = 9L,
            name = "Replace",
            intervalKm = 3_500.0,
            trackedKm = 3_400.0,
            type = SERVICE_INTERVAL_TYPE_REPLACE,
        )
        coEvery { serviceIntervalDao.getIntervalById(31L) } returns interval

        val completed = repository.completeServiceInterval(31L)

        assertFalse(completed)
        coVerify(exactly = 0) { serviceIntervalDao.update(any()) }
    }

    @Test
    fun completeServiceInterval_repeatedCompletion_isDeterministic() = runTest {
        val completedInterval = ServiceIntervalEntity(
            id = 41L,
            componentId = 10L,
            name = "Inspect",
            intervalKm = 500.0,
            trackedKm = 0.0,
            type = SERVICE_INTERVAL_TYPE_INSPECTION,
            intervalTimeSeconds = 2_592_000L,
            trackedTimeSeconds = 0L,
        )
        coEvery { serviceIntervalDao.getIntervalById(41L) } returns completedInterval
        coEvery { serviceIntervalDao.update(any()) } returns Unit

        assertTrue(repository.completeServiceInterval(41L))
        assertTrue(repository.completeServiceInterval(41L))

        coVerify(exactly = 2) { serviceIntervalDao.update(completedInterval) }
    }
}
