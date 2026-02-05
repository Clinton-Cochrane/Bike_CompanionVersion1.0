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
    private val componentAlertNotifier: ComponentAlertNotifier,
) {
    fun getAllRides(): Flow<List<RideEntity>> = rideDao.getAllRides()

    fun getRidesByBikeId(bikeId: Long): Flow<List<RideEntity>> = rideDao.getRidesByBikeId(bikeId)

    suspend fun getRideById(id: Long): RideEntity? = rideDao.getRideById(id)

    /**
     * Saves a ride and updates the bike's total distance and all components' distanceUsedKm.
     * Call this after a ride ends (in-app or imported).
     */
    suspend fun saveRideAndUpdateBikeAndComponents(ride: RideEntity) {
        val id = rideDao.insert(ride)
        val savedRide = ride.copy(id = id)
        val bikeId = savedRide.bikeId ?: return
        val bike = bikeDao.getBikeById(bikeId) ?: return
        bikeDao.update(
            bike.copy(
                totalDistanceKm = bike.totalDistanceKm + savedRide.distanceKm,
                lastRideAt = savedRide.endedAt,
            ),
        )
        val components = componentDao.getComponentsByBikeIdOnce(bikeId)
        components.forEach { comp ->
            componentDao.update(comp.copy(distanceUsedKm = comp.distanceUsedKm + savedRide.distanceKm))
        }
        componentAlertNotifier.notifyIfNeeded(bikeId)
    }

    suspend fun insertRide(ride: RideEntity): Long = rideDao.insert(ride)

    suspend fun deleteRide(ride: RideEntity) = rideDao.deleteById(ride.id)
}
