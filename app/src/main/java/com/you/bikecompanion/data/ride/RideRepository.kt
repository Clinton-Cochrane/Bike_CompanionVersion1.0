package com.you.bikecompanion.data.ride

import com.you.bikecompanion.notifications.ComponentAlertNotifier
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RideRepository @Inject constructor(
    private val rideDao: RideDao,
    private val bikeDao: com.you.bikecompanion.data.bike.BikeDao,
    private val componentDao: com.you.bikecompanion.data.component.ComponentDao,
    private val serviceIntervalDao: com.you.bikecompanion.data.component.ServiceIntervalDao,
    private val componentAlertNotifier: ComponentAlertNotifier,
) {
    fun getAllRides(): Flow<List<RideEntity>> = rideDao.getAllRides()

    fun getRidesByBikeId(bikeId: Long): Flow<List<RideEntity>> = rideDao.getRidesByBikeId(bikeId)

    suspend fun getRideById(id: Long): RideEntity? = rideDao.getRideById(id)

    /**
     * Saves a ride and updates the bike's total distance, total time, and all components'
     * distanceUsedKm and totalTimeSeconds. Call this after a ride ends (in-app or imported).
     *
     * Aggregation strategy: Option B (denormalized). Totals are stored on bike/components
     * and incremented on trip completion for fast reads. Reverted by [deleteRidesAndRevertAggregates].
     */
    suspend fun saveRideAndUpdateBikeAndComponents(ride: RideEntity) {
        val id = rideDao.insert(ride)
        val savedRide = ride.copy(id = id)
        val bikeId = savedRide.bikeId ?: return
        val bike = bikeDao.getBikeById(bikeId) ?: return
        val durationSeconds = (savedRide.durationMs / 1000).coerceAtLeast(0L)
        val newDistance = bike.totalDistanceKm + savedRide.distanceKm
        val newTimeSeconds = bike.totalTimeSeconds + durationSeconds
        val newAvgSpeed = if (newTimeSeconds > 0) {
            newDistance / (newTimeSeconds / 3600.0)
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
        componentAlertNotifier.notifyIfNeeded(bikeId)
    }

    suspend fun insertRide(ride: RideEntity): Long = rideDao.insert(ride)

    /**
     * Deletes [ride] and reverts denormalized totals on the bike, its components, and service intervals
     * (mirror of [saveRideAndUpdateBikeAndComponents]).
     */
    suspend fun deleteRide(ride: RideEntity) {
        deleteRidesAndRevertAggregates(listOf(ride))
    }

    /**
     * Deletes rides and reverts denormalized roll-ups. Rides with null [RideEntity.bikeId] are only removed
     * from the rides table. Grouped by bike so max speed / last ride date reflect remaining rows.
     *
     * Steps run sequentially (not in a single DB transaction) to keep JVM unit tests simple; callers
     * should invoke from a coroutine scope off the main thread.
     */
    suspend fun deleteRidesAndRevertAggregates(rides: List<RideEntity>) {
        val unique = rides.distinctBy { it.id }
        if (unique.isEmpty()) return

        unique.filter { it.bikeId == null }.forEach { ride ->
            rideDao.deleteById(ride.id)
        }

        val byBike = unique.filter { it.bikeId != null }.groupBy { it.bikeId!! }
        for ((bikeId, toRemove) in byBike) {
            val excludeIds = toRemove.map { it.id }
            val sumDistanceKm = toRemove.sumOf { it.distanceKm }
            val sumDurationSeconds = toRemove.sumOf { (it.durationMs / 1000).coerceAtLeast(0L) }
            val sumElevGainM = toRemove.sumOf { it.elevGainM }
            val sumElevLossM = toRemove.sumOf { it.elevLossM }

            val bike = bikeDao.getBikeById(bikeId)
            if (bike == null) {
                toRemove.forEach { rideDao.deleteById(it.id) }
                continue
            }

            val maxEndedAtRemaining = rideDao.getMaxEndedAtExcluding(bikeId, excludeIds)
            val maxSpeedRemaining = rideDao.getMaxMaxSpeedKmhExcluding(bikeId, excludeIds)?.coerceAtLeast(0.0) ?: 0.0

            val newDistanceKm = (bike.totalDistanceKm - sumDistanceKm).coerceAtLeast(0.0)
            val newTimeSeconds = (bike.totalTimeSeconds - sumDurationSeconds).coerceAtLeast(0L)
            val newAvgSpeedKmh = if (newTimeSeconds > 0) {
                newDistanceKm / (newTimeSeconds / 3600.0)
            } else {
                0.0
            }

            bikeDao.update(
                bike.copy(
                    totalDistanceKm = newDistanceKm,
                    totalTimeSeconds = newTimeSeconds,
                    lastRideAt = maxEndedAtRemaining,
                    avgSpeedKmh = newAvgSpeedKmh,
                    maxSpeedKmh = maxSpeedRemaining,
                    totalElevGainM = (bike.totalElevGainM - sumElevGainM).coerceAtLeast(0.0),
                    totalElevLossM = (bike.totalElevLossM - sumElevLossM).coerceAtLeast(0.0),
                ),
            )

            val components = componentDao.getComponentsByBikeIdOnce(bikeId)
            val compMaxSpeedBikeId = if (maxSpeedRemaining > 0.0) bikeId else null
            components.forEach { comp ->
                val compNewDistance = (comp.distanceUsedKm - sumDistanceKm).coerceAtLeast(0.0)
                val compNewTime = (comp.totalTimeSeconds - sumDurationSeconds).coerceAtLeast(0L)
                val compNewAvg = if (compNewTime > 0) {
                    compNewDistance / (compNewTime / 3600.0)
                } else {
                    0.0
                }
                componentDao.update(
                    comp.copy(
                        distanceUsedKm = compNewDistance,
                        totalTimeSeconds = compNewTime,
                        avgSpeedKmh = compNewAvg,
                        maxSpeedKmh = maxSpeedRemaining,
                        maxSpeedBikeId = compMaxSpeedBikeId,
                    ),
                )
                serviceIntervalDao.getIntervalsByComponentIdOnce(comp.id).forEach { interval ->
                    val newTrackedKm = (interval.trackedKm - sumDistanceKm).coerceAtLeast(0.0)
                    val newTrackedTimeSeconds = if (interval.intervalTimeSeconds != null) {
                        ((interval.trackedTimeSeconds ?: 0L) - sumDurationSeconds).coerceAtLeast(0L)
                    } else {
                        interval.trackedTimeSeconds
                    }
                    serviceIntervalDao.update(
                        interval.copy(
                            trackedKm = newTrackedKm,
                            trackedTimeSeconds = newTrackedTimeSeconds,
                        ),
                    )
                }
            }

            toRemove.forEach { ride ->
                rideDao.deleteById(ride.id)
            }
            componentAlertNotifier.notifyIfNeeded(bikeId)
        }
    }
}
