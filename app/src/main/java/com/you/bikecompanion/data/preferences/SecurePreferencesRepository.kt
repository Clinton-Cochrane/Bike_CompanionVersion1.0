package com.you.bikecompanion.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_FILE = "bike_companion_secure"
private const val KEY_GEMINI_API_KEY = "gemini_api_key"

/**
 * Stores sensitive values (e.g. Gemini API key) using EncryptedSharedPreferences.
 * Encryption key is held in Android KeyStore. Do not use for non-sensitive data.
 */
@Singleton
class SecurePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _apiKeyFlow = MutableStateFlow(prefs.getString(KEY_GEMINI_API_KEY, null))
    val apiKey: Flow<String?> = _apiKeyFlow.asStateFlow()

    fun getApiKeySync(): String? = prefs.getString(KEY_GEMINI_API_KEY, null)

    fun setApiKey(value: String?) {
        val toStore = value?.trim()?.takeIf { it.isNotEmpty() }
        prefs.edit().putString(KEY_GEMINI_API_KEY, toStore).apply()
        _apiKeyFlow.value = getApiKeySync()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
        _apiKeyFlow.value = null
    }
}
