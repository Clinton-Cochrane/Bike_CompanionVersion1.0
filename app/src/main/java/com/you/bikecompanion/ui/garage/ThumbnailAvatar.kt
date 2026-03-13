package com.you.bikecompanion.ui.garage

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import java.io.File

/**
 * Displays a circular avatar: image from [thumbnailUri] if present, otherwise [placeholder].
 *
 * @param thumbnailUri File path or content URI for the image; null shows placeholder.
 * @param size Diameter of the circle.
 * @param placeholder Composable for when no image (e.g. letter, icon).
 */
@Composable
fun ThumbnailAvatar(
    thumbnailUri: String?,
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
        when {
            thumbnailUri.isNullOrBlank() -> placeholder()
            else -> AsyncImage(
                model = File(thumbnailUri),
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
