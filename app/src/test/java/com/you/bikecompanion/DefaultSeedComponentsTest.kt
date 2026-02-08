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
}
