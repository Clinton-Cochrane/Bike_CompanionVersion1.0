package com.you.bikecompanion.data.component

import kotlinx.coroutines.flow.Flow
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

    suspend fun insertInterval(interval: ServiceIntervalEntity): Long =
        serviceIntervalDao.insert(interval)

    suspend fun updateInterval(interval: ServiceIntervalEntity) =
        serviceIntervalDao.update(interval)

    suspend fun deleteInterval(id: Long) = serviceIntervalDao.deleteById(id)
}
