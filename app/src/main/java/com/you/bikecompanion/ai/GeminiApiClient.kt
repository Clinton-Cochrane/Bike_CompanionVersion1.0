package com.you.bikecompanion.ai

import com.you.bikecompanion.data.preferences.SecurePreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
private const val MODEL = "gemini-1.5-flash"
private const val NO_KEY_MESSAGE = "Please set your Gemini API key in Settings to use the bike assistant. Tap the settings icon above to add your key."

private val SYSTEM_INSTRUCTION = """
You are a bike mechanic expert. You can answer maintenance, compatibility, and how-to questions, including torque specs when relevant.

The user's garage summary is provided below. Use it to answer questions about trip readiness, which bikes or components need service, and drivetrain compatibility (e.g. cassette and chain). If the user asks about a specific bike or component, refer to this data when applicable.

Be concise and accurate. For torque, cite common standards (e.g. manufacturer or industry) when you can. If something is beyond your knowledge, say so.
""".trimIndent()

/**
 * Calls the Gemini (Generative Language) API with the user's API key from secure storage.
 * When no key is set, returns a friendly message instead of calling the API.
 */
@Singleton
class GeminiApiClient @Inject constructor(
    private val securePreferences: SecurePreferencesRepository,
) : AiApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun send(
        userMessage: String,
        componentHealthSummary: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = securePreferences.getApiKeySync()
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.success(NO_KEY_MESSAGE)
        }

        val userContent = buildUserContent(userMessage, componentHealthSummary)
        val requestBody = buildRequestBody(userContent)

        val request = Request.Builder()
            .url("$BASE_URL/models/$MODEL:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    return@withContext Result.failure(
                        ApiException("API error ${response.code}: ${response.message}. $body"),
                    )
                }
                val json = response.body?.string() ?: return@withContext Result.failure(
                    ApiException("Empty response"),
                )
                parseResponse(json)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildUserContent(userMessage: String, componentHealthSummary: String): String {
        return "Garage context:\n$componentHealthSummary\n\nUser question: $userMessage"
    }

    private fun buildRequestBody(userContent: String): String {
        val partsArray = org.json.JSONArray().put(JSONObject().put("text", userContent))
        val contentsArray = org.json.JSONArray().put(JSONObject().put("parts", partsArray))
        val systemParts = org.json.JSONArray().put(JSONObject().put("text", SYSTEM_INSTRUCTION))
        val systemInstruction = JSONObject().put("parts", systemParts)
        return JSONObject()
            .put("contents", contentsArray)
            .put("systemInstruction", systemInstruction)
            .toString()
    }

    private fun parseResponse(json: String): Result<String> {
        return try {
            val obj = JSONObject(json)
            val candidates = obj.optJSONArray("candidates")
                ?: return Result.failure(ApiException("No candidates in response"))
            val first = candidates.optJSONObject(0)
                ?: return Result.failure(ApiException("Empty candidates"))
            val content = first.optJSONObject("content")
                ?: return Result.failure(ApiException("No content in candidate"))
            val parts = content.optJSONArray("parts")
                ?: return Result.failure(ApiException("No parts in content"))
            val textPart = parts.optJSONObject(0)
                ?: return Result.failure(ApiException("No text part"))
            val text = textPart.optString("text", "").trim()
            if (text.isEmpty()) Result.failure(ApiException("Empty text in response"))
            else Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ApiException(message: String) : Exception(message)
