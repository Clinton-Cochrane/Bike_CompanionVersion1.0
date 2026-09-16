package com.clintoncochrane.bikecompanion.ui.garage

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentEditNavigationPolicyTest {

    @Test
    fun componentEditRoute_opensExistingEditorOnLaunch() {
        val navigationSource = File("src/main/java/com/clintoncochrane/bikecompanion/ui/navigation/BikeCompanionNavGraph.kt")
            .readText()

        assertTrue(navigationSource.contains("data object EditComponent : Screen(\"edit_component/{componentId}\")"))
        assertTrue(navigationSource.contains("composable(Screen.EditComponent.route)"))
        assertTrue(navigationSource.contains("openEditorOnLaunch = true"))
    }

    @Test
    fun componentEditor_launchesOnceAndNormalDetailDoesNotOpenIt() {
        val detailSource = File("src/main/java/com/clintoncochrane/bikecompanion/ui/garage/ComponentDetailScreen.kt")
            .readText()

        assertTrue(detailSource.contains("openEditorOnLaunch: Boolean = false"))
        assertTrue(detailSource.contains("rememberSaveable { mutableStateOf(openEditorOnLaunch) }"))
        assertTrue(detailSource.contains("if (showComponentEdit && uiState.component != null)"))
        assertTrue(detailSource.contains("viewModel.updateComponent("))
        assertTrue(detailSource.contains("showComponentEdit = false"))
    }

    @Test
    fun componentContextMenuEdit_usesEditRouteInsteadOfDetailRoute() {
        val bikeDetailSource = File("src/main/java/com/clintoncochrane/bikecompanion/ui/garage/BikeDetailScreen.kt")
            .readText()

        assertTrue(bikeDetailSource.contains("onEdit = { navController.navigate(Screen.EditComponent.withId(it.id)) }"))
        assertTrue(bikeDetailSource.contains("onViewDetails = { navController.navigate(Screen.ComponentDetail.withId(it.id)) }"))
        assertTrue(bikeDetailSource.contains("onEdit()\n                                    onContextMenuDismiss(true)"))
        assertFalse(bikeDetailSource.contains("onClick = {\n                                    onViewDetails()\n                                    onContextMenuDismiss(true)\n                                },"))
    }
}
