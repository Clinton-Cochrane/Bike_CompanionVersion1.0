package com.clintoncochrane.bikecompanion.ui.garage

import com.clintoncochrane.bikecompanion.data.bike.BikeEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.data.component.ComponentLifecycleStatus
import com.clintoncochrane.bikecompanion.data.component.ComponentSwapEntity
import com.clintoncochrane.bikecompanion.data.component.PriorUsageCertainty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PartsDirectoryPresenterTest {

    private val rory = BikeEntity(id = 1L, name = "Rory", createdAt = 1L)
    private val willow = BikeEntity(id = 2L, name = "Willow", createdAt = 2L)
    private val active = component(
        id = 10L,
        bikeId = rory.id,
        status = ComponentLifecycleStatus.INSTALLED,
        type = "chain",
        name = "Everyday Chain",
    )
    private val withoutBike = component(
        id = 20L,
        bikeId = null,
        status = ComponentLifecycleStatus.IN_GARAGE,
        type = "cassette",
        name = "Spare Cassette",
    )
    private val retiredWithHistory = component(
        id = 30L,
        bikeId = null,
        status = ComponentLifecycleStatus.RETIRED,
        type = "chain",
        name = "Old Chain",
    )
    private val retiredWithoutHistory = component(
        id = 40L,
        bikeId = null,
        status = ComponentLifecycleStatus.RETIRED,
        type = "tire",
        name = "Old Tire",
    )
    private val swaps = listOf(
        ComponentSwapEntity(componentId = retiredWithHistory.id, bikeId = rory.id, installedAt = 100L, uninstalledAt = 200L),
        ComponentSwapEntity(componentId = retiredWithHistory.id, bikeId = willow.id, installedAt = 300L, uninstalledAt = 400L),
    )
    private val allComponents = listOf(active, withoutBike, retiredWithHistory, retiredWithoutHistory)

    @Test
    fun defaultFilter_isAllParts() {
        assertEquals(PartsDirectoryFilter.ALL_PARTS, PartsDirectoryUiState().filter)
    }

    @Test
    fun emptyDirectory_hasNoSections() {
        val sections = PartsDirectoryPresenter.build(
            components = emptyList(),
            bikes = emptyList(),
            swaps = emptyList(),
            filter = PartsDirectoryFilter.ALL_PARTS,
        )

        assertTrue(sections.isEmpty())
    }

    @Test
    fun allParts_includesActiveUnassignedAndRetiredComponents() {
        val ids = build(filter = PartsDirectoryFilter.ALL_PARTS).flatMap { section -> section.rows }.map { it.componentId }

        assertEquals(setOf(10L, 20L, 30L, 40L), ids.toSet())
    }

    @Test
    fun filters_areMutuallyExclusiveAndUseLifecycleDefinitions() {
        assertEquals(listOf(10L), idsFor(PartsDirectoryFilter.ACTIVE))
        assertEquals(listOf(30L, 40L), idsFor(PartsDirectoryFilter.RETIRED))
        assertEquals(listOf(20L), idsFor(PartsDirectoryFilter.WITHOUT_BIKES))
    }

    @Test
    fun sectionsAndRows_areAlphabetizedCaseInsensitivelyAndEmptySectionsAreOmitted() {
        val components = listOf(
            component(1L, null, ComponentLifecycleStatus.IN_GARAGE, "tire", "zulu"),
            component(2L, null, ComponentLifecycleStatus.IN_GARAGE, "Chain", "beta"),
            component(3L, null, ComponentLifecycleStatus.IN_GARAGE, "chain", "Alpha"),
            component(4L, null, ComponentLifecycleStatus.RETIRED, "brake_pad", "hidden"),
        )

        val sections = PartsDirectoryPresenter.build(
            components = components,
            bikes = emptyList(),
            swaps = emptyList(),
            filter = PartsDirectoryFilter.WITHOUT_BIKES,
        )

        assertEquals(listOf("Chain", "Tire"), sections.map { it.typeHeading })
        assertEquals(listOf("Alpha", "beta"), sections.first().rows.map { it.displayName })
        assertTrue(sections.none { it.typeHeading == "Brake Pad" })
    }

    @Test
    fun rows_mapCurrentNoBikeAndRetiredAssociationStates() {
        val rows = build().flatMap { it.rows }.associateBy { it.componentId }

        assertEquals(PartAssociation.CurrentBike("Rory"), rows.getValue(active.id).association)
        assertEquals(PartAssociation.NoBike, rows.getValue(withoutBike.id).association)
        assertEquals(PartAssociation.LastBike("Willow"), rows.getValue(retiredWithHistory.id).association)
        assertEquals(PartAssociation.Retired, rows.getValue(retiredWithoutHistory.id).association)
    }

    @Test
    fun retiredComponentWithDeletedMostRecentBike_doesNotInventAnOlderAssociation() {
        val deletedBikeSwap = ComponentSwapEntity(
            componentId = retiredWithHistory.id,
            bikeId = null,
            installedAt = 500L,
            uninstalledAt = 600L,
        )

        val row = build(swaps = swaps + deletedBikeSwap)
            .flatMap { it.rows }
            .single { it.componentId == retiredWithHistory.id }

        assertEquals(PartAssociation.Retired, row.association)
    }

    @Test
    fun rowUsesStableComponentIdAndPersistedLifetimeDistance() {
        val movedComponent = component(
            id = 77L,
            bikeId = willow.id,
            status = ComponentLifecycleStatus.INSTALLED,
            type = "wheel",
            name = "Touring Wheel",
            distanceUsedKm = 120.0,
            baselineKm = 30.0,
            priorUsageCertainty = PriorUsageCertainty.KNOWN,
        )

        val row = PartsDirectoryPresenter.build(
            components = listOf(movedComponent),
            bikes = listOf(rory, willow),
            swaps = listOf(
                ComponentSwapEntity(componentId = 77L, bikeId = rory.id, installedAt = 100L, uninstalledAt = 200L),
                ComponentSwapEntity(componentId = 77L, bikeId = willow.id, installedAt = 300L),
            ),
            filter = PartsDirectoryFilter.ALL_PARTS,
        ).single().rows.single()

        assertEquals(77L, row.componentId)
        assertEquals(150.0, row.lifetimeDistanceKm, 0.001)
        assertEquals(false, row.isTrackedDistanceOnly)
    }

    private fun idsFor(filter: PartsDirectoryFilter): List<Long> =
        build(filter).flatMap { it.rows }.map { it.componentId }.sorted()

    private fun build(
        filter: PartsDirectoryFilter = PartsDirectoryFilter.ALL_PARTS,
        swaps: List<ComponentSwapEntity> = this.swaps,
    ) = PartsDirectoryPresenter.build(
        components = allComponents,
        bikes = listOf(rory, willow),
        swaps = swaps,
        filter = filter,
    )

    private fun component(
        id: Long,
        bikeId: Long?,
        status: ComponentLifecycleStatus,
        type: String,
        name: String,
        distanceUsedKm: Double = 0.0,
        baselineKm: Double = 0.0,
        priorUsageCertainty: PriorUsageCertainty = PriorUsageCertainty.UNKNOWN,
    ) = ComponentEntity(
        id = id,
        bikeId = bikeId,
        lifecycleStatus = status,
        type = type,
        name = name,
        lifespanKm = 1_000.0,
        distanceUsedKm = distanceUsedKm,
        baselineKm = baselineKm,
        priorUsageCertainty = priorUsageCertainty,
        installedAt = 1L,
    )
}
