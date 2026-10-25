package com.example.core.network

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
import com.example.BuildConfig
 * Service to initialize and manage Gemini API calls using the GEMINI_API_KEY
 * from BuildConfig environment configuration.
 */
class GeminiService(
    private val customApiKey: String? = null
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves the active API key: custom key if provided, or GEMINI_API_KEY from BuildConfig.
     */
    fun getApiKey(): String {
        val custom = customApiKey?.trim()
        if (!custom.isNullOrEmpty()) return custom
        return BuildConfig.GEMINI_API_KEY.trim()
    }

    /**
     * Checks whether a valid Gemini API key is configured.
     */
    fun isApiKeyConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotEmpty() && !key.contains("MY_GEMINI_API_KEY")
    }

    /**
     * Executes a simple test query to verify Gemini API initialization and connectivity.
     *
     * @param prompt The test prompt string. Defaults to a standard sanity query.
     * @return Result containing the response text from Gemini or exception on failure.
     */
    suspend fun testQuery(
        prompt: String = "Hello Gemini! Briefly state your status and model identity in one short sentence."
    ): Result<String> = generateContent(prompt)

    /**
     * Sends a text prompt to the Gemini API generateContent endpoint.
     *
     * @param prompt User prompt text.
     * @param model Gemini model alias. Defaults to 'gemini-3.5-flash'.
     * @return Result wrapping the generated text string or exception.
     */
    suspend fun generateContent(
        prompt: String,
        model: String = "gemini-3.5-flash"
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey.contains("MY_GEMINI_API_KEY")) {
            return@withContext Result.failure(
                IllegalStateException("GEMINI_API_KEY is not configured in environment or settings.")
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val root = JSONObject(responseBody)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotEmpty()) {
                            return@withContext Result.success(text)
                        }
                    }
                }
                Result.failure(Exception("Empty candidate response received from Gemini API."))
            } else {
                val errorBody = response.body?.string() ?: ""
                val errorMessage = try {
                    JSONObject(errorBody).optJSONObject("error")?.optString("message")
                        ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}"
                }
                Result.failure(Exception("Gemini API Error: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
