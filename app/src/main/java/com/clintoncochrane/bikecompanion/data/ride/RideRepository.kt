package com.clintoncochrane.bikecompanion.data.ride

import com.clintoncochrane.bikecompanion.data.bike.recordedDistanceKm
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
     * Replaces a completed ride without reassigning it to another bike, then reconciles the
     * denormalized bike, component, and service interval totals in the same transaction.
     * Bike reassignment is intentionally handled separately.
     */
    suspend fun updateCompletedRideAndReconcileAggregates(
        oldRide: RideEntity,
        replacementRide: RideEntity,
    ) {
        validateReplacement(oldRide, replacementRide)

        val bikeIdForNotification = ridePersistenceTransaction.run transaction@{
            val persistedRide = requireNotNull(rideDao.getRideById(oldRide.id)) {
                "Ride ${oldRide.id} does not exist"
            }
            require(persistedRide == oldRide) {
                "Ride ${oldRide.id} changed before it could be updated"
            }

            rideDao.update(replacementRide)
            val bikeId = replacementRide.bikeId ?: return@transaction null
            val bike = bikeDao.getBikeById(bikeId) ?: return@transaction null
            val rides = rideDao.getRidesByBikeIdOnce(bikeId)
            val totalDistanceKm = bike.baselineDistanceKm + rides.sumOf { it.distanceKm }
            val totalTimeSeconds = rides.sumOf { it.durationMs / 1000L }
            val maxSpeedKmh = rides.maxOfOrNull { it.maxSpeedKmh } ?: 0.0
            val lastRideAt = rides.maxOfOrNull { it.endedAt }

            bikeDao.update(
                bike.copy(
                    totalDistanceKm = totalDistanceKm,
                    totalTimeSeconds = totalTimeSeconds,
                    lastRideAt = lastRideAt,
                    avgSpeedKmh = averageSpeed(totalDistanceKm - bike.baselineDistanceKm, totalTimeSeconds),
                    maxSpeedKmh = maxSpeedKmh,
                    totalElevGainM = rides.sumOf { it.elevGainM },
                    totalElevLossM = rides.sumOf { it.elevLossM },
                ),
            )

            val distanceDeltaKm = replacementRide.distanceKm - persistedRide.distanceKm
            val timeDeltaSeconds = (replacementRide.durationMs / 1000L) - (persistedRide.durationMs / 1000L)
            val components = componentDao.getComponentsByBikeIdOnce(bikeId)
            components.forEach { component ->
                val componentDistanceKm = component.distanceUsedKm + distanceDeltaKm
                val componentTimeSeconds = component.totalTimeSeconds + timeDeltaSeconds
                componentDao.update(
                    component.copy(
                        distanceUsedKm = componentDistanceKm,
                        totalTimeSeconds = componentTimeSeconds,
                        avgSpeedKmh = averageSpeed(componentDistanceKm, componentTimeSeconds),
                        maxSpeedKmh = maxSpeedKmh,
                        maxSpeedBikeId = if (rides.isEmpty()) null else bikeId,
                    ),
                )
                serviceIntervalDao.getIntervalsByComponentIdOnce(component.id).forEach { interval ->
                    serviceIntervalDao.update(
                        interval.copy(
                            trackedKm = interval.trackedKm + distanceDeltaKm,
                            trackedTimeSeconds = interval.trackedTimeSeconds?.let { it + timeDeltaSeconds },
                        ),
                    )
                }
            }
            bikeId
        }
        bikeIdForNotification?.let { componentAlertNotifier.notifyIfNeeded(it) }
    }

    suspend fun deleteRide(ride: RideEntity) = rideDao.deleteById(ride.id)

    private fun validateReplacement(oldRide: RideEntity, replacementRide: RideEntity) {
        require(oldRide.id > 0L && replacementRide.id == oldRide.id) {
            "A replacement ride must keep the persisted ride id"
        }
        require(replacementRide.bikeId == oldRide.bikeId) {
            "Completed ride reassignment is not supported by this operation"
        }
        require(replacementRide.distanceKm.isValidRideValue()) {
            "Ride distance must be a non-negative finite value"
        }
        require(replacementRide.durationMs >= 0L) {
            "Ride duration must be non-negative"
        }
        require(replacementRide.avgSpeedKmh.isValidRideValue()) {
            "Ride average speed must be a non-negative finite value"
        }
        require(replacementRide.maxSpeedKmh.isValidRideValue()) {
            "Ride maximum speed must be a non-negative finite value"
        }
        require(replacementRide.elevGainM.isValidRideValue()) {
            "Ride elevation gain must be a non-negative finite value"
        }
        require(replacementRide.elevLossM.isValidRideValue()) {
            "Ride elevation loss must be a non-negative finite value"
        }
    }

    private fun Double.isValidRideValue(): Boolean = isFinite() && this >= 0.0

    private fun averageSpeed(distanceKm: Double, timeSeconds: Long): Double =
        if (timeSeconds > 0L) distanceKm / (timeSeconds / 3600.0) else 0.0
}
