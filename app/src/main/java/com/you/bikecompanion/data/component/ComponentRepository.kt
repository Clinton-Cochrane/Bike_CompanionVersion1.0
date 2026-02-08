package com.you.bikecompanion.data.component

import com.you.bikecompanion.data.bike.BikeDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComponentRepository @Inject constructor(
    private val componentDao: ComponentDao,
    private val serviceIntervalDao: ServiceIntervalDao,
    private val componentSwapDao: ComponentSwapDao,
    private val bikeDao: BikeDao,
) {
    fun getComponentsByBikeId(bikeId: Long): Flow<List<ComponentEntity>> =
        componentDao.getComponentsByBikeId(bikeId)

    suspend fun getComponentById(id: Long): ComponentEntity? = componentDao.getComponentById(id)

    suspend fun insertComponent(component: ComponentEntity): Long {
        val id = componentDao.insert(component)
        insertServiceIntervalsForComponent(id, component.type, component.lifespanKm, component.distanceUsedKm, component.totalTimeSeconds)
        return id
    }

    /**
     * Inserts service intervals for a component. Uses [DefaultServiceIntervalSpecs] when available;
     * otherwise falls back to a single "Max life" Replace interval from lifespanKm.
     */
    private suspend fun insertServiceIntervalsForComponent(
        componentId: Long,
        componentType: String,
        lifespanKm: Double,
        initialTrackedKm: Double,
        initialTrackedTimeSeconds: Long,
    ) {
        val specs = DefaultServiceIntervalSpecs.byType(componentType)
        if (specs.isNotEmpty()) {
            specs
                .filter { it.intervalType != SERVICE_INTERVAL_TYPE_ON_FAILURE }
                .forEach { spec ->
                    serviceIntervalDao.insert(
                        ServiceIntervalEntity(
                            componentId = componentId,
                            name = spec.serviceName,
                            intervalKm = spec.intervalKm,
                            trackedKm = initialTrackedKm,
                            type = spec.intervalType,
                            intervalTimeSeconds = spec.intervalTimeSeconds,
                            trackedTimeSeconds = if (spec.intervalTimeSeconds != null) initialTrackedTimeSeconds else null,
                        ),
                    )
                }
        } else {
            serviceIntervalDao.insert(
                ServiceIntervalEntity(
                    componentId = componentId,
                    name = "Max life",
                    intervalKm = lifespanKm,
                    trackedKm = initialTrackedKm,
                    type = SERVICE_INTERVAL_TYPE_REPLACE,
                ),
            )
        }
    }

    suspend fun updateComponent(component: ComponentEntity) = componentDao.update(component)

    suspend fun deleteComponent(component: ComponentEntity) = componentDao.deleteById(component.id)

    suspend fun getAllComponents(): List<ComponentEntity> = componentDao.getAllComponents()

    fun getComponentsInGarage(): Flow<List<ComponentEntity>> = componentDao.getComponentsInGarage()

    fun getAllComponentsFlow(): Flow<List<ComponentEntity>> = componentDao.getAllComponentsFlow()

    /**
     * Seeds the bike with default components if it has none.
     * Idempotent: calling again for the same bike does not duplicate components.
     */
    suspend fun seedDefaultComponentsIfEmpty(bikeId: Long) {
        if (componentDao.getComponentCountByBikeId(bikeId) > 0) return
        val now = System.currentTimeMillis()
        val entities = DefaultSeedComponents.LIST.map { template ->
            ComponentEntity(
                bikeId = bikeId,
                type = template.type,
                name = template.name,
                lifespanKm = template.defaultLifespanKm,
                distanceUsedKm = 0.0,
                position = template.position,
                baselineKm = 0.0,
                baselineTimeSeconds = 0L,
                installedAt = now,
            )
        }
        entities.forEach { entity ->
            val id = componentDao.insert(entity)
            insertServiceIntervalsForComponent(id, entity.type, entity.lifespanKm, 0.0, 0L)
        }
    }

    /**
     * Adds any default components that are missing for this bike (e.g. bikes created before
     * wheels/brakes/cables were in the default list). Idempotent: only inserts (type, position)
     * pairs that don't already exist.
     */
    suspend fun seedMissingDefaultComponents(bikeId: Long) {
        val existing = componentDao.getComponentsByBikeIdOnce(bikeId)
        val existingKeys = existing.map { it.type to it.position }.toSet()
        val toAdd = DefaultSeedComponents.LIST.filter { (it.type to it.position) !in existingKeys }
        if (toAdd.isEmpty()) return
        val now = System.currentTimeMillis()
        toAdd.forEach { template ->
            val entity = ComponentEntity(
                bikeId = bikeId,
                type = template.type,
                name = template.name,
                lifespanKm = template.defaultLifespanKm,
                distanceUsedKm = 0.0,
                position = template.position,
                baselineKm = 0.0,
                baselineTimeSeconds = 0L,
                installedAt = now,
            )
            val id = componentDao.insert(entity)
            insertServiceIntervalsForComponent(id, entity.type, entity.lifespanKm, 0.0, 0L)
        }
    }

    suspend fun installComponent(component: ComponentEntity, bikeId: Long) {
        val currentSwap = componentSwapDao.getCurrentSwap(component.id)
        currentSwap?.let {
            componentSwapDao.update(it.copy(uninstalledAt = System.currentTimeMillis()))
        }
        componentDao.update(component.copy(bikeId = bikeId))
        componentSwapDao.insert(
            ComponentSwapEntity(
                componentId = component.id,
                bikeId = bikeId,
                installedAt = System.currentTimeMillis(),
                uninstalledAt = null,
            ),
        )
    }

    suspend fun uninstallComponent(component: ComponentEntity) {
        val currentSwap = componentSwapDao.getCurrentSwap(component.id)
        currentSwap?.let {
            componentSwapDao.update(it.copy(uninstalledAt = System.currentTimeMillis()))
        }
        componentDao.update(component.copy(bikeId = null))
    }

    /**
     * Marks inspection (and grease) intervals complete for a component.
     * Resets trackedKm and trackedTimeSeconds for intervals of type inspection or grease.
     * Does not change component distanceUsedKm or totalTimeSeconds.
     */
    suspend fun markInspectionComplete(componentId: Long) {
        serviceIntervalDao.getIntervalsByComponentIdOnce(componentId)
            .filter { it.type == SERVICE_INTERVAL_TYPE_INSPECTION || it.type == SERVICE_INTERVAL_TYPE_GREASE }
            .forEach { interval ->
                serviceIntervalDao.update(
                    interval.copy(
                        trackedKm = 0.0,
                        trackedTimeSeconds = if (interval.intervalTimeSeconds != null) 0L else interval.trackedTimeSeconds,
                    ),
                )
            }
    }

    suspend fun markComponentReplaced(component: ComponentEntity) {
        componentDao.update(
            component.copy(
                distanceUsedKm = 0.0,
                totalTimeSeconds = 0L,
                installedAt = System.currentTimeMillis(),
                alertSnoozeUntilKm = null,
                alertSnoozeUntilTime = null,
            ),
        )
        serviceIntervalDao.getIntervalsByComponentIdOnce(component.id).forEach { interval ->
            serviceIntervalDao.update(
                interval.copy(
                    trackedKm = 0.0,
                    trackedTimeSeconds = if (interval.intervalTimeSeconds != null) 0L else interval.trackedTimeSeconds,
                ),
            )
        }
        val bikeId = component.bikeId ?: return
        val bike = bikeDao.getBikeById(bikeId) ?: return
        when (component.type) {
            "chain" -> bikeDao.update(bike.copy(chainReplacementCount = bike.chainReplacementCount + 1))
            "cassette", "freewheel", "chainring" -> bikeDao.update(bike.copy(chainReplacementCount = 0))
            else -> { }
        }
    }
}
