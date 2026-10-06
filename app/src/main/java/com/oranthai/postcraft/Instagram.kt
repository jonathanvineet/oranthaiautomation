package com.oranthai.postcraft

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import java.io.File

object Instagram {
    private const val PACKAGE = "com.instagram.android"
    private const val GRAPH_VERSION = "v23.0"

    fun isInstalled(context: Context) =
        runCatching { context.packageManager.getPackageInfo(PACKAGE, 0) }.isSuccess

    /**
     * Opens Instagram's composer with the image attached. Instagram ignores caption
     * extras for feed posts, so the caption goes on the clipboard for a single paste.
     */
    fun openComposer(context: Context, jpeg: ByteArray, caption: String) {
        val uri = cacheForSharing(context, jpeg)
        if (caption.isNotBlank()) {
            val cm = context.getSystemService(ClipboardManager::class.java)
            cm.setPrimaryClip(ClipData.newPlainText("Instagram caption", caption))
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            clipData = ClipData.newRawUri("post", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (isInstalled(context)) {
            context.startActivity(Intent(send).setPackage(PACKAGE))
        } else {
            context.startActivity(Intent.createChooser(send, "Share post")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun shareAnywhere(context: Context, jpeg: ByteArray, caption: String) {
        val uri = cacheForSharing(context, jpeg)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            clipData = ClipData.newRawUri("post", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share post").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * Fully automatic publish through the official Instagram API (Business/Creator accounts).
     * The API only accepts a public image URL, so the image is first uploaded to imgbb
     * with a 1-hour expiry. Returns the published media id.
     */
    fun publishDirect(prefs: Prefs, jpeg: ByteArray, caption: String, aspectRatio: String): String {
        if (!prefs.directReady) throw ApiException("Fill in Instagram User ID, access token and imgbb key in Settings.")
        if (aspectRatio == "9:16") throw ApiException("Feed posts can't be 9:16. Use 4:5, 1:1 or 16:9 for direct publish.")

        val imageUrl = uploadToImgbb(prefs.imgbbKey, jpeg)

        // Tokens from "Instagram Login" start with IG and use graph.instagram.com;
        // Facebook-Login (Page-linked) tokens use graph.facebook.com.
        val host = if (prefs.igToken.startsWith("IG")) "graph.instagram.com" else "graph.facebook.com"
        val base = "https://$host/$GRAPH_VERSION"
        val token = prefs.igToken

        val containerId = JSONObject(Http.call(Request.Builder()
            .url("$base/${prefs.igUserId}/media")
            .post(FormBody.Builder()
                .add("image_url", imageUrl)
                .add("caption", caption)
                .add("access_token", token)
                .build())
            .build())).getString("id")

        // Wait for Instagram to fetch and process the image.
        repeat(20) {
            val status = JSONObject(Http.call(Request.Builder()
                .url("$base/$containerId?fields=status_code&access_token=${Uri.encode(token)}")
                .build())).optString("status_code")
            if (status == "FINISHED" || status.isBlank()) return publish(base, prefs.igUserId, containerId, token)
            if (status == "ERROR" || status == "EXPIRED") throw ApiException("Instagram couldn't process the image ($status).")
            Thread.sleep(1500)
        }
        throw ApiException("Instagram is still processing - try again in a minute.")
    }

    private fun publish(base: String, userId: String, containerId: String, token: String): String =
        JSONObject(Http.call(Request.Builder()
            .url("$base/$userId/media_publish")
            .post(FormBody.Builder()
                .add("creation_id", containerId)
                .add("access_token", token)
                .build())
            .build())).getString("id")

    private fun uploadToImgbb(key: String, jpeg: ByteArray): String {
        val res = JSONObject(Http.call(Request.Builder()
            .url("https://api.imgbb.com/1/upload?expiration=3600&key=${Uri.encode(key)}")
            .post(FormBody.Builder()
                .add("image", Base64.encodeToString(jpeg, Base64.NO_WRAP))
                .build())
            .build()))
        return res.getJSONObject("data").getString("url")
    }

    private fun cacheForSharing(context: Context, jpeg: ByteArray): Uri {
        val dir = File(context.cacheDir, "posts").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "post_${System.currentTimeMillis()}.jpg").apply { writeBytes(jpeg) }
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }
}
