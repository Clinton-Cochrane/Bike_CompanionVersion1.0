package com.you.bikecompanion.ai

/**
 * Abstraction for the bike assistant AI (e.g. Gemini or OpenAI).
 * Implement with a real API when an API key is configured (BuildConfig or remote config).
 * [componentHealthSummary] is passed for trip-readiness and maintenance advice.
 */
interface AiApiClient {
    suspend fun send(
        userMessage: String,
        componentHealthSummary: String,
    ): Result<String>
}
