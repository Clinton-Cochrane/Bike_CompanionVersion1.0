package com.clintoncochrane.bikecompanion.ui.garage

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.clintoncochrane.bikecompanion.R
import kotlin.math.roundToInt

internal const val GARAGE_SERVICE_SHEET_TAG = "garage_service_sheet"

@Composable
internal fun GarageServiceSheetHost(
    state: GarageServiceSheetState,
    onDismiss: () -> Unit,
    onToggle: (Long) -> Unit,
    onShowConfirmation: () -> Unit,
    onShowChecklist: () -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onLiftChanged: (Float) -> Unit,
    navigationBarClearance: Dp = 0.dp,
) {
    var sheetHeightPx by remember { mutableIntStateOf(0) }
    val swipeDismissThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }
    val swipeDownToDismiss = remember(state.isSubmitting, swipeDismissThresholdPx, onDismiss) {
        object : NestedScrollConnection {
            private var downwardDragPx = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (state.isSubmitting || source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y <= 0f) {
                    downwardDragPx = 0f
                    return Offset.Zero
                }

                downwardDragPx += available.y
                if (downwardDragPx >= swipeDismissThresholdPx) {
                    downwardDragPx = 0f
                    onDismiss()
                }
                return Offset(0f, available.y)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                downwardDragPx = 0f
                return Velocity.Zero
            }
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (state.isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "garageServiceSheetProgress",
    )

    LaunchedEffect(progress, sheetHeightPx) {
        onLiftChanged(progress * sheetHeightPx)
    }
    DisposableEffect(Unit) {
        onDispose { onLiftChanged(0f) }
    }
    BackHandler(enabled = state.isVisible && !state.isSubmitting, onBack = onDismiss)

    if (state.isVisible || progress > 0f) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(progress * 0.32f)
                    .background(Color.Black)
                    .clickable(
                        enabled = state.isVisible && !state.isSubmitting,
                        onClick = onDismiss,
                    ),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .nestedScroll(swipeDownToDismiss)
                    .onSizeChanged { sheetHeightPx = it.height }
                    .then(
                        Modifier.offset {
                            IntOffset(0, ((1f - progress) * sheetHeightPx).roundToInt())
                        },
                    )
                    .testTag(GARAGE_SERVICE_SHEET_TAG),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 24.dp,
                            top = navigationBarClearance + 20.dp,
                            end = 24.dp,
                            bottom = 20.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (state.step) {
                        GarageServiceSheetStep.CHECKLIST -> ServiceChecklist(
                            state = state,
                            onToggle = onToggle,
                            onContinue = onShowConfirmation,
                        )
                        GarageServiceSheetStep.CONFIRMATION -> ServiceConfirmation(
                            state = state,
                            onBack = onShowChecklist,
                            onConfirm = onConfirm,
                        )
                        GarageServiceSheetStep.RESULT -> ServiceResult(
                            state = state,
                            onRetry = onRetry,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceChecklist(
    state: GarageServiceSheetState,
    onToggle: (Long) -> Unit,
    onContinue: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.garage_service_sheet_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Button(
            onClick = onContinue,
            enabled = state.canCompleteSelected,
        ) {
            Text(stringResource(R.string.garage_service_complete_selected))
        }
    }
    state.requirements.forEach { requirement ->
        val checked = requirement.intervalId in state.selectedIntervalIds
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    enabled = !state.isSubmitting,
                    onValueChange = { onToggle(requirement.intervalId) },
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                enabled = !state.isSubmitting,
            )
            Text(
                text = stringResource(
                    R.string.garage_service_requirement_description,
                    requirement.serviceName,
                    requirement.componentLabel,
                ),
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ServiceConfirmation(
    state: GarageServiceSheetState,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    Text(stringResource(R.string.garage_service_confirmation_title), style = MaterialTheme.typography.headlineSmall)
    state.selectedRequirements.forEach { requirement ->
        Column {
            Text(
                text = stringResource(
                    R.string.garage_service_bullet_requirement,
                    requirement.serviceName,
                    requirement.componentLabel,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(
                    if (requirement.isReplacement) {
                        R.string.garage_service_replacement_confirmation
                    } else {
                        R.string.garage_service_normal_confirmation
                    },
                ),
                modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("garage_service_confirmation_actions"),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack, enabled = !state.isSubmitting) {
            Text(stringResource(R.string.common_back))
        }
        Button(onClick = onConfirm, enabled = !state.isSubmitting) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.garage_service_confirm))
            }
        }
    }
}

@Composable
private fun ServiceResult(
    state: GarageServiceSheetState,
    onRetry: () -> Unit,
) {
    Text(
        pluralStringResource(
            R.plurals.garage_service_completed_count,
            state.completedCount,
            state.completedCount,
        ),
        style = MaterialTheme.typography.headlineSmall,
    )
    Text(stringResource(R.string.garage_service_could_not_complete), style = MaterialTheme.typography.titleMedium)
    state.failedRequirements.forEach { requirement ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = stringResource(
                    R.string.garage_service_requirement_description,
                    requirement.serviceName,
                    requirement.componentLabel,
                ),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Button(
            onClick = onRetry,
            enabled = !state.isSubmitting,
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.garage_service_try_again))
            }
        }
    }
}
