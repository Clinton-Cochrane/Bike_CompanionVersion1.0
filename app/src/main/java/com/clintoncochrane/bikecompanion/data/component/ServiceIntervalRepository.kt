package com.clintoncochrane.bikecompanion.data.component

import com.clintoncochrane.bikecompanion.data.bike.BikeDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceIntervalRepository @Inject constructor(
    private val serviceIntervalDao: ServiceIntervalDao,
    private val serviceHistoryDao: ServiceHistoryDao,
    private val componentDao: ComponentDao,
    private val bikeDao: BikeDao,
    private val lifecycleTransaction: ComponentLifecycleTransaction,
) {
    fun getAllIntervals(): Flow<List<ServiceIntervalEntity>> = serviceIntervalDao.getAllIntervals()

    suspend fun getAllIntervalsOnce(): List<ServiceIntervalEntity> = serviceIntervalDao.getAllIntervalsOnce()

    fun getIntervalsByComponentId(componentId: Long): Flow<List<ServiceIntervalEntity>> =
        serviceIntervalDao.getIntervalsByComponentId(componentId)

    suspend fun getIntervalsByComponentIdOnce(componentId: Long): List<ServiceIntervalEntity> =
        serviceIntervalDao.getIntervalsByComponentIdOnce(componentId)

    suspend fun getIntervalsByComponentIdsOnce(componentIds: List<Long>): List<ServiceIntervalEntity> =
        if (componentIds.isEmpty()) emptyList()
        else serviceIntervalDao.getIntervalsByComponentIdsOnce(componentIds)

    fun getIntervalsByComponentIds(componentIds: List<Long>): Flow<List<ServiceIntervalEntity>> =
        if (componentIds.isEmpty()) flowOf(emptyList())
        else serviceIntervalDao.observeIntervalsByComponentIds(componentIds)

    suspend fun insertInterval(interval: ServiceIntervalEntity): Long =
        serviceIntervalDao.insert(interval)

    suspend fun updateInterval(interval: ServiceIntervalEntity) =
        serviceIntervalDao.update(interval)

    /**
     * Completes one inspection or grease/service interval without changing its policy.
     * Only progress tracked by a configured distance or time basis is reset.
     */
    suspend fun completeServiceInterval(
        intervalId: Long,
        completedAt: Long = System.currentTimeMillis(),
    ): Boolean {
        val interval = serviceIntervalDao.getIntervalById(intervalId) ?: return false
        if (interval.type != SERVICE_INTERVAL_TYPE_INSPECTION &&
            interval.type != SERVICE_INTERVAL_TYPE_GREASE
        ) {
            return false
        }

        serviceIntervalDao.update(
            interval.copy(
                trackedKm = if (interval.intervalKm > 0.0) 0.0 else interval.trackedKm,
                trackedTimeSeconds = if (interval.intervalTimeSeconds != null) {
                    0L
                } else {
                    interval.trackedTimeSeconds
                },
                lastCompletedAt = completedAt,
            ),
        )
        return true
    }

    /**
     * Atomically records and completes one non-replacement service requirement.
     * A repeated call for the same session and interval is an idempotent success.
     */
    suspend fun completeServiceRequirement(
        intervalId: Long,
        sessionId: String,
        completedAt: Long = System.currentTimeMillis(),
    ): Boolean {
        return lifecycleTransaction.run {
            if (serviceHistoryDao.getBySessionAndInterval(sessionId, intervalId) != null) {
                return@run true
            }
            val interval = serviceIntervalDao.getIntervalById(intervalId) ?: return@run false
            if (interval.type == SERVICE_INTERVAL_TYPE_REPLACE || interval.type == SERVICE_INTERVAL_TYPE_ON_FAILURE) {
                return@run false
            }
            val component = componentDao.getComponentById(interval.componentId) ?: return@run false
            val bikeId = component.bikeId ?: return@run false
            val bike = bikeDao.getBikeById(bikeId) ?: return@run false

            serviceIntervalDao.update(
                interval.copy(
                    trackedKm = if (interval.intervalKm > 0.0) 0.0 else interval.trackedKm,
                    trackedTimeSeconds = if (interval.intervalTimeSeconds != null) 0L else interval.trackedTimeSeconds,
                    lastCompletedAt = completedAt,
                ),
            )
            check(
                serviceHistoryDao.insert(
                    ServiceHistoryEntity(
                        sessionId = sessionId,
                        serviceIntervalId = interval.id,
                        serviceName = interval.name,
                        serviceType = interval.type,
                        componentId = component.id,
                        bikeId = bikeId,
                        completedAt = completedAt,
                        bikeOdometerKm = bike.totalDistanceKm,
                    ),
                ) != -1L,
            ) { "Service history was not written" }
            true
        }
    }

    suspend fun deleteInterval(id: Long) = serviceIntervalDao.deleteById(id)
}
