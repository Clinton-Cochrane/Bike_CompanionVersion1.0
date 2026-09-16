package com.clintoncochrane.bikecompanion.ui.trip

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripScreenPluralResourceTest {

    @Test
    fun rideHistoryTotal_usesPluralStringResource() {
        val tripScreen = File(
            "src/main/java/com/clintoncochrane/bikecompanion/ui/trip/TripScreen.kt",
        ).readText()

        assertTrue(tripScreen.contains("import androidx.compose.ui.res.pluralStringResource"))
        assertTrue(
            Regex("""pluralStringResource\(\s*R\.plurals\.ride_history_total""")
                .containsMatchIn(tripScreen),
        )
        assertFalse(
            Regex("""(?<!plural)stringResource\(\s*R\.plurals\.ride_history_total""")
                .containsMatchIn(tripScreen),
        )
    }
}
