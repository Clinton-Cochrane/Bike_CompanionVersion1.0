package com.you.bikecompanion.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Handler
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
    private var lastAltitude: Double? = null
    private var lastMovementTimeMs: Long = 0L
    private val noMovementCheckHandler = Handler(Looper.getMainLooper())
    private val noMovementCheckRunnable = object : Runnable {
        override fun run() {
            checkNoMovementAndAutoPause()
            noMovementCheckHandler.postDelayed(this, NO_MOVEMENT_CHECK_INTERVAL_MS)
        }
    }

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
            ACTION_PAUSE -> pauseTracking(wasAutoPause = false)
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
            ACTION_CLEAR_AUTO_PAUSE_FLAG -> clearAutoPauseFlag()
        }
        return START_STICKY
    }

    private fun startTracking(bikeId: Long) {
        createNotificationChannel()
        lastLatLng = null
        lastAltitude = null
        val now = System.currentTimeMillis()
        lastMovementTimeMs = now
        _rideState.value = _rideState.value.copy(
            bikeId = bikeId,
            isTracking = true,
            isPaused = false,
            wasAutoPausedDueToNoMovement = false,
            startTimeMs = now,
        )
        rideActiveBikeId.value = bikeId
        startForeground(NOTIFICATION_ID, createNotification(false))
        requestLocationUpdates()
        scheduleNoMovementCheck()
    }

    private fun pauseTracking(wasAutoPause: Boolean = false) {
        noMovementCheckHandler.removeCallbacks(noMovementCheckRunnable)
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        _rideState.value = _rideState.value.copy(
            isPaused = true,
            wasAutoPausedDueToNoMovement = wasAutoPause,
        )
        updateNotification()
    }

    private fun resumeTracking() {
        lastMovementTimeMs = System.currentTimeMillis()
        _rideState.value = _rideState.value.copy(
            isPaused = false,
            wasAutoPausedDueToNoMovement = false,
        )
        requestLocationUpdates()
        updateNotification()
        scheduleNoMovementCheck()
    }

    private fun stopTracking() {
        noMovementCheckHandler.removeCallbacks(noMovementCheckRunnable)
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        rideActiveBikeId.value = -1L
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS).apply {
            setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_M)
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    if (!isLocationAcceptable(location)) return@let

                    val lat = location.latitude
                    val lon = location.longitude
                    val alt = location.altitude
                    val now = System.currentTimeMillis()

                    lastLatLng?.let { (prevLat, prevLon) ->
                        val distanceM = haversineM(prevLat, prevLon, lat, lon)
                        if (distanceM in MIN_MOVEMENT_M..MAX_MOVEMENT_PER_UPDATE_M) {
                            lastMovementTimeMs = now
                            val distanceKm = distanceM / 1000.0
                            val speedKmh = location.speed * 3.6
                            val prevAlt = lastAltitude
                            if (prevAlt != null) {
                                val elevDelta = alt - prevAlt
                                val cappedGain = elevDelta.coerceIn(0.0, MAX_ELEV_CHANGE_PER_UPDATE_M)
                                val cappedLoss = (-elevDelta).coerceIn(0.0, MAX_ELEV_CHANGE_PER_UPDATE_M)
                                _rideState.value = _rideState.value.copy(
                                    distanceKm = _rideState.value.distanceKm + distanceKm,
                                    currentSpeedKmh = speedKmh.coerceAtLeast(0.0),
                                    maxSpeedKmh = maxOf(_rideState.value.maxSpeedKmh, speedKmh.coerceAtLeast(0.0)),
                                    elevGainM = _rideState.value.elevGainM + cappedGain,
                                    elevLossM = _rideState.value.elevLossM + cappedLoss,
                                )
                            } else {
                                _rideState.value = _rideState.value.copy(
                                    distanceKm = _rideState.value.distanceKm + distanceKm,
                                    currentSpeedKmh = speedKmh.coerceAtLeast(0.0),
                                    maxSpeedKmh = maxOf(_rideState.value.maxSpeedKmh, speedKmh.coerceAtLeast(0.0)),
                                )
                            }
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

    private fun scheduleNoMovementCheck() {
        noMovementCheckHandler.removeCallbacks(noMovementCheckRunnable)
        noMovementCheckHandler.postDelayed(noMovementCheckRunnable, NO_MOVEMENT_CHECK_INTERVAL_MS)
    }

    private fun checkNoMovementAndAutoPause() {
        if (!_rideState.value.isTracking || _rideState.value.isPaused) return
        val now = System.currentTimeMillis()
        if (now - lastMovementTimeMs >= NO_MOVEMENT_AUTO_PAUSE_MS) {
            pauseTracking(wasAutoPause = true)
        }
    }

    private fun clearAutoPauseFlag() {
        _rideState.value = _rideState.value.copy(wasAutoPausedDueToNoMovement = false)
        updateNotification()
    }

    /**
     * Rejects locations that are likely inaccurate or stale (GPS drift, cached fixes).
     * - Poor accuracy: radius > MAX_ACCURACY_M or invalid (negative)
     * - Stale: older than MAX_LOCATION_AGE_MS (cached/cold start)
     */
    private fun isLocationAcceptable(location: Location): Boolean {
        if (location.hasAccuracy()) {
            val accuracy = location.accuracy
            if (accuracy < 0 || accuracy > MAX_ACCURACY_M) return false
        }
        val ageMs = System.currentTimeMillis() - location.time
        if (ageMs > MAX_LOCATION_AGE_MS || ageMs < -MAX_LOCATION_AGE_MS) return false
        return true
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
        private const val NO_MOVEMENT_CHECK_INTERVAL_MS = 60_000L
        private const val NO_MOVEMENT_AUTO_PAUSE_MS = 30 * 60 * 1000L

        /** Reject locations with accuracy radius worse than this (meters). */
        private const val MAX_ACCURACY_M = 50f
        /** Reject locations older than this (cached/stale). */
        private const val MAX_LOCATION_AGE_MS = 15_000L
        /** Min distance to count as movement; filters stationary GPS jitter. */
        private const val MIN_MOVEMENT_M = 5.0
        /** Max distance per update; filters outliers and jumps. */
        private const val MAX_MOVEMENT_PER_UPDATE_M = 300.0
        /** Min displacement to request an update; reduces drift callbacks. */
        private const val MIN_UPDATE_DISTANCE_M = 5f
        /** Cap elevation change per update; GPS altitude is noisy. */
        private const val MAX_ELEV_CHANGE_PER_UPDATE_M = 30.0

        const val ACTION_KEY = "action"
        const val BIKE_ID_KEY = "bike_id"
        const val ACTION_START = "start"
        const val ACTION_PAUSE = "pause"
        const val ACTION_RESUME = "resume"
        const val ACTION_STOP = "stop"
        const val ACTION_CLEAR_AUTO_PAUSE_FLAG = "clear_auto_pause_flag"
    }
}

data class RideState(
    val bikeId: Long = -1L,
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val wasAutoPausedDueToNoMovement: Boolean = false,
    val startTimeMs: Long = 0L,
    val distanceKm: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val elevGainM: Double = 0.0,
    val elevLossM: Double = 0.0,
    val locationUpdateCount: Int = 0,
)
