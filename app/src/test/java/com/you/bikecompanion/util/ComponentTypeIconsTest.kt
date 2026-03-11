package com.you.bikecompanion.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Unit tests for [componentTypeIcon].
 */
class ComponentTypeIconsTest {

    @Test
    fun componentTypeIcon_returnsNonNull_forKnownTypes() {
        val types = listOf(
            "chain", "cassette", "tire", "brake_pads", "frame", "handlebars",
            "battery", "tubeless_sealant", "dropper_post", "suspension_pivots",
        )
        types.forEach { type ->
            val icon = componentTypeIcon(type)
            assertNotNull("Icon for type '$type' should not be null", icon)
        }
    }

    @Test
    fun componentTypeIcon_chain_returnsLink() {
        assertSame(Icons.Filled.Link, componentTypeIcon("chain"))
    }

    @Test
    fun componentTypeIcon_tubelessSealant_returnsWaterDrop() {
        assertSame(Icons.Filled.WaterDrop, componentTypeIcon("tubeless_sealant"))
    }

    @Test
    fun componentTypeIcon_suspensionPivots_returnsBuild() {
        assertSame(Icons.Filled.Build, componentTypeIcon("suspension_pivots"))
    }

    @Test
    fun componentTypeIcon_unknownType_returnsBuild() {
        val icon: ImageVector = componentTypeIcon("unknown_type_xyz")
        assertNotNull(icon)
        assertSame(Icons.Filled.Build, icon)
    }
}
