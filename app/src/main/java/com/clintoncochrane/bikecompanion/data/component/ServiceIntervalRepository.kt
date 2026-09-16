package com.clintoncochrane.bikecompanion.data.component

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceIntervalRepository @Inject constructor(
    private val serviceIntervalDao: ServiceIntervalDao,
) {
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

    suspend fun deleteInterval(id: Long) = serviceIntervalDao.deleteById(id)
}
