package com.clintoncochrane.bikecompanion.ui.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Displays a circular avatar placeholder.
 *
 * @param size Diameter of the circle.
 * @param placeholder Composable for when no image (e.g. letter, icon).
 */
@Composable
fun ThumbnailAvatar(
    size: Dp,
    modifier: Modifier = Modifier,
    placeholder: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Filled.DirectionsBike,
            contentDescription = null,
            modifier = Modifier.size(size * 0.6f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    },
) {
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        placeholder()
    }
}
