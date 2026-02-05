package com.you.bikecompanion.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder implementation until Gemini/OpenAI API key is configured.
 * Returns a fixed message. Replace binding in [AiModule] with a real client
 * that uses BuildConfig or remote config for the key.
 */
@Singleton
class PlaceholderAiClient @Inject constructor() : AiApiClient {
    override suspend fun send(
        userMessage: String,
        componentHealthSummary: String,
    ): Result<String> = Result.success(
        "Configure your API key (e.g. Gemini or OpenAI) in the app to get bike mechanic advice and trip readiness. " +
            "You can ask about maintenance, describe sounds or issues, or ask \"Can I do this 80 km ride?\". " +
            "When configured, I'll use your component health summary to advise on trip readiness.",
    )
}
