package com.clintoncochrane.bikecompanion.data.ride

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.activeRideCheckpointDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "active_ride_checkpoint",
)

/**
 * Stores one active-ride checkpoint in a dedicated Preferences DataStore.
 *
 * DataStore is appropriate here because the data is a single atomic, local-only value rather
 * than relational history. It also avoids coupling checkpoint writes to completed-ride rows.
 */
@Singleton
class ActiveRideCheckpointRepository {

    private val dataStore: DataStore<Preferences>

    @Inject
    constructor(@ApplicationContext context: Context) {
        dataStore = context.activeRideCheckpointDataStore
    }

    internal constructor(dataStore: DataStore<Preferences>) {
        this.dataStore = dataStore
    }

    suspend fun save(checkpoint: ActiveRideCheckpoint) {
        dataStore.edit { preferences ->
            preferences[Keys.START_TIME_MS] = checkpoint.startTimeMs
            checkpoint.bikeId?.let { preferences[Keys.BIKE_ID] = it }
                ?: preferences.remove(Keys.BIKE_ID)
            preferences[Keys.HAD_PLACEHOLDERS_AT_START] = checkpoint.hadPlaceholdersAtStart
            preferences[Keys.DISTANCE_KM] = checkpoint.distanceKm
            preferences[Keys.AVG_SPEED_KMH] = checkpoint.avgSpeedKmh
            preferences[Keys.MAX_SPEED_KMH] = checkpoint.maxSpeedKmh
            preferences[Keys.ELEV_GAIN_M] = checkpoint.elevGainM
            preferences[Keys.ELEV_LOSS_M] = checkpoint.elevLossM
            preferences[Keys.LOCATION_UPDATE_COUNT] = checkpoint.locationUpdateCount
            preferences[Keys.IS_PAUSED] = checkpoint.isPaused
            preferences[Keys.PAUSED_AT_MS] = checkpoint.pausedAtMs
            preferences[Keys.TOTAL_PAUSED_DURATION_MS] = checkpoint.totalPausedDurationMs
            preferences[Keys.CHECKPOINTED_AT_MS] = checkpoint.checkpointedAtMs
        }
    }

    suspend fun get(): ActiveRideCheckpoint? {
        val preferences = dataStore.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }
            .first()
        val startTimeMs = preferences[Keys.START_TIME_MS] ?: return null
        return ActiveRideCheckpoint(
            bikeId = preferences[Keys.BIKE_ID],
            hadPlaceholdersAtStart = preferences[Keys.HAD_PLACEHOLDERS_AT_START] ?: false,
            startTimeMs = startTimeMs,
            distanceKm = preferences[Keys.DISTANCE_KM] ?: 0.0,
            avgSpeedKmh = preferences[Keys.AVG_SPEED_KMH] ?: 0.0,
            maxSpeedKmh = preferences[Keys.MAX_SPEED_KMH] ?: 0.0,
            elevGainM = preferences[Keys.ELEV_GAIN_M] ?: 0.0,
            elevLossM = preferences[Keys.ELEV_LOSS_M] ?: 0.0,
            locationUpdateCount = preferences[Keys.LOCATION_UPDATE_COUNT] ?: 0,
            isPaused = preferences[Keys.IS_PAUSED] ?: false,
            pausedAtMs = preferences[Keys.PAUSED_AT_MS] ?: 0L,
            totalPausedDurationMs = preferences[Keys.TOTAL_PAUSED_DURATION_MS] ?: 0L,
            checkpointedAtMs = preferences[Keys.CHECKPOINTED_AT_MS] ?: startTimeMs,
        )
    }

    suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private object Keys {
        val BIKE_ID = longPreferencesKey("bike_id")
        val HAD_PLACEHOLDERS_AT_START = booleanPreferencesKey("had_placeholders_at_start")
        val START_TIME_MS = longPreferencesKey("start_time_ms")
        val DISTANCE_KM = doublePreferencesKey("distance_km")
        val AVG_SPEED_KMH = doublePreferencesKey("avg_speed_kmh")
        val MAX_SPEED_KMH = doublePreferencesKey("max_speed_kmh")
        val ELEV_GAIN_M = doublePreferencesKey("elev_gain_m")
        val ELEV_LOSS_M = doublePreferencesKey("elev_loss_m")
        val LOCATION_UPDATE_COUNT = intPreferencesKey("location_update_count")
        val IS_PAUSED = booleanPreferencesKey("is_paused")
        val PAUSED_AT_MS = longPreferencesKey("paused_at_ms")
        val TOTAL_PAUSED_DURATION_MS = longPreferencesKey("total_paused_duration_ms")
        val CHECKPOINTED_AT_MS = longPreferencesKey("checkpointed_at_ms")
    }
}
