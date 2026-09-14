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

    /** Creates a manual mileage entry through the normal completed-ride accounting path. */
    suspend fun saveManualRide(
        bikeId: Long,
        distanceKm: Double,
        occurredAt: Long = System.currentTimeMillis(),
    ) {
        require(bikeId > 0L) { "A manual ride requires a bike" }
        require(distanceKm.isFinite() && distanceKm > 0.0) {
            "Manual ride distance must be positive and finite"
        }
        saveRideAndUpdateBikeAndComponents(
            RideEntity(
                bikeId = bikeId,
                distanceKm = distanceKm,
                durationMs = 0L,
                startedAt = occurredAt,
                endedAt = occurredAt,
                source = RideSource.MANUAL,
            ),
        )
    }

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
            updateBikeAndComponentsForNewRide(savedRide, bike, bikeId)
            bikeId
        }
        bikeIdForNotification?.let { componentAlertNotifier.notifyIfNeeded(it) }
    }

    /** Saves a recovered ride once, including its normal bike/component accounting. */
    suspend fun saveRecoveredRideAndUpdateBikeAndComponents(ride: RideEntity): Boolean {
        require(ride.source == RideSource.APP) { "Recovered rides must be app rides" }
        require(ride.bikeId != null) { "A recovered ride needs a bike before it can be saved" }
        require(!ride.recoveryCheckpointId.isNullOrBlank()) { "A recovery checkpoint ID is required" }
        val bikeIdForNotification = ridePersistenceTransaction.run transaction@{
            val id = rideDao.insertIgnoringDuplicate(ride)
            // A prior process may have committed this recovery immediately before dying. The
            // unique recovery ID makes that case a successful, no-op retry.
            if (id == -1L) return@transaction -1L
            val savedRide = ride.copy(id = id)
            val bikeId = savedRide.bikeId ?: return@transaction null
            val bike = bikeDao.getBikeById(bikeId) ?: return@transaction null
            updateBikeAndComponentsForNewRide(savedRide, bike, bikeId)
            bikeId
        }
        bikeIdForNotification?.takeIf { it > 0L }?.let { componentAlertNotifier.notifyIfNeeded(it) }
        return bikeIdForNotification != null
    }

    /**
     * Saves a Health Connect ride exactly once. The unique record-ID index is checked inside the
     * same transaction as all mileage updates, so repeated scans cannot apply aggregates twice.
     *
     * @return true when the session was newly imported; false when it was already imported.
     */
    suspend fun saveHealthConnectRideAndUpdateBikeAndComponents(ride: RideEntity): Boolean {
        require(ride.source == RideSource.HEALTH_CONNECT) {
            "Only Health Connect rides can use Health Connect duplicate protection"
        }
        require(!ride.healthConnectRecordId.isNullOrBlank()) {
            "A Health Connect ride requires a stable record ID"
        }

        val bikeIdForNotification = ridePersistenceTransaction.run transaction@{
            val id = rideDao.insertIgnoringHealthConnectDuplicate(ride)
            if (id == -1L) return@transaction null
            val savedRide = ride.copy(id = id)
            val bikeId = savedRide.bikeId ?: return@transaction null
            val bike = bikeDao.getBikeById(bikeId) ?: return@transaction null
            updateBikeAndComponentsForNewRide(savedRide, bike, bikeId)
            bikeId
        }
        bikeIdForNotification?.let { componentAlertNotifier.notifyIfNeeded(it) }
        return bikeIdForNotification != null
    }

    suspend fun insertRide(ride: RideEntity): Long = rideDao.insert(ride)

    private suspend fun updateBikeAndComponentsForNewRide(
        ride: RideEntity,
        bike: com.clintoncochrane.bikecompanion.data.bike.BikeEntity,
        bikeId: Long,
    ) {
        val durationSeconds = (ride.durationMs / 1000).coerceAtLeast(0L)
        val newDistance = bike.totalDistanceKm + ride.distanceKm
        val newTimeSeconds = bike.totalTimeSeconds + durationSeconds
        val newRecordedDistance = bike.recordedDistanceKm + ride.distanceKm
        val newAvgSpeed = if (newTimeSeconds > 0) {
            newRecordedDistance / (newTimeSeconds / 3600.0)
        } else bike.avgSpeedKmh
        bikeDao.update(
            bike.copy(
                totalDistanceKm = newDistance,
                totalTimeSeconds = newTimeSeconds,
                lastRideAt = ride.endedAt,
                avgSpeedKmh = newAvgSpeed,
                maxSpeedKmh = maxOf(bike.maxSpeedKmh, ride.maxSpeedKmh),
                totalElevGainM = bike.totalElevGainM + ride.elevGainM,
                totalElevLossM = bike.totalElevLossM + ride.elevLossM,
            ),
        )
        componentDao.getComponentsByBikeIdOnce(bikeId).forEach { component ->
            val distanceUsedKm = component.distanceUsedKm + ride.distanceKm
            val totalTimeSeconds = component.totalTimeSeconds + durationSeconds
            componentDao.update(
                component.copy(
                    distanceUsedKm = distanceUsedKm,
                    totalTimeSeconds = totalTimeSeconds,
                    avgSpeedKmh = averageSpeed(distanceUsedKm, totalTimeSeconds),
                    maxSpeedKmh = maxOf(component.maxSpeedKmh, ride.maxSpeedKmh),
                    maxSpeedBikeId = if (ride.maxSpeedKmh >= component.maxSpeedKmh) bikeId else component.maxSpeedBikeId,
                ),
            )
            serviceIntervalDao.getIntervalsByComponentIdOnce(component.id).forEach { interval ->
                serviceIntervalDao.update(
                    interval.copy(
                        trackedKm = interval.trackedKm + ride.distanceKm,
                        trackedTimeSeconds = if (interval.intervalTimeSeconds != null) {
                            (interval.trackedTimeSeconds ?: 0L) + durationSeconds
                        } else {
                            interval.trackedTimeSeconds
                        },
                    ),
                )
            }
        }
    }

    /**
     * Moves a completed ride to another bike and reconciles all denormalized aggregates in one
     * transaction. Components are selected from their install history at the ride end time.
     */
    suspend fun reassignCompletedRide(rideId: Long, newBikeId: Long) {
        require(rideId > 0L) { "A completed ride id is required" }

        val bikeIdForNotification = ridePersistenceTransaction.run transaction@{
            val savedRide = requireNotNull(rideDao.getRideById(rideId)) {
                "Ride $rideId does not exist"
            }
            val oldBikeId = requireNotNull(savedRide.bikeId) {
                "Ride $rideId is not assigned to a bike"
            }
            if (oldBikeId == newBikeId) return@transaction null

            val oldBike = requireNotNull(bikeDao.getBikeById(oldBikeId)) {
                "Bike $oldBikeId does not exist"
            }
            val newBike = requireNotNull(bikeDao.getBikeById(newBikeId)) {
                "Bike $newBikeId does not exist"
            }
            val reassignedRide = savedRide.copy(bikeId = newBikeId)

            rideDao.update(reassignedRide)
            val oldBikeRides = rideDao.getRidesByBikeIdOnce(oldBikeId)
            val newBikeRides = rideDao.getRidesByBikeIdOnce(newBikeId)
            updateBikeFromRides(oldBike, oldBikeRides)
            updateBikeFromRides(newBike, newBikeRides)
            updateComponentsForDeletedRide(savedRide, oldBikeRides)
            updateComponentsForAssignedRide(reassignedRide, newBikeId)
            newBikeId
        }
        bikeIdForNotification?.let { componentAlertNotifier.notifyIfNeeded(it) }
    }

    /**
     * Replaces a completed ride without changing its bike, then reconciles every aggregate that
     * the original completed ride affected. Bike reassignment remains owned by [reassignCompletedRide].
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
            updateBikeFromRides(bike, rides)
            updateComponentsForEditedRide(persistedRide, replacementRide, rides)
            bikeId
        }
        bikeIdForNotification?.let { componentAlertNotifier.notifyIfNeeded(it) }
    }

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
        componentsInstalledOnBikeAt(bikeId, deletedRide.endedAt).forEach { component ->
            updateComponentForDeletedRide(component, deletedRide, remainingRides, bikeId)
        }
    }

    private suspend fun updateComponentsForEditedRide(
        oldRide: RideEntity,
        replacementRide: RideEntity,
        rides: List<RideEntity>,
    ) {
        val bikeId = oldRide.bikeId ?: return
        val distanceDeltaKm = replacementRide.distanceKm - oldRide.distanceKm
        val timeDeltaSeconds = (replacementRide.durationMs / 1000L) - (oldRide.durationMs / 1000L)
        componentsInstalledOnBikeAt(bikeId, oldRide.endedAt).forEach { component ->
            val distanceUsedKm = (component.distanceUsedKm + distanceDeltaKm).coerceAtLeast(0.0)
            val totalTimeSeconds = (component.totalTimeSeconds + timeDeltaSeconds).coerceAtLeast(0L)
            val maxSpeedKmh = when {
                replacementRide.maxSpeedKmh >= component.maxSpeedKmh -> replacementRide.maxSpeedKmh
                oldRide.maxSpeedKmh >= component.maxSpeedKmh -> rides
                    .filter { it.endedAt >= component.installedAt }
                    .maxOfOrNull { it.maxSpeedKmh }
                    ?: 0.0
                else -> component.maxSpeedKmh
            }
            componentDao.update(
                component.copy(
                    distanceUsedKm = distanceUsedKm,
                    totalTimeSeconds = totalTimeSeconds,
                    avgSpeedKmh = averageSpeed(distanceUsedKm, totalTimeSeconds),
                    maxSpeedKmh = maxSpeedKmh,
                    maxSpeedBikeId = if (maxSpeedKmh > 0.0) bikeId else null,
                ),
            )
            serviceIntervalDao.getIntervalsByComponentIdOnce(component.id).forEach { interval ->
                serviceIntervalDao.update(
                    interval.copy(
                        trackedKm = (interval.trackedKm + distanceDeltaKm).coerceAtLeast(0.0),
                        trackedTimeSeconds = if (interval.intervalTimeSeconds != null) {
                            ((interval.trackedTimeSeconds ?: 0L) + timeDeltaSeconds).coerceAtLeast(0L)
                        } else {
                            interval.trackedTimeSeconds
                        },
                    ),
                )
            }
        }
    }

    private suspend fun updateComponentsForAssignedRide(assignedRide: RideEntity, bikeId: Long) {
        val durationSeconds = (assignedRide.durationMs / 1000L).coerceAtLeast(0L)
        componentsInstalledOnBikeAt(bikeId, assignedRide.endedAt).forEach { component ->
            val distanceUsedKm = component.distanceUsedKm + assignedRide.distanceKm
            val totalTimeSeconds = component.totalTimeSeconds + durationSeconds
            componentDao.update(
                component.copy(
                    distanceUsedKm = distanceUsedKm,
                    totalTimeSeconds = totalTimeSeconds,
                    avgSpeedKmh = if (totalTimeSeconds > 0L) {
                        distanceUsedKm / (totalTimeSeconds / 3600.0)
                    } else {
                        0.0
                    },
                    maxSpeedKmh = maxOf(component.maxSpeedKmh, assignedRide.maxSpeedKmh),
                    maxSpeedBikeId = if (assignedRide.maxSpeedKmh >= component.maxSpeedKmh) bikeId else component.maxSpeedBikeId,
                ),
            )
            serviceIntervalDao.getIntervalsByComponentIdOnce(component.id).forEach { interval ->
                serviceIntervalDao.update(
                    interval.copy(
                        trackedKm = interval.trackedKm + assignedRide.distanceKm,
                        trackedTimeSeconds = if (interval.intervalTimeSeconds != null) {
                            (interval.trackedTimeSeconds ?: 0L) + durationSeconds
                        } else {
                            interval.trackedTimeSeconds
                        },
                    ),
                )
            }
        }
    }

    private suspend fun componentsInstalledOnBikeAt(bikeId: Long, rideEndedAt: Long): List<ComponentEntity> {
        val historicalComponentIds = componentSwapDao.getComponentIdsInstalledOnBikeAt(bikeId, rideEndedAt)
        val historicalComponents = if (historicalComponentIds.isEmpty()) {
            emptyList()
        } else {
            componentDao.getComponentsByIdsOnce(historicalComponentIds)
        }
        val legacyInstalledComponents = componentDao.getComponentsByBikeIdOnce(bikeId)
            .filter { it.installedAt <= rideEndedAt }
        return (historicalComponents + legacyInstalledComponents).distinctBy { it.id }
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
