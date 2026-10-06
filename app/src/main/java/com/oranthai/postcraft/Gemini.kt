package com.oranthai.postcraft

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** Minimal Gemini REST client (generateContent) for image restyling and captions. */
class Gemini(private val apiKey: String) {

    data class Caption(val caption: String, val hashtags: List<String>) {
        fun full() = if (hashtags.isEmpty()) caption
        else caption + "\n\n" + hashtags.joinToString(" ") { if (it.startsWith("#")) it else "#$it" }
    }

    /** Sends [jpeg] plus [prompt] to the image model and returns the generated image bytes. */
    fun generateImage(model: String, jpeg: ByteArray, prompt: String, aspectRatio: String): ByteArray {
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray()
                .put(imagePart(jpeg))
                .put(JSONObject().put("text", prompt)))))
            .put("generationConfig", JSONObject()
                .put("responseModalities", JSONArray().put("TEXT").put("IMAGE"))
                .put("imageConfig", JSONObject().put("aspectRatio", aspectRatio)))

        val parts = generate(model, body)
        for (i in 0 until parts.length()) {
            val p = parts.getJSONObject(i)
            val inline = p.optJSONObject("inlineData") ?: p.optJSONObject("inline_data") ?: continue
            return Base64.decode(inline.getString("data"), Base64.DEFAULT)
        }
        throw ApiException("Model returned no image. " + textOf(parts).ifBlank { "Try another style or photo." })
    }

    fun generateCaption(model: String, jpeg: ByteArray, instructions: String): Caption {
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray()
                .put(imagePart(jpeg))
                .put(JSONObject().put("text", instructions)))))
            .put("generationConfig", JSONObject()
                .put("responseMimeType", "application/json")
                .put("responseSchema", JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", JSONObject()
                        .put("caption", JSONObject().put("type", "STRING"))
                        .put("hashtags", JSONObject().put("type", "ARRAY")
                            .put("items", JSONObject().put("type", "STRING"))))
                    .put("required", JSONArray().put("caption").put("hashtags"))))

        val text = textOf(generate(model, body)).trim()
        val json = runCatching { JSONObject(text.removePrefix("```json").removeSuffix("```").trim()) }
            .getOrElse { return Caption(text, emptyList()) }
        val tags = json.optJSONArray("hashtags")
        return Caption(
            json.optString("caption").trim(),
            List(tags?.length() ?: 0) { tags!!.getString(it).trim() }.filter { it.isNotBlank() },
        )
    }

    private fun generate(model: String, body: JSONObject): JSONArray {
        if (apiKey.isBlank()) throw ApiException("Add your Gemini API key in Settings first.")
        val req = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val res = JSONObject(Http.call(req))
        res.optJSONObject("promptFeedback")?.optString("blockReason")?.takeIf { it.isNotBlank() }?.let {
            throw ApiException("Blocked by safety filter ($it).")
        }
        val cand = res.optJSONArray("candidates")?.optJSONObject(0)
            ?: throw ApiException("Empty response from model.")
        return cand.optJSONObject("content")?.optJSONArray("parts")
            ?: throw ApiException("No content (finishReason=${cand.optString("finishReason")}).")
    }

    private fun imagePart(jpeg: ByteArray) = JSONObject().put("inline_data", JSONObject()
        .put("mime_type", "image/jpeg")
        .put("data", Base64.encodeToString(jpeg, Base64.NO_WRAP)))

    private fun textOf(parts: JSONArray) = buildString {
        for (i in 0 until parts.length()) append(parts.getJSONObject(i).optString("text"))
    }
}
