package com.clintoncochrane.bikecompanion.data.bike

import com.clintoncochrane.bikecompanion.data.component.ComponentDao
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentLifecycleStatus
import com.clintoncochrane.bikecompanion.data.component.ComponentLifecycleTransaction
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import com.clintoncochrane.bikecompanion.data.component.ServiceIntervalDao
import javax.inject.Inject
import javax.inject.Singleton

enum class MileageCorrectionResult {
    APPLIED,
    BIKE_NOT_FOUND,
    INVALID_BIKE_MILEAGE,
    BELOW_RECORDED_RIDE_DISTANCE,
    COMPONENT_NOT_INSTALLED,
    COMPONENT_WOULD_BECOME_NEGATIVE,
}

@Singleton
class BikeMileageCorrectionRepository @Inject constructor(
    private val bikeDao: BikeDao,
    private val componentDao: ComponentDao,
    private val serviceIntervalDao: ServiceIntervalDao,
    private val transaction: ComponentLifecycleTransaction,
) {
    suspend fun correctMileage(
        bikeId: Long,
        correctedMileageKm: Double,
        selectedComponentIds: Set<Long>,
    ): MileageCorrectionResult = transaction.run {
        if (!correctedMileageKm.isFinite() || correctedMileageKm < 0.0) {
            return@run MileageCorrectionResult.INVALID_BIKE_MILEAGE
        }

        val bike = bikeDao.getBikeById(bikeId)
            ?: return@run MileageCorrectionResult.BIKE_NOT_FOUND
        if (correctedMileageKm < bike.recordedDistanceKm) {
            return@run MileageCorrectionResult.BELOW_RECORDED_RIDE_DISTANCE
        }

        val componentIds = selectedComponentIds.sorted()
        val components = if (componentIds.isEmpty()) {
            emptyList()
        } else {
            componentDao.getComponentsByIdsOnce(componentIds)
        }
        val hasInvalidComponent = components.any { component ->
            component.bikeId != bikeId || component.lifecycleStatus != ComponentLifecycleStatus.INSTALLED
        }
        if (components.size != componentIds.size || hasInvalidComponent) {
            return@run MileageCorrectionResult.COMPONENT_NOT_INSTALLED
        }

        val deltaKm = correctedMileageKm - bike.totalDistanceKm
        if (components.any { it.lifetimeDistanceKm + deltaKm < 0.0 }) {
            return@run MileageCorrectionResult.COMPONENT_WOULD_BECOME_NEGATIVE
        }

        val correctedBikeBaselineKm = correctedMileageKm - bike.recordedDistanceKm
        bikeDao.update(bike.withBaselineDistanceKm(correctedBikeBaselineKm))
        components.forEach { component ->
            componentDao.update(component.withMileageCorrection(deltaKm))
            serviceIntervalDao.getIntervalsByComponentIdOnce(component.id).forEach { interval ->
                serviceIntervalDao.update(
                    interval.copy(trackedKm = (interval.trackedKm + deltaKm).coerceAtLeast(0.0)),
                )
            }
        }

        MileageCorrectionResult.APPLIED
    }
}

private fun ComponentEntity.withMileageCorrection(deltaKm: Double): ComponentEntity {
    val correctedLifetimeKm = (lifetimeDistanceKm + deltaKm).coerceAtLeast(0.0)
    if (priorUsageCertainty == PriorUsageCertainty.UNKNOWN) {
        return copy(distanceUsedKm = correctedLifetimeKm)
    }

    // Keep both persisted distance fields non-negative while applying the exact lifetime delta.
    // A downward correction consumes prior-use baseline before touching app-tracked distance.
    val correctedBaselineKm = (baselineKm + deltaKm).coerceAtLeast(0.0)
    return copy(
        baselineKm = correctedBaselineKm,
        distanceUsedKm = correctedLifetimeKm - correctedBaselineKm,
    )
}
