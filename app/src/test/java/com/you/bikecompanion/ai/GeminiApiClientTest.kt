package com.you.bikecompanion.ai

import com.you.bikecompanion.data.preferences.SecurePreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [GeminiApiClient]: no-api-key path (returns friendly message without network call).
 * Full request/response behavior can be covered by integration tests or MockWebServer.
 */
class GeminiApiClientTest {

    private val securePreferences = mockk<SecurePreferencesRepository>()
    private lateinit var client: GeminiApiClient

    @Before
    fun setUp() {
        client = GeminiApiClient(securePreferences)
    }

    @Test
    fun send_returns_success_with_message_when_api_key_null() = runTest {
        every { securePreferences.getApiKeySync() } returns null

        val result = client.send("Hello", "No bikes.")

        assertTrue(result.isSuccess)
        assertTrue(
            result.getOrNull()!!.contains("API key") && result.getOrNull()!!.contains("Settings"),
        )
    }

    @Test
    fun send_returns_success_with_message_when_api_key_blank() = runTest {
        every { securePreferences.getApiKeySync() } returns "   "

        val result = client.send("Hi", "Summary")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.contains("API key"))
    }

    @Test
    fun send_includes_instructions_when_no_key() = runTest {
        every { securePreferences.getApiKeySync() } returns null

        val result = client.send("What needs service?", "Bikes:\nRoad Bike 100 km")

        assertTrue(result.isSuccess)
        val text = result.getOrNull()!!
        assertTrue(text.contains("Settings") || text.contains("key"))
    }
}
