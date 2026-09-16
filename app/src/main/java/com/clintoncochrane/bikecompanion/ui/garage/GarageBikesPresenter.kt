package com.clintoncochrane.bikecompanion.ui.garage

import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.ride.RideEntity
import com.clintoncochrane.bikecompanion.util.componentHealthPercent
import com.clintoncochrane.bikecompanion.util.DisplayFormatHelper

private const val RECENT_RIDES_LIMIT = 5

/** The advisory maintenance state shown for the currently displayed bike. */
enum class GarageBikeStatus {
    ReadyToRide,
    InspectSoon,
    ServiceDue,
}

data class GarageBikeStatusSummary(
    val level: GarageBikeStatus = GarageBikeStatus.ReadyToRide,
    val affectedComponentNames: List<String> = emptyList(),
)

/** Immutable state required to render the one-bike-at-a-time Garage overview. */
data class GarageBikesUiState(
    val bikes: List<BikeEntity> = emptyList(),
    val selectedBikeId: Long? = null,
    val selectedBikeIndex: Int = 0,
    val status: GarageBikeStatusSummary = GarageBikeStatusSummary(),
    val recentRides: List<RideEntity> = emptyList(),
) {
    val selectedBike: BikeEntity?
        get() = bikes.getOrNull(selectedBikeIndex)
}

/** Maps repository data into the Garage Bikes presentation without recalculating maintenance rules. */
object GarageBikesPresenter {

    fun build(
        bikes: List<BikeEntity>,
        components: List<ComponentEntity>,
        rides: List<RideEntity>,
        closeToServiceThreshold: Int,
        selectedBikeId: Long?,
    ): GarageBikesUiState {
        val selectedIndex = bikes.indexOfFirst { it.id == selectedBikeId }
            .takeIf { it >= 0 }
            ?: 0
        val selectedBike = bikes.getOrNull(selectedIndex)
        val selectedComponents = selectedBike?.let { bike ->
            components.filter { it.bikeId == bike.id }
        }.orEmpty()

        return GarageBikesUiState(
            bikes = bikes,
            selectedBikeId = selectedBike?.id,
            selectedBikeIndex = selectedIndex,
            status = statusFor(selectedComponents, closeToServiceThreshold),
            recentRides = selectedBike?.let { bike ->
                rides.asSequence()
                    .filter { it.bikeId == bike.id }
                    .sortedWith(compareByDescending<RideEntity> { it.endedAt }.thenByDescending { it.id })
                    .take(RECENT_RIDES_LIMIT)
                    .toList()
            }.orEmpty(),
        )
    }

    private fun statusFor(
        components: List<ComponentEntity>,
        closeToServiceThreshold: Int,
    ): GarageBikeStatusSummary {
        val knownHealth = components.mapNotNull { component ->
            componentHealthPercent(component)?.let { health -> component to health }
        }
        val dueComponents = knownHealth.filter { (_, health) -> health == 0 }.map { (component, _) ->
            DisplayFormatHelper.componentLabels(component.name, component.make, component.model, component.type).primary
        }
        if (dueComponents.isNotEmpty()) {
            return GarageBikeStatusSummary(GarageBikeStatus.ServiceDue, dueComponents)
        }

        val inspectComponents = knownHealth
            .filter { (_, health) -> health <= closeToServiceThreshold }
            .map { (component, _) ->
                DisplayFormatHelper.componentLabels(component.name, component.make, component.model, component.type).primary
            }
        return if (inspectComponents.isNotEmpty()) {
            GarageBikeStatusSummary(GarageBikeStatus.InspectSoon, inspectComponents)
        } else {
            GarageBikeStatusSummary()
        }
    }
}
