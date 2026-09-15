package com.clintoncochrane.bikecompanion.ui.trip

import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.ride.RideEntity

/** The two independent controls for the Rides history. */
data class RideHistoryUiState(
    /** Null represents the All bikes filter. */
    val bikeFilterId: Long? = null,
    val sort: RideHistorySort = RideHistorySort.NEWEST,
)

enum class RideHistorySort {
    NEWEST,
    DISTANCE,
    DURATION,
}

/** A ride plus the optional display data resolved from the current bike list. */
data class RideHistoryRow(
    val ride: RideEntity,
    val bikeName: String?,
) {
    val hasAssignedBike: Boolean
        get() = !bikeName.isNullOrBlank()
}

/**
 * Deterministically filters and sorts the single ride source used by the history and its count.
 * Keeping this logic here ensures rows do not independently choose their own order.
 */
object RideHistoryPresenter {

    fun visibleRides(
        rides: List<RideEntity>,
        state: RideHistoryUiState,
    ): List<RideEntity> {
        val filteredRides = state.bikeFilterId?.let { bikeId ->
            rides.filter { it.bikeId == bikeId }
        } ?: rides

        return when (state.sort) {
            RideHistorySort.NEWEST -> filteredRides.sortedWith(
                compareByDescending<RideEntity> { it.endedAt }.thenByDescending { it.id },
            )
            RideHistorySort.DISTANCE -> filteredRides.sortedWith(
                compareByDescending<RideEntity> { it.distanceKm }
                    .thenByDescending { it.endedAt }
                    .thenByDescending { it.id },
            )
            RideHistorySort.DURATION -> filteredRides.sortedWith(
                compareByDescending<RideEntity> { it.durationMs }
                    .thenByDescending { it.endedAt }
                    .thenByDescending { it.id },
            )
        }
    }

    fun toRows(
        rides: List<RideEntity>,
        bikes: List<BikeEntity>,
    ): List<RideHistoryRow> {
        val bikeNamesById = bikes.associate { it.id to it.name }
        return rides.map { ride ->
            RideHistoryRow(
                ride = ride,
                bikeName = ride.bikeId?.let(bikeNamesById::get),
            )
        }
    }
}
