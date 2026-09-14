package com.clintoncochrane.bikecompanion.data.component

import com.clintoncochrane.bikecompanion.data.bike.BikeDao
import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.image.ImageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComponentRepository @Inject constructor(
    private val componentDao: ComponentDao,
    private val serviceIntervalDao: ServiceIntervalDao,
    private val componentSwapDao: ComponentSwapDao,
    private val bikeDao: BikeDao,
    private val imageRepository: ImageRepository,
    private val lifecycleTransaction: ComponentLifecycleTransaction,
) {
    fun getComponentsByBikeId(bikeId: Long): Flow<List<ComponentEntity>> =
        componentDao.getComponentsByBikeId(bikeId)

    suspend fun getComponentsByBikeIdOnce(bikeId: Long): List<ComponentEntity> =
        componentDao.getComponentsByBikeIdOnce(bikeId)

    suspend fun getComponentById(id: Long): ComponentEntity? = componentDao.getComponentById(id)

    suspend fun insertComponent(component: ComponentEntity): Long = lifecycleTransaction.run {
        insertComponentRecords(component)
    }

    private suspend fun insertComponentRecords(component: ComponentEntity): Long {
        require(component.bikeId != null || component.lifecycleStatus != ComponentLifecycleStatus.INSTALLED) {
            "An installed component requires a bike"
        }
        require(component.bikeId == null || component.lifecycleStatus == ComponentLifecycleStatus.INSTALLED) {
            "A component assigned to a bike must be installed"
        }
        val id = componentDao.insert(component)
        insertServiceIntervalsForComponent(id, component.type, component.lifespanKm, component.lifetimeDistanceKm, component.totalTimeSeconds)
        component.bikeId?.let { bikeId ->
            componentSwapDao.insert(
                ComponentSwapEntity(
                    componentId = id,
                    bikeId = bikeId,
                    installedAt = component.installedAt,
                ),
            )
        }
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

    suspend fun deleteComponent(component: ComponentEntity) {
        imageRepository.deleteComponentImage(component.id)
        componentDao.deleteById(component.id)
    }

    suspend fun getAllComponents(): List<ComponentEntity> = componentDao.getAllComponents()

    fun getComponentsInGarage(): Flow<List<ComponentEntity>> = componentDao.getComponentsInGarage()

    fun getNonRetiredComponents(): Flow<List<ComponentEntity>> = componentDao.getNonRetiredComponents()

    fun getRetiredComponents(): Flow<List<ComponentEntity>> = componentDao.getRetiredComponents()

    fun getAllComponentsFlow(): Flow<List<ComponentEntity>> = componentDao.getAllComponentsFlow()

    /**
     * Seeds the bike with components filtered by drivetrain and brake type (Simple Add flow).
     * Call only for a newly created bike with no components.
     */
    suspend fun seedComponentsForBikeType(bikeId: Long, drivetrainType: String, brakeType: String) =
        lifecycleTransaction.run {
            val list = DefaultSeedComponents.seedListFor(drivetrainType, brakeType)
            val now = System.currentTimeMillis()
            list.forEach { template ->
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
                insertComponentRecords(entity)
            }
        }

    /**
     * Seeds the bike with default components if it has none.
     * Idempotent: calling again for the same bike does not duplicate components.
     */
    suspend fun seedDefaultComponentsIfEmpty(bikeId: Long) = lifecycleTransaction.run {
        if (componentDao.getComponentCountByBikeId(bikeId) > 0) return@run
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
            insertComponentRecords(entity)
        }
    }

    /**
     * Adds any default components that are missing for this bike (e.g. bikes created before
     * wheels/brakes/cables were in the default list). Idempotent: only inserts (type, position)
     * pairs that don't already exist.
     */
    suspend fun seedMissingDefaultComponents(bikeId: Long) = lifecycleTransaction.run {
        val existing = componentDao.getComponentsByBikeIdOnce(bikeId)
        val existingKeys = existing.map { it.type to it.position }.toSet()
        val toAdd = DefaultSeedComponents.LIST.filter { (it.type to it.position) !in existingKeys }
        if (toAdd.isEmpty()) return@run
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
            insertComponentRecords(entity)
        }
    }

    /**
     * Returns true if the target bike already has a component with the same type and position.
     * Used to prevent duplicate parts when swapping or installing.
     */
    suspend fun wouldBeDuplicatePart(component: ComponentEntity, targetBikeId: Long): Boolean {
        val existing = componentDao.getComponentsByBikeIdOnce(targetBikeId)
        return existing.any { it.type == component.type && it.position == component.position }
    }

    suspend fun installComponent(component: ComponentEntity, bikeId: Long) {
        lifecycleTransaction.run {
            val persisted = requireNotNull(componentDao.getComponentById(component.id))
            require(persisted.lifecycleStatus != ComponentLifecycleStatus.RETIRED) {
                "Retired components cannot be installed"
            }
            require(!wouldBeDuplicatePart(persisted, bikeId)) {
                "A component of this type is already installed on the target bike"
            }
            val now = System.currentTimeMillis()
            componentSwapDao.getCurrentSwap(persisted.id)?.let {
                componentSwapDao.update(it.copy(uninstalledAt = now))
            }
            componentDao.update(
                persisted.copy(
                    bikeId = bikeId,
                    lifecycleStatus = ComponentLifecycleStatus.INSTALLED,
                    installedAt = now,
                ),
            )
            componentSwapDao.insert(
                ComponentSwapEntity(componentId = persisted.id, bikeId = bikeId, installedAt = now),
            )
        }
    }

    suspend fun uninstallComponent(component: ComponentEntity) {
        removeToGarage(component)
    }

    suspend fun removeToGarage(component: ComponentEntity) {
        lifecycleTransaction.run {
            val persisted = requireNotNull(componentDao.getComponentById(component.id))
            require(persisted.lifecycleStatus != ComponentLifecycleStatus.RETIRED) {
                "Retired components cannot be moved to the garage"
            }
            val now = System.currentTimeMillis()
            componentSwapDao.getCurrentSwap(persisted.id)?.let {
                componentSwapDao.update(it.copy(uninstalledAt = now))
            }
            componentDao.update(
                persisted.copy(bikeId = null, lifecycleStatus = ComponentLifecycleStatus.IN_GARAGE),
            )
        }
    }

    suspend fun retireComponent(component: ComponentEntity) {
        lifecycleTransaction.run {
            val persisted = requireNotNull(componentDao.getComponentById(component.id))
            if (persisted.lifecycleStatus == ComponentLifecycleStatus.RETIRED) return@run
            val now = System.currentTimeMillis()
            componentSwapDao.getCurrentSwap(persisted.id)?.let {
                componentSwapDao.update(it.copy(uninstalledAt = now))
            }
            componentDao.update(
                persisted.copy(bikeId = null, lifecycleStatus = ComponentLifecycleStatus.RETIRED),
            )
        }
    }

    /**
     * Returns expected component slots (type, position) that the bike does not have.
     * Uses [DefaultSeedComponents.seedListFor] based on bike drivetrain and brake type.
     */
    suspend fun getMissingComponentsForBike(bike: BikeEntity): List<DefaultSeedComponent> {
        val expected = DefaultSeedComponents.seedListFor(bike.drivetrainType, bike.brakeType)
        val existing = componentDao.getComponentsByBikeIdOnce(bike.id)
        val existingKeys = existing.map { it.type to it.position }.toSet()
        return expected.filter { (it.type to it.position) !in existingKeys }
    }

    /**
     * Returns components in the garage (bikeId = null) that match the given type and position.
     */
    suspend fun getComponentsInGarageMatching(type: String, position: String): List<ComponentEntity> {
        return componentDao.getComponentsInGarageOnce()
            .filter { it.type == type && it.position == position }
    }

    suspend fun replaceComponent(component: ComponentEntity, replacement: ComponentEntity): Long =
        lifecycleTransaction.run {
            val oldComponent = requireNotNull(componentDao.getComponentById(component.id))
            val bikeId = requireNotNull(oldComponent.bikeId) { "Only installed components can be replaced" }
            require(oldComponent.lifecycleStatus == ComponentLifecycleStatus.INSTALLED)
            require(replacement.type == oldComponent.type && replacement.position == oldComponent.position) {
                "Replacement must use the same component slot"
            }
            require(replacement.lifespanKm.isFinite() && replacement.lifespanKm >= 0.0)
            require(replacement.baselineKm.isFinite() && replacement.baselineKm >= 0.0)
            require(componentDao.getComponentsByBikeIdOnce(bikeId).none {
                it.id != oldComponent.id && it.type == oldComponent.type && it.position == oldComponent.position
            }) { "The bike already has another component in this slot" }

            val now = System.currentTimeMillis()
            componentSwapDao.getCurrentSwap(oldComponent.id)?.let {
                componentSwapDao.update(it.copy(uninstalledAt = now))
            }
            componentDao.update(oldComponent.copy(bikeId = null, lifecycleStatus = ComponentLifecycleStatus.RETIRED))

            val installedReplacement = replacement.copy(
                id = 0,
                bikeId = bikeId,
                lifecycleStatus = ComponentLifecycleStatus.INSTALLED,
                distanceUsedKm = 0.0,
                totalTimeSeconds = 0L,
                baselineKm = if (replacement.priorUsageCertainty == PriorUsageCertainty.UNKNOWN) {
                    0.0
                } else {
                    replacement.baselineKm
                },
                installedAt = now,
            )
            val replacementId = componentDao.insert(installedReplacement)
            insertServiceIntervalsForComponent(
                replacementId,
                installedReplacement.type,
                installedReplacement.lifespanKm,
                installedReplacement.lifetimeDistanceKm,
                0L,
            )
            componentSwapDao.insert(
                ComponentSwapEntity(
                    componentId = replacementId,
                    bikeId = bikeId,
                    installedAt = now,
                ),
            )

            bikeDao.getBikeById(bikeId)?.let { bike ->
                when (oldComponent.type) {
                    "chain" -> bikeDao.update(bike.copy(chainReplacementCount = bike.chainReplacementCount + 1))
                    "cassette", "freewheel", "chainring" -> bikeDao.update(bike.copy(chainReplacementCount = 0))
                    else -> Unit
                }
            }
            replacementId
        }
}
