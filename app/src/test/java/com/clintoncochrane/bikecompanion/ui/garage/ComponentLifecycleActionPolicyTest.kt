package com.clintoncochrane.bikecompanion.ui.garage

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentLifecycleActionPolicyTest {

    @Test
    fun installedComponentMenu_offersMoveToGarageAndPermanentDeleteWithoutRetire() {
        val source = bikeDetailSource()

        assertTrue(source.contains("R.string.component_move_to_garage"))
        assertTrue(source.contains("componentForDeleteConfirm = component"))
        assertFalse(source.contains("componentForRemoveDialog"))
        assertFalse(Regex("""R\.string\.component_retire\b""").containsMatchIn(source))
    }

    @Test
    fun deleteAction_waitsForConfirmationAndCancelLeavesComponentUntouched() {
        val source = bikeDetailSource()
        val dialogStart = source.indexOf("val componentToDeleteConfirm = componentForDeleteConfirm")
        val dialogEnd = source.indexOf("Scaffold(", startIndex = dialogStart)
        val dialogSource = source.substring(dialogStart, dialogEnd)

        assertTrue(dialogSource.contains("onDismissRequest = { componentForDeleteConfirm = null }"))
        assertTrue(dialogSource.contains("viewModel.deleteComponent(componentToDeleteConfirm)"))
        assertTrue(dialogSource.contains("Text(stringResource(R.string.common_delete)"))
    }

    @Test
    fun componentDetail_actionsDoNotExposeRetire() {
        val source = File("src/main/java/com/clintoncochrane/bikecompanion/ui/garage/ComponentDetailScreen.kt")
            .readText()

        assertTrue(source.contains("R.string.component_move_to_garage"))
        assertTrue(source.contains("R.string.component_delete"))
        assertFalse(source.contains("showRetireConfirm"))
        assertFalse(Regex("""R\.string\.component_retire\b""").containsMatchIn(source))
    }

    @Test
    fun garageMenu_doesNotExposeWallOfHonorEntryPoint() {
        val source = File("src/main/java/com/clintoncochrane/bikecompanion/ui/garage/GarageScreen.kt")
            .readText()

        assertFalse(source.contains("Screen.WallOfHonor.route"))
        assertFalse(source.contains("R.string.wall_of_honor_title"))
    }

    @Test
    fun replacementWorkflow_remainsWiredToExistingReplacementPath() {
        val source = bikeDetailSource()

        assertTrue(source.contains("R.string.bike_component_replace"))
        assertTrue(source.contains("viewModel.replaceComponent(component, replacement)"))
    }

    private fun bikeDetailSource(): String =
        File("src/main/java/com/clintoncochrane/bikecompanion/ui/garage/BikeDetailScreen.kt").readText()
}
