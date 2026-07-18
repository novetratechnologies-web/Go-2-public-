package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val tools: List<Tool>? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String // Base64 encoded audio or media data
)

@JsonClass(generateAdapter = true)
data class Tool(
    val googleSearch: GoogleSearchTool? = null,
    val googleMaps: GoogleMapsTool? = null
)

@JsonClass(generateAdapter = true)
class GoogleSearchTool

@JsonClass(generateAdapter = true)
class GoogleMapsTool

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null,
    val groundingMetadata: GroundingMetadata? = null
)

@JsonClass(generateAdapter = true)
data class GroundingMetadata(
    val webSearchQueries: List<String>? = null,
    val groundingChunks: List<GroundingChunk>? = null,
    val groundingSupports: List<GroundingSupport>? = null
)

@JsonClass(generateAdapter = true)
data class GroundingChunk(
    val web: WebSource? = null
)

@JsonClass(generateAdapter = true)
data class WebSource(
    val uri: String? = null,
    val title: String? = null
)

@JsonClass(generateAdapter = true)
data class GroundingSupport(
    val segment: Segment? = null,
    val groundingChunkIndices: List<Int>? = null
)

@JsonClass(generateAdapter = true)
data class Segment(
    val startIndex: Int? = null,
    val endIndex: Int? = null,
    val text: String? = null
)

object GeminiServiceClient {
    private const val TAG = "GeminiServiceClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(
        prompt: String,
        useSearch: Boolean = false,
        useMaps: Boolean = false,
        audioBase64: String? = null,
        audioMimeType: String? = null,
        history: List<Content> = emptyList()
    ): GeminiResponse? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty()) {
            Log.e(TAG, "GEMINI_API_KEY is empty")
            return@withContext null
        }

        // Build list of tools based on toggles
        val toolsList = mutableListOf<Tool>()
        if (useSearch) {
            toolsList.add(Tool(googleSearch = GoogleSearchTool()))
        }
        if (useMaps) {
            toolsList.add(Tool(googleMaps = GoogleMapsTool()))
        }

        val tools = if (toolsList.isNotEmpty()) toolsList else null

        // Build the parts
        val parts = mutableListOf<Part>()
        
        // If audio data is provided, add it as inlineData Part
        if (audioBase64 != null && audioMimeType != null) {
            parts.add(Part(inlineData = InlineData(mimeType = audioMimeType, data = audioBase64)))
        }
        
        // Add prompt
        parts.add(Part(text = prompt))

        // Create current contents turn
        val currentContent = Content(parts = parts, role = "user")

        // Merge with history
        val contents = history + currentContent

        val requestData = GeminiRequest(
            contents = contents,
            tools = tools
        )

        try {
            val adapter = moshi.adapter(GeminiRequest::class.java)
            val jsonRequest = adapter.toJson(requestData)
            Log.d(TAG, "Request payload: $jsonRequest")

            val url = "$BASE_URL?key=$apiKey"
            val body = jsonRequest.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                Log.d(TAG, "Response code: ${response.code}, payload: $bodyString")
                
                if (!response.isSuccessful || bodyString == null) {
                    Log.e(TAG, "Failed request: ${response.code} - ${response.message}")
                    return@withContext null
                }

                val responseAdapter = moshi.adapter(GeminiResponse::class.java)
                return@withContext responseAdapter.fromJson(bodyString)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API", e)
            return@withContext null
        }
    }
}
