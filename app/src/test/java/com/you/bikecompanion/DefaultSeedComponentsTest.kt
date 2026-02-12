package com.you.bikecompanion

import com.you.bikecompanion.data.component.ComponentCategory
import com.you.bikecompanion.data.component.DefaultSeedComponents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the default seed list is non-empty, each entry has required fields,
 * and all entries belong to [ComponentCategory.defaultBikeCategories].
 * Idempotency is covered by [ComponentSeedingIdempotencyTest].
 */
class DefaultSeedComponentsTest {

    @Test
    fun list_hasExpectedComponents() {
        val list = DefaultSeedComponents.LIST
        assertTrue("Default seed list should not be empty", list.isNotEmpty())
        assertEquals(
            "Default seed count: cockpit, frame (incl. fork), drivetrain, wheels (hub/spokes/tire/tube/rim), brakes (caliper/pads/rotor), cables",
            43,
            list.size,
        )
    }

    @Test
    fun allSeedComponents_belongToDefaultBikeCategories() {
        val defaultCategories = ComponentCategory.defaultBikeCategories.toSet()
        for (entry in DefaultSeedComponents.LIST) {
            val category = ComponentCategory.fromComponentType(entry.type)
            assertTrue(
                "Seed component ${entry.type} maps to $category which is not in defaultBikeCategories",
                category in defaultCategories,
            )
        }
    }

    @Test
    fun eachEntry_hasRequiredFields() {
        val validPositions = setOf("none", "front", "rear")
        for (entry in DefaultSeedComponents.LIST) {
            assertTrue("type should be non-blank: $entry", entry.type.isNotBlank())
            assertTrue("name should be non-blank: $entry", entry.name.isNotBlank())
            assertTrue(
                "position should be none/front/rear: ${entry.position}",
                entry.position in validPositions,
            )
            assertTrue("lifespan should be positive: $entry", entry.defaultLifespanKm > 0)
        }
    }

    @Test
    fun seedListFor_singleSpeed_excludesFrontDerailleurCassetteAndRearDerailleur() {
        val list = DefaultSeedComponents.seedListFor("single_speed", "rim")
        val types = list.map { it.type }.toSet()
        assertTrue("single_speed should not seed front_derailleur", "front_derailleur" !in types)
        assertTrue("single_speed should not seed rear_derailleur", "rear_derailleur" !in types)
        assertTrue("single_speed should not seed cassette", "cassette" !in types)
        assertTrue("single_speed should still seed chain", "chain" in types)
    }

    @Test
    fun seedListFor_1x_excludesOnlyFrontDerailleur() {
        val list = DefaultSeedComponents.seedListFor("1x", "disc_mechanical")
        val types = list.map { it.type }.toSet()
        assertTrue("1x should not seed front_derailleur", "front_derailleur" !in types)
        assertTrue("1x should seed cassette", "cassette" in types)
        assertTrue("1x should seed rear_derailleur", "rear_derailleur" in types)
    }

    @Test
    fun seedListFor_discHydraulic_excludesBrakeCables() {
        val list = DefaultSeedComponents.seedListFor("multi_speed", "disc_hydraulic")
        val types = list.map { it.type }.toSet()
        assertTrue("disc_hydraulic should not seed cable_front_brake", "cable_front_brake" !in types)
        assertTrue("disc_hydraulic should not seed cable_rear_brake", "cable_rear_brake" !in types)
    }

    @Test
    fun seedListFor_coaster_excludesBrakePartsAndCables() {
        val list = DefaultSeedComponents.seedListFor("single_speed", "coaster")
        val types = list.map { it.type }.toSet()
        assertTrue("coaster should not seed brake_caliper", "brake_caliper" !in types)
        assertTrue("coaster should not seed brake_pads", "brake_pads" !in types)
        assertTrue("coaster should not seed brake_rotor", "brake_rotor" !in types)
        assertTrue("coaster should not seed cable_front_brake", "cable_front_brake" !in types)
        assertTrue("coaster should not seed cable_rear_brake", "cable_rear_brake" !in types)
    }
}
