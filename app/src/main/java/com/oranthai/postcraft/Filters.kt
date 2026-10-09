package com.oranthai.postcraft

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import kotlin.math.hypot
import kotlin.random.Random

/** A free, on-device look: a colour grade plus optional finishing touches. */
data class Look(
    val label: String,
    val saturation: Float = 1f,
    val contrast: Float = 1f,
    /** Added to every channel, roughly -40..40. */
    val brightness: Float = 0f,
    /** Positive warms (more red, less blue), negative cools. */
    val warmth: Float = 0f,
    /** Colour overlaid on the whole photo for a toned feel. */
    val tint: Int? = null,
    val tintAlpha: Float = 0f,
    /** Lifts the blacks for a matte film look, 0..1. */
    val fade: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val polaroid: Boolean = false,
)

/** Restyles photos on the phone with colour grading, so posts cost nothing and keep the real subject. */
object Filters {
    /** Instagram's recommended feed width. */
    private const val OUT_WIDTH = 1080

    /** (zoom, strength) per variation, so each option is framed or graded a little differently. */
    private val variants = listOf(1f to 1f, 1.15f to 1f, 1f to 1.35f, 1.08f to 0.7f)

    /** Renders [src] with [look], cropped to [ratio] (e.g. "4:5"). */
    fun render(src: Bitmap, look: Look, ratio: String, variation: Int): Bitmap {
        val (zoom, strength) = variants[variation % variants.size]
        val (rw, rh) = ratio.split(":").map { it.toFloat() }
        val outW = OUT_WIDTH
        val outH = (OUT_WIDTH * rh / rw).toInt()

        if (!look.polaroid) return graded(src, look, outW, outH, zoom, strength, variation)

        // Size the photo so photo + frame together still match the chosen ratio.
        val margin = (outW * 0.06f).toInt()
        val bottom = (outW * 0.2f).toInt()
        val photo = graded(src, look, outW - 2 * margin, outH - margin - bottom, zoom, strength, variation)
        return Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888).also {
            Canvas(it).apply {
                drawColor(Color.rgb(250, 250, 246))
                drawBitmap(photo, margin.toFloat(), margin.toFloat(), null)
            }
        }
    }

    private fun graded(src: Bitmap, look: Look, w: Int, h: Int, zoom: Float, strength: Float, seed: Int): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix(look, strength)) }
        canvas.drawBitmap(centerCrop(src, w.toFloat() / h, zoom), null, Rect(0, 0, w, h), paint)

        look.tint?.let {
            val a = (look.tintAlpha * strength * 255).toInt().coerceIn(0, 255)
            canvas.drawColor(Color.argb(a, Color.red(it), Color.green(it), Color.blue(it)), PorterDuff.Mode.OVERLAY)
        }
        if (look.vignette > 0f) vignette(canvas, w, h, (look.vignette * strength).coerceAtMost(1f))
        if (look.grain > 0f) grain(canvas, w, h, (look.grain * strength).coerceAtMost(1f), seed)
        return out
    }

    private fun matrix(look: Look, strength: Float): ColorMatrix {
        fun mix(v: Float, neutral: Float) = neutral + (v - neutral) * strength
        val m = ColorMatrix().apply { setSaturation(mix(look.saturation, 1f).coerceAtLeast(0f)) }

        val c = mix(look.contrast, 1f)
        val t = 128f * (1f - c) + look.brightness * strength
        m.postConcat(ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f,
        )))

        val w = look.warmth * strength * 0.08f
        m.postConcat(ColorMatrix().apply { setScale(1f + w, 1f, 1f - w, 1f) })

        val f = look.fade * strength * 0.18f
        if (f > 0f) m.postConcat(ColorMatrix(floatArrayOf(
            1f - f, 0f, 0f, 0f, f * 255f,
            0f, 1f - f, 0f, 0f, f * 255f,
            0f, 0f, 1f - f, 0f, f * 255f,
            0f, 0f, 0f, 1f, 0f,
        )))
        return m
    }

    /** Largest centred region of [src] with [aspect] (w/h), tightened by [zoom]. */
    private fun centerCrop(src: Bitmap, aspect: Float, zoom: Float): Bitmap {
        var cw = src.width.toFloat()
        var ch = cw / aspect
        if (ch > src.height) { ch = src.height.toFloat(); cw = ch * aspect }
        cw /= zoom; ch /= zoom
        val x = ((src.width - cw) / 2).toInt()
        val y = ((src.height - ch) / 2).toInt()
        return Bitmap.createBitmap(src, x, y, cw.toInt().coerceAtLeast(1), ch.toInt().coerceAtLeast(1))
    }

    private fun vignette(canvas: Canvas, w: Int, h: Int, amount: Float) {
        val radius = hypot(w.toFloat(), h.toFloat()) / 2f
        val edge = Color.argb((amount * 210).toInt(), 0, 0, 0)
        val paint = Paint().apply {
            shader = RadialGradient(w / 2f, h / 2f, radius, intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, edge),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    private fun grain(canvas: Canvas, w: Int, h: Int, amount: Float, seed: Int) {
        val size = 256
        val rnd = Random(seed)
        val pixels = IntArray(size * size) {
            val v = rnd.nextInt(256)
            Color.argb(rnd.nextInt(40), v, v, v)
        }
        val tile = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            alpha = (amount * 255).toInt()
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }
}
