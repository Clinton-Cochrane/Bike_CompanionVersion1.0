package com.clintoncochrane.bikecompanion.ui.garage

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test

class ComposeClickableRippleCompatibilityTest {
    @Test
    fun materialThemeClickable_composesWithoutIndicationCompatibilityCrash() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, ComponentActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        ) as ComponentActivity

        try {
            instrumentation.runOnMainSync {
                activity.setContent { ClickableContent() }
            }
            instrumentation.waitForIdleSync()

            assertNotNull(activity.window.decorView)
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    @Composable
    private fun ClickableContent() {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .clickable(onClick = {}),
            )
        }
    }
}
