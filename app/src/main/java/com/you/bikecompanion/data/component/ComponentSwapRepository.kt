package com.you.bikecompanion.data.component

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComponentSwapRepository @Inject constructor(
    private val componentSwapDao: ComponentSwapDao,
) {
    fun getSwapsByComponentId(componentId: Long): Flow<List<ComponentSwapEntity>> =
        componentSwapDao.getSwapsByComponentId(componentId)

    suspend fun getSwapsByComponentIdOnce(componentId: Long): List<ComponentSwapEntity> =
        componentSwapDao.getSwapsByComponentIdOnce(componentId)

    suspend fun insertSwap(swap: ComponentSwapEntity): Long = componentSwapDao.insert(swap)

    suspend fun updateSwap(swap: ComponentSwapEntity) = componentSwapDao.update(swap)

    suspend fun getCurrentSwap(componentId: Long): ComponentSwapEntity? =
        componentSwapDao.getCurrentSwap(componentId)
}
