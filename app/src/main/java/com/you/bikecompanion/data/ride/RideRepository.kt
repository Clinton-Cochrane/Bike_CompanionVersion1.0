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
     * and incremented on trip completion for fast reads. See TRIP_TIME_TRACKING.md.
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
                        trackedKm = compNewDistance,
                        trackedTimeSeconds = if (interval.intervalTimeSeconds != null) compNewTime else interval.trackedTimeSeconds,
                    ),
                )
            }
        }
        componentAlertNotifier.notifyIfNeeded(bikeId)
    }

    suspend fun insertRide(ride: RideEntity): Long = rideDao.insert(ride)

    suspend fun deleteRide(ride: RideEntity) = rideDao.deleteById(ride.id)
}
