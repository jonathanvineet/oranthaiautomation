package com.oranthai.postcraft

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object Images {
    /** Long edge we send to the model - plenty for restyling, keeps uploads fast. */
    private const val MAX_EDGE = 1536

    fun fromUri(context: Context, uri: Uri): ByteArray =
        decodeScaled(ImageDecoder.createSource(context.contentResolver, uri))

    /**
     * Shared text may be a direct image link or a Google Images result page
     * (…/imgres?imgurl=…). Pulls the image URL out and downloads it.
     */
    fun fromSharedText(text: String): ByteArray {
        val link = Regex("""https?://\S+""").find(text)?.value
            ?: throw ApiException("No image or link found in what was shared.")
        val parsed = Uri.parse(link)
        val imageUrl = parsed.getQueryParameter("imgurl") ?: parsed.getQueryParameter("mediaurl") ?: link
        val bytes = Http.client.newCall(Request.Builder().url(imageUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) PostCraft")
            .build()).execute().use { res ->
            val type = res.body?.contentType()
            if (!res.isSuccessful || type?.type != "image") {
                throw ApiException("That link isn't a direct image. Open the image and share the photo itself.")
            }
            res.body!!.bytes()
        }
        return decodeScaled(ImageDecoder.createSource(ByteBuffer.wrap(bytes)))
    }

    fun toJpeg(bitmap: Bitmap, quality: Int = 92): ByteArray =
        ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()

    /** Re-encodes model output (often PNG) as JPEG, which Instagram handles best. */
    fun normalize(bytes: ByteArray): Pair<ByteArray, Bitmap> {
        val bmp = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { d, _, _ ->
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        return toJpeg(bmp, 95) to bmp
    }

    fun decode(bytes: ByteArray): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { d, _, _ ->
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }

    fun saveToGallery(context: Context, jpeg: ByteArray): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "PostCraft_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PostCraft")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw ApiException("Couldn't save to gallery.")
        resolver.openOutputStream(uri)!!.use { it.write(jpeg) }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        return uri
    }

    private fun decodeScaled(source: ImageDecoder.Source): ByteArray {
        val bmp = ImageDecoder.decodeBitmap(source) { d, info, _ ->
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val w = info.size.width
            val h = info.size.height
            val scale = MAX_EDGE.toFloat() / maxOf(w, h)
            if (scale < 1f) d.setTargetSize((w * scale).toInt(), (h * scale).toInt())
        }
        return toJpeg(bmp, 90)
    }
}
