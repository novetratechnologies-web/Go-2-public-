package com.example.data.sync

import android.util.Log
import com.example.data.database.BlockedSite
import com.example.data.database.Skill
import com.example.data.database.WebHistory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class CloudSyncService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val payloadAdapter = moshi.adapter(SyncPayload::class.java)

    private val baseUrl = "https://kvdb.io/go_learn_pzxktr_v1"

    /**
     * Generates a random, secure, student-friendly 6-character sync key (e.g. "GO-9X2").
     */
    fun generateSyncKey(): String {
        val allowedChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ" // Avoid confusing chars like O, 0, I, 1
        return buildString {
            append("GO-")
            for (i in 0 until 4) {
                append(allowedChars.random())
            }
        }
    }

    /**
     * Uploads the student's progress payload to the secure cloud.
     */
    suspend fun pushProgress(
        syncKey: String,
        skills: List<Skill>,
        bookmarks: List<WebHistory>,
        blockedSites: List<BlockedSite>
    ): Boolean {
        return try {
            val sanitizedKey = syncKey.trim().uppercase()
            if (sanitizedKey.isEmpty()) return false

            val payload = SyncPayload(
                syncKey = sanitizedKey,
                lastSyncedAt = System.currentTimeMillis(),
                skills = skills,
                bookmarks = bookmarks,
                blockedSites = blockedSites
            )

            val jsonString = payloadAdapter.toJson(payload)
            val requestBody = jsonString.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/$sanitizedKey")
                .put(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("CloudSyncService", "Successfully pushed progress for key: $sanitizedKey")
                    true
                } else {
                    Log.e("CloudSyncService", "Failed to push progress. Code: ${response.code}")
                    false
                }
            }
        } catch (e: IOException) {
            Log.e("CloudSyncService", "Network error pushing progress", e)
            false
        } catch (e: Exception) {
            Log.e("CloudSyncService", "Error parsing / pushing progress", e)
            false
        }
    }

    /**
     * Downloads the student's progress payload from the cloud.
     */
    suspend fun pullProgress(syncKey: String): SyncPayload? {
        return try {
            val sanitizedKey = syncKey.trim().uppercase()
            if (sanitizedKey.isEmpty()) return null

            val request = Request.Builder()
                .url("$baseUrl/$sanitizedKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrEmpty()) {
                        Log.d("CloudSyncService", "Successfully pulled progress for key: $sanitizedKey")
                        payloadAdapter.fromJson(responseBody)
                    } else {
                        null
                    }
                } else if (response.code == 404) {
                    Log.d("CloudSyncService", "No data found for sync key: $sanitizedKey")
                    null
                } else {
                    Log.e("CloudSyncService", "Failed to pull progress. Code: ${response.code}")
                    null
                }
            }
        } catch (e: IOException) {
            Log.e("CloudSyncService", "Network error pulling progress", e)
            null
        } catch (e: Exception) {
            Log.e("CloudSyncService", "Error parsing / pulling progress", e)
            null
        }
    }
}
