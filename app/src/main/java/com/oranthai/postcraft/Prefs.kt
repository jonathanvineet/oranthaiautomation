package com.oranthai.postcraft

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("postcraft", Context.MODE_PRIVATE)

    var geminiKey: String
        get() = sp.getString("gemini_key", "") ?: ""
        set(v) = sp.edit().putString("gemini_key", v.trim()).apply()

    var imageModel: String
        get() = sp.getString("image_model", null)?.takeIf { it.isNotBlank() } ?: DEFAULT_IMAGE_MODEL
        set(v) = sp.edit().putString("image_model", v.trim()).apply()

    var textModel: String
        get() = sp.getString("text_model", null)?.takeIf { it.isNotBlank() } ?: DEFAULT_TEXT_MODEL
        set(v) = sp.edit().putString("text_model", v.trim()).apply()

    /** Optional: who is posting (brand/creator voice) - fed into captions. */
    var brand: String
        get() = sp.getString("brand", "") ?: ""
        set(v) = sp.edit().putString("brand", v.trim()).apply()

    var directPublish: Boolean
        get() = sp.getBoolean("direct_publish", false)
        set(v) = sp.edit().putBoolean("direct_publish", v).apply()

    var igUserId: String
        get() = sp.getString("ig_user_id", "") ?: ""
        set(v) = sp.edit().putString("ig_user_id", v.trim()).apply()

    var igToken: String
        get() = sp.getString("ig_token", "") ?: ""
        set(v) = sp.edit().putString("ig_token", v.trim()).apply()

    var imgbbKey: String
        get() = sp.getString("imgbb_key", "") ?: ""
        set(v) = sp.edit().putString("imgbb_key", v.trim()).apply()

    val directReady get() = igUserId.isNotBlank() && igToken.isNotBlank() && imgbbKey.isNotBlank()

    companion object {
        const val DEFAULT_IMAGE_MODEL = "gemini-2.5-flash-image"
        const val DEFAULT_TEXT_MODEL = "gemini-2.5-flash"
    }
}
