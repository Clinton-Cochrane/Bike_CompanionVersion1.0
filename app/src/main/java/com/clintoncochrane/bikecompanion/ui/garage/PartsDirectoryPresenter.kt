package com.clintoncochrane.bikecompanion.ui.garage

import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentLifecycleStatus
import com.clintoncochrane.bikecompanion.data.component.ComponentSwapEntity
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import com.clintoncochrane.bikecompanion.util.DisplayFormatHelper
import java.util.Locale

enum class PartsDirectoryFilter {
    ALL_PARTS,
    ACTIVE,
    RETIRED,
    WITHOUT_BIKES,
}

data class PartsDirectoryUiState(
    val filter: PartsDirectoryFilter = PartsDirectoryFilter.ALL_PARTS,
    val sections: List<PartsDirectorySection> = emptyList(),
)

sealed interface PartAssociation {
    data class CurrentBike(val bikeName: String) : PartAssociation
    data class LastBike(val bikeName: String) : PartAssociation
    data object NoBike : PartAssociation
    data object Retired : PartAssociation
}

data class PartsDirectoryRow(
    val componentId: Long,
    val displayName: String,
    val secondaryLabel: String?,
    val typeKey: String,
    val association: PartAssociation,
    val isRetired: Boolean,
    val lifetimeDistanceKm: Double,
    val isTrackedDistanceOnly: Boolean,
)

data class PartsDirectorySection(
    val typeKey: String,
    val typeHeading: String,
    val rows: List<PartsDirectoryRow>,
)

/** Builds the complete, deterministic Parts directory outside the composable layer. */
object PartsDirectoryPresenter {

    fun build(
        components: List<ComponentEntity>,
        bikes: List<BikeEntity>,
        swaps: List<ComponentSwapEntity>,
        filter: PartsDirectoryFilter,
    ): List<PartsDirectorySection> {
        val bikesById = bikes.associateBy(BikeEntity::id)
        val latestSwapByComponentId = swaps
            .groupBy(ComponentSwapEntity::componentId)
            .mapValues { (_, componentSwaps) -> componentSwaps.maxByOrNull(ComponentSwapEntity::installedAt) }

        return components
            .asSequence()
            .filter { component -> component.matches(filter) }
            .map { component ->
                val typeKey = component.type.lowercase(Locale.ROOT)
                PartsDirectoryRow(
                    componentId = component.id,
                    displayName = DisplayFormatHelper.componentLabels(
                        component.name, component.make, component.model, component.type,
                    ).primary,
                    secondaryLabel = DisplayFormatHelper.componentLabels(
                        component.name, component.make, component.model, component.type,
                    ).secondary,
                    typeKey = typeKey,
                    association = associationFor(
                        component = component,
                        bikesById = bikesById,
                        latestSwap = latestSwapByComponentId[component.id],
                    ),
                    isRetired = component.lifecycleStatus == ComponentLifecycleStatus.RETIRED,
                    lifetimeDistanceKm = component.lifetimeDistanceKm,
                    isTrackedDistanceOnly = component.priorUsageCertainty == PriorUsageCertainty.UNKNOWN,
                )
            }
            .groupBy(PartsDirectoryRow::typeKey)
            .map { (typeKey, rows) ->
                PartsDirectorySection(
                    typeKey = typeKey,
                    typeHeading = DisplayFormatHelper.formatComponentTypeForDisplay(typeKey),
                    rows = rows.sortedWith(
                        compareBy<PartsDirectoryRow> { it.displayName.lowercase(Locale.ROOT) }
                            .thenBy(PartsDirectoryRow::componentId),
                    ),
                )
            }
            .sortedBy { it.typeHeading.lowercase(Locale.ROOT) }
    }

    private fun ComponentEntity.matches(filter: PartsDirectoryFilter): Boolean = when (filter) {
        PartsDirectoryFilter.ALL_PARTS -> true
        PartsDirectoryFilter.ACTIVE ->
            lifecycleStatus == ComponentLifecycleStatus.INSTALLED && bikeId != null
        PartsDirectoryFilter.RETIRED -> lifecycleStatus == ComponentLifecycleStatus.RETIRED
        PartsDirectoryFilter.WITHOUT_BIKES ->
            lifecycleStatus != ComponentLifecycleStatus.RETIRED && bikeId == null
    }

    private fun associationFor(
        component: ComponentEntity,
        bikesById: Map<Long, BikeEntity>,
        latestSwap: ComponentSwapEntity?,
    ): PartAssociation {
        if (component.lifecycleStatus == ComponentLifecycleStatus.RETIRED) {
            val lastBikeName = latestSwap?.bikeId?.let(bikesById::get)?.let {
                DisplayFormatHelper.bikeLabels(it.name, it.make, it.model).primary
            }
            return lastBikeName?.let(PartAssociation::LastBike) ?: PartAssociation.Retired
        }

        val currentBikeName = component.bikeId?.let(bikesById::get)?.let {
            DisplayFormatHelper.bikeLabels(it.name, it.make, it.model).primary
        }
        return currentBikeName?.let(PartAssociation::CurrentBike) ?: PartAssociation.NoBike
    }
}
