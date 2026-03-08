package com.you.bikecompanion.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/** User-configurable app preferences. Defaults match app defaults (e.g. health alert threshold 20%). */
@Singleton
class AppPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val closeToServiceKey = intPreferencesKey("close_to_service_health_threshold")
    private val hasSeenHealthConnectImportDisclaimerKey = booleanPreferencesKey("has_seen_health_connect_import_disclaimer")
    private val dismissedRideFlagIdsKey = stringPreferencesKey("dismissed_ride_flag_ids")

    /**
     * Threshold (health %) below which a "mild" alert is shown (close to inspection/service).
     * Default [DEFAULT_CLOSE_TO_SERVICE_THRESHOLD]. When health is 0, a danger alert is shown instead.
     */
    val closeToServiceHealthThreshold: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[closeToServiceKey] ?: DEFAULT_CLOSE_TO_SERVICE_THRESHOLD
    }

    suspend fun setCloseToServiceHealthThreshold(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[closeToServiceKey] = value.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD)
        }
    }

    /** Whether the user has seen the Health Connect import disclaimer (elevation/max speed may be unavailable). */
    val hasSeenHealthConnectImportDisclaimer: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[hasSeenHealthConnectImportDisclaimerKey] ?: false
    }

    suspend fun setHasSeenHealthConnectImportDisclaimer() {
        context.dataStore.edit { prefs ->
            prefs[hasSeenHealthConnectImportDisclaimerKey] = true
        }
    }

    /** Returns current value of [hasSeenHealthConnectImportDisclaimer]. */
    suspend fun getHasSeenHealthConnectImportDisclaimer(): Boolean =
        hasSeenHealthConnectImportDisclaimer.first()

    /** Ride IDs whose review flags have been dismissed by the user. */
    val dismissedRideFlagIds: Flow<Set<Long>> = context.dataStore.data.map { prefs ->
        val raw = prefs[dismissedRideFlagIdsKey] ?: ""
        if (raw.isEmpty()) emptySet()
        else raw.split(",").mapNotNull { s -> s.trim().toLongOrNull() }.toSet()
    }

    suspend fun addDismissedRideFlagId(rideId: Long) {
        context.dataStore.edit { prefs ->
            val raw = prefs[dismissedRideFlagIdsKey] ?: ""
            val current = if (raw.isEmpty()) emptySet<Long>() else raw.split(",").mapNotNull { s -> s.trim().toLongOrNull() }.toSet()
            prefs[dismissedRideFlagIdsKey] = (current + rideId).joinToString(",")
        }
    }

    companion object {
        const val DEFAULT_CLOSE_TO_SERVICE_THRESHOLD = 20
        const val MIN_THRESHOLD = 1
        const val MAX_THRESHOLD = 100
    }
}
