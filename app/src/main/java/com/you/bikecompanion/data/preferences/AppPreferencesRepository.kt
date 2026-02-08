package com.you.bikecompanion.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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

    companion object {
        const val DEFAULT_CLOSE_TO_SERVICE_THRESHOLD = 20
        const val MIN_THRESHOLD = 1
        const val MAX_THRESHOLD = 100
    }
}
