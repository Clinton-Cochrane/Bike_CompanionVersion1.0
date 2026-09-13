package com.clintoncochrane.bikecompanion.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.clintoncochrane.bikecompanion.R
import com.clintoncochrane.bikecompanion.data.component.ComponentDao
import com.clintoncochrane.bikecompanion.data.component.ComponentEntity
import com.clintoncochrane.bikecompanion.util.DisplayFormatHelper
import com.clintoncochrane.bikecompanion.util.componentHealthPercent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Checks component health for a bike and posts a notification if any component
 * is past its alert threshold (and not snoozed, alerts enabled).
 */
class ComponentAlertNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val componentDao: ComponentDao,
) {
    suspend fun notifyIfNeeded(bikeId: Long) = withContext(Dispatchers.IO) {
        val components = componentDao.getComponentsByBikeIdOnce(bikeId)
        val nowMillis = System.currentTimeMillis()
        val needAlert = components.filter { component ->
            shouldNotifyForComponent(component, nowMillis)
        }
        if (needAlert.isEmpty()) return@withContext
        ensureChannel()
        val title = context.getString(R.string.garage_components_attention, needAlert.size)
        val text = needAlert.joinToString { DisplayFormatHelper.formatForDisplay(it.name) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.garage_components),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "component_alerts"
        private const val NOTIFICATION_ID = 4002
    }
}

internal fun shouldNotifyForComponent(component: ComponentEntity, nowMillis: Long): Boolean {
    if (!component.alertsEnabled) return false
    if (component.alertSnoozeUntilKm?.let { component.lifetimeDistanceKm < it } == true) return false
    if (component.alertSnoozeUntilTime?.let { nowMillis < it } == true) return false
    return componentHealthPercent(component)?.let { health ->
        health <= component.alertThresholdPercent
    } == true
}
