package com.you.bikecompanion.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.you.bikecompanion.R
import com.you.bikecompanion.ui.ride.ActiveRideActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single boundary for location intake during an active ride.
 * Location is used only to compute distance and elevation; coordinates are not logged or persisted.
 * See PRIVACY.md for retention.
 */
class RideTrackingService : Service() {

    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private val _rideState = MutableStateFlow(RideState())
    val rideState: StateFlow<RideState> = _rideState.asStateFlow()

    private var lastLatLng: Pair<Double, Double>? = null
    private var lastAltitude: Double = 0.0

    inner class LocalBinder : Binder() {
        fun getService(): RideTrackingService = this@RideTrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra(ACTION_KEY)) {
            ACTION_START -> {
                val bikeId = intent.getLongExtra(BIKE_ID_KEY, -1L)
                if (bikeId >= 0) startTracking(bikeId)
            }
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking(bikeId: Long) {
        createNotificationChannel()
        rideActiveBikeId.value = bikeId
        startForeground(NOTIFICATION_ID, createNotification(false))
        _rideState.value = _rideState.value.copy(
            bikeId = bikeId,
            isTracking = true,
            isPaused = false,
            startTimeMs = System.currentTimeMillis(),
        )
        requestLocationUpdates()
    }

    private fun pauseTracking() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        _rideState.value = _rideState.value.copy(isPaused = true)
        updateNotification()
    }

    private fun resumeTracking() {
        _rideState.value = _rideState.value.copy(isPaused = false)
        requestLocationUpdates()
        updateNotification()
    }

    private fun stopTracking() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        rideActiveBikeId.value = -1L
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS).apply {
            setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val lat = location.latitude
                    val lon = location.longitude
                    val alt = location.altitude
                    lastLatLng?.let { (prevLat, prevLon) ->
                        val distanceM = haversineM(prevLat, prevLon, lat, lon)
                        if (distanceM in 1.0..500.0) {
                            val distanceKm = distanceM / 1000.0
                            val speedKmh = location.speed * 3.6
                            _rideState.value = _rideState.value.copy(
                                distanceKm = _rideState.value.distanceKm + distanceKm,
                                currentSpeedKmh = speedKmh.coerceAtLeast(0.0),
                                maxSpeedKmh = maxOf(_rideState.value.maxSpeedKmh, speedKmh.coerceAtLeast(0.0)),
                                elevGainM = _rideState.value.elevGainM + (alt - lastAltitude).coerceAtLeast(0.0),
                                elevLossM = _rideState.value.elevLossM + (lastAltitude - alt).coerceAtLeast(0.0),
                            )
                            val n = _rideState.value.locationUpdateCount + 1
                            val prevAvg = _rideState.value.avgSpeedKmh
                            _rideState.value = _rideState.value.copy(
                                avgSpeedKmh = prevAvg + (speedKmh.coerceAtLeast(0.0) - prevAvg) / n,
                                locationUpdateCount = n,
                            )
                        }
                    }
                    lastLatLng = lat to lon
                    lastAltitude = alt
                }
            }
        }
        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback!!,
                Looper.getMainLooper(),
            )
        } catch (_: SecurityException) { }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun createNotification(isPaused: Boolean): Notification {
        val bikeId = _rideState.value.bikeId
        val openIntent = Intent(this, ActiveRideActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(ActiveRideActivity.BIKE_ID_EXTRA, bikeId)
        }
        val contentIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val pauseResumeAction = if (isPaused) {
            createActionIntent(ACTION_RESUME, getString(R.string.ride_resume))
        } else {
            createActionIntent(ACTION_PAUSE, getString(R.string.ride_pause))
        }
        val stopAction = createActionIntent(ACTION_STOP, getString(R.string.ride_stop))
        val state = _rideState.value
        val distanceText = "%.2f km".format(state.distanceKm)
        val statusText = if (isPaused) getString(R.string.ride_paused) else getString(R.string.ride_active_title)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("$statusText • $distanceText")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(contentIntent)
            .addAction(pauseResumeAction.first, pauseResumeAction.second, pauseResumeAction.third)
            .addAction(stopAction.first, stopAction.second, stopAction.third)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createActionIntent(action: String, title: String): Triple<Int, CharSequence, PendingIntent> {
        val intent = Intent(this, RideTrackingService::class.java).apply {
            putExtra(ACTION_KEY, action)
        }
        val pendingIntent = PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Triple(android.R.drawable.ic_menu_mylocation, title, pendingIntent)
    }

    private fun updateNotification() {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, createNotification(_rideState.value.isPaused))
    }

    private fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }

    companion object {
        /** Exposed so UI can show "View current trip" when a ride is active. Updated by service. */
        val rideActiveBikeId = MutableStateFlow(-1L)

        private const val CHANNEL_ID = "ride_tracking"
        private const val NOTIFICATION_ID = 4001
        private const val UPDATE_INTERVAL_MS = 5000L
        private const val FASTEST_INTERVAL_MS = 2500L

        const val ACTION_KEY = "action"
        const val BIKE_ID_KEY = "bike_id"
        const val ACTION_START = "start"
        const val ACTION_PAUSE = "pause"
        const val ACTION_RESUME = "resume"
        const val ACTION_STOP = "stop"
    }
}

data class RideState(
    val bikeId: Long = -1L,
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val startTimeMs: Long = 0L,
    val distanceKm: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val elevGainM: Double = 0.0,
    val elevLossM: Double = 0.0,
    val locationUpdateCount: Int = 0,
)
