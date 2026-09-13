package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.clintoncochrane.bikecompanion.R
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.util.componentHealthPercent

@Composable
internal fun ComponentHealthSummary(
    component: ComponentEntity,
    modifier: Modifier = Modifier,
) {
    val healthPercent = componentHealthPercent(component)
    Column(modifier = modifier) {
        if (healthPercent == null) {
            Text(
                text = stringResource(R.string.component_tracked_distance, component.distanceUsedKm),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.component_health_unavailable),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val healthDescription = stringResource(R.string.bike_component_health, healthPercent)
            Text(
                text = stringResource(
                    R.string.bike_component_used_km,
                    component.lifetimeDistanceKm,
                    component.lifespanKm,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            LinearProgressIndicator(
                progress = { healthPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .semantics { contentDescription = healthDescription },
            )
            Text(
                text = healthDescription,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
