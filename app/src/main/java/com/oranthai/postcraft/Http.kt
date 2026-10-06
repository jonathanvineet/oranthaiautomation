package com.oranthai.postcraft

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Executes [req] and returns the body, throwing a readable error on non-2xx. */
    fun call(req: Request): String = client.newCall(req).execute().use { it.bodyOrThrow() }

    private fun Response.bodyOrThrow(): String {
        val text = body?.string().orEmpty()
        if (!isSuccessful) {
            val msg = runCatching {
                val o = org.json.JSONObject(text)
                o.optJSONObject("error")?.let { e -> e.optString("message").ifBlank { e.toString() } }
                    ?: o.optString("message")
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: text.take(300)
            throw ApiException("HTTP $code: $msg")
        }
        return text
    }
}

class ApiException(message: String) : Exception(message)
