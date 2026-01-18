package com.example.wind

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiService {

    private const val BASE_URL = "http://192.168.1.10:3001"


    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun postEvent(topic: String, message: String, lat: Double, lon: Double) : Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("topic", topic)
                    put("message", message)
                    put("lat", lat)
                    put("lon", lon)
                }

                val req = Request.Builder()
                    .url("$BASE_URL/api/events")
                    .post(payload.toString().toRequestBody(JSON))
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) Result.success(Unit)
                    else Result.failure(Exception("HTTP ${resp.code}: ${resp.body?.string()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getEvents(topic: String? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = if (topic.isNullOrBlank()) {
                    "$BASE_URL/api/events"
                } else {
                    val encoded = java.net.URLEncoder.encode(topic, "UTF-8")
                    "$BASE_URL/api/events?topic=$encoded"
                }

                val req = Request.Builder().url(url).get().build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) Result.success(body)
                    else Result.failure(Exception("HTTP ${resp.code}: $body"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    suspend fun deleteEvent(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("$BASE_URL/api/events/$id")
                    .delete()
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) Result.success(Unit)
                    else Result.failure(Exception("HTTP ${resp.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

}
