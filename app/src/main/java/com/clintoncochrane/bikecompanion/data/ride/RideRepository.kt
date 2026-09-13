package com.clintoncochrane.bikecompanion.data.ride

import com.clintoncochrane.bikecompanion.data.bike.recordedDistanceKm
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentSwapDao
import com.clintoncochrane.bikecompanion.notifications.ComponentAlertNotifier
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RideRepository @Inject constructor(
    private val rideDao: RideDao,
    private val bikeDao: com.clintoncochrane.bikecompanion.data.bike.BikeDao,
    private val componentDao: com.clintoncochrane.bikecompanion.data.component.ComponentDao,
    private val serviceIntervalDao: com.clintoncochrane.bikecompanion.data.component.ServiceIntervalDao,
    private val componentSwapDao: ComponentSwapDao,
    private val componentAlertNotifier: ComponentAlertNotifier,
    private val ridePersistenceTransaction: RidePersistenceTransaction,
) {
    fun getAllRides(): Flow<List<RideEntity>> = rideDao.getAllRides()

    fun getRidesByBikeId(bikeId: Long): Flow<List<RideEntity>> = rideDao.getRidesByBikeId(bikeId)

    suspend fun getRideById(id: Long): RideEntity? = rideDao.getRideById(id)

    /**
     * Saves a ride and updates the bike's total distance, total time, and all components'
     * distanceUsedKm and totalTimeSeconds. Call this after a ride ends (in-app or imported).
     *
     * Aggregation strategy: Option B (denormalized). Totals are stored on bike/components
     * and incremented on trip completion for fast reads. See TRIP_TIME_TRACKING.md.
     */
    suspend fun saveRideAndUpdateBikeAndComponents(ride: RideEntity) {
        val bikeIdForNotification = ridePersistenceTransaction.run transaction@{
            val id = rideDao.insert(ride)
            val savedRide = ride.copy(id = id)
            val bikeId = savedRide.bikeId ?: return@transaction null
            val bike = bikeDao.getBikeById(bikeId) ?: return@transaction null
            val durationSeconds = (savedRide.durationMs / 1000).coerceAtLeast(0L)
            val newDistance = bike.totalDistanceKm + savedRide.distanceKm
            val newTimeSeconds = bike.totalTimeSeconds + durationSeconds
            val newRecordedDistance = bike.recordedDistanceKm + savedRide.distanceKm
            val newAvgSpeed = if (newTimeSeconds > 0) {
                newRecordedDistance / (newTimeSeconds / 3600.0)
            } else bike.avgSpeedKmh
            val newMaxSpeed = maxOf(bike.maxSpeedKmh, savedRide.maxSpeedKmh)
            bikeDao.update(
                bike.copy(
                    totalDistanceKm = newDistance,
                    totalTimeSeconds = newTimeSeconds,
                    lastRideAt = savedRide.endedAt,
                    avgSpeedKmh = newAvgSpeed,
                    maxSpeedKmh = newMaxSpeed,
                    totalElevGainM = bike.totalElevGainM + savedRide.elevGainM,
                    totalElevLossM = bike.totalElevLossM + savedRide.elevLossM,
                ),
            )
            val components = componentDao.getComponentsByBikeIdOnce(bikeId)
            components.forEach { comp ->
                val compNewDistance = comp.distanceUsedKm + savedRide.distanceKm
                val compNewTime = comp.totalTimeSeconds + durationSeconds
                val compNewAvgSpeed = if (compNewTime > 0) {
                    compNewDistance / (compNewTime / 3600.0)
                } else comp.avgSpeedKmh
                val compNewMaxSpeed = maxOf(comp.maxSpeedKmh, savedRide.maxSpeedKmh)
                val compNewMaxSpeedBikeId = if (savedRide.maxSpeedKmh >= comp.maxSpeedKmh) bikeId else comp.maxSpeedBikeId
                componentDao.update(
                    comp.copy(
                        distanceUsedKm = compNewDistance,
                        totalTimeSeconds = compNewTime,
                        avgSpeedKmh = compNewAvgSpeed,
                        maxSpeedKmh = compNewMaxSpeed,
                        maxSpeedBikeId = compNewMaxSpeedBikeId,
                    ),
                )
                serviceIntervalDao.getIntervalsByComponentIdOnce(comp.id).forEach { interval ->
                    serviceIntervalDao.update(
                        interval.copy(
                            trackedKm = interval.trackedKm + savedRide.distanceKm,
                            trackedTimeSeconds = if (interval.intervalTimeSeconds != null) {
                                (interval.trackedTimeSeconds ?: 0L) + durationSeconds
                            } else {
                                interval.trackedTimeSeconds
                            },
                        ),
                    )
                }
            }
            bikeId
        }
        bikeIdForNotification?.let { componentAlertNotifier.notifyIfNeeded(it) }
    }

    suspend fun insertRide(ride: RideEntity): Long = rideDao.insert(ride)

    /**
     * Deletes a completed ride and reconciles every denormalized value that ride updated.
     *
     * Bike totals are rebuilt from authoritative ride history. Component and service usage are
     * safely decremented only for components installed when the deleted ride ended; component
     * swaps provide that history, while [ComponentEntity.installedAt] covers legacy components
     * that predate swap records.
     */
    suspend fun deleteRide(ride: RideEntity) {
        ridePersistenceTransaction.run transaction@{
            val savedRide = rideDao.getRideById(ride.id) ?: return@transaction null
            val bikeId = savedRide.bikeId ?: run {
                rideDao.deleteById(savedRide.id)
                return@transaction null
            }
            val bike = bikeDao.getBikeById(bikeId) ?: run {
                rideDao.deleteById(savedRide.id)
                return@transaction null
            }

            rideDao.deleteById(savedRide.id)
            val remainingRides = rideDao.getRidesByBikeIdOnce(bikeId)
            updateBikeFromRides(bike, remainingRides)
            updateComponentsForDeletedRide(savedRide, remainingRides)
            savedRide.id
        }
    }

    private suspend fun updateBikeFromRides(
        bike: com.clintoncochrane.bikecompanion.data.bike.BikeEntity,
        rides: List<RideEntity>,
    ) {
        val recordedDistanceKm = rides.sumOf { it.distanceKm }.coerceAtLeast(0.0)
        val totalTimeSeconds = rides.sumOf { it.durationMs / 1000 }.coerceAtLeast(0L)
        val avgSpeedKmh = if (totalTimeSeconds > 0L) {
            recordedDistanceKm / (totalTimeSeconds / 3600.0)
        } else {
            0.0
        }
        bikeDao.update(
            bike.copy(
                totalDistanceKm = bike.baselineDistanceKm + recordedDistanceKm,
                totalTimeSeconds = totalTimeSeconds,
                lastRideAt = rides.maxOfOrNull { it.endedAt },
                avgSpeedKmh = avgSpeedKmh,
                maxSpeedKmh = rides.maxOfOrNull { it.maxSpeedKmh } ?: 0.0,
                totalElevGainM = rides.sumOf { it.elevGainM }.coerceAtLeast(0.0),
                totalElevLossM = rides.sumOf { it.elevLossM }.coerceAtLeast(0.0),
            ),
        )
    }

    private suspend fun updateComponentsForDeletedRide(
        deletedRide: RideEntity,
        remainingRides: List<RideEntity>,
    ) {
        val bikeId = deletedRide.bikeId ?: return
        val historicalComponentIds = componentSwapDao.getComponentIdsInstalledOnBikeAt(bikeId, deletedRide.endedAt)
        val historicalComponents = if (historicalComponentIds.isEmpty()) {
            emptyList()
        } else {
            componentDao.getComponentsByIdsOnce(historicalComponentIds)
        }
        val legacyInstalledComponents = componentDao.getComponentsByBikeIdOnce(bikeId)
            .filter { it.installedAt <= deletedRide.endedAt }
        (historicalComponents + legacyInstalledComponents)
            .distinctBy { it.id }
            .forEach { component -> updateComponentForDeletedRide(component, deletedRide, remainingRides, bikeId) }
    }

    private suspend fun updateComponentForDeletedRide(
        component: ComponentEntity,
        deletedRide: RideEntity,
        remainingRides: List<RideEntity>,
        bikeId: Long,
    ) {
        val durationSeconds = (deletedRide.durationMs / 1000).coerceAtLeast(0L)
        val distanceUsedKm = (component.distanceUsedKm - deletedRide.distanceKm).coerceAtLeast(0.0)
        val totalTimeSeconds = (component.totalTimeSeconds - durationSeconds).coerceAtLeast(0L)
        val remainingMaxSpeedKmh = if (deletedRide.maxSpeedKmh >= component.maxSpeedKmh) {
            remainingRides
                .filter { it.endedAt >= component.installedAt }
                .maxOfOrNull { it.maxSpeedKmh }
                ?: 0.0
        } else {
            component.maxSpeedKmh
        }
        componentDao.update(
            component.copy(
                distanceUsedKm = distanceUsedKm,
                totalTimeSeconds = totalTimeSeconds,
                avgSpeedKmh = if (totalTimeSeconds > 0L) distanceUsedKm / (totalTimeSeconds / 3600.0) else 0.0,
                maxSpeedKmh = remainingMaxSpeedKmh,
                maxSpeedBikeId = if (remainingMaxSpeedKmh > 0.0) bikeId else null,
            ),
        )
        serviceIntervalDao.getIntervalsByComponentIdOnce(component.id).forEach { interval ->
            serviceIntervalDao.update(
                interval.copy(
                    trackedKm = (interval.trackedKm - deletedRide.distanceKm).coerceAtLeast(0.0),
                    trackedTimeSeconds = if (interval.intervalTimeSeconds != null) {
                        ((interval.trackedTimeSeconds ?: 0L) - durationSeconds).coerceAtLeast(0L)
                    } else {
                        interval.trackedTimeSeconds
                    },
                ),
            )
        }
    }
}
