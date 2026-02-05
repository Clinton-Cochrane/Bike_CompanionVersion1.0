package com.you.bikecompanion.data.component

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComponentRepository @Inject constructor(
    private val componentDao: ComponentDao,
) {
    fun getComponentsByBikeId(bikeId: Long): Flow<List<ComponentEntity>> =
        componentDao.getComponentsByBikeId(bikeId)

    suspend fun getComponentById(id: Long): ComponentEntity? = componentDao.getComponentById(id)

    suspend fun insertComponent(component: ComponentEntity): Long = componentDao.insert(component)

    suspend fun updateComponent(component: ComponentEntity) = componentDao.update(component)

    suspend fun deleteComponent(component: ComponentEntity) = componentDao.deleteById(component.id)

    suspend fun getAllComponents(): List<ComponentEntity> = componentDao.getAllComponents()
}
