package com.oranthai.postcraft

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudioViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)

    class Result(val jpeg: ByteArray, val bitmap: Bitmap)

    // Source photo
    var sourceJpeg by mutableStateOf<ByteArray?>(null); private set
    var sourceBitmap by mutableStateOf<Bitmap?>(null); private set
    var loadingSource by mutableStateOf(false); private set

    // Options
    var aesthetic by mutableStateOf(Styles.aesthetics.first())
    var tone by mutableStateOf(Styles.tones.first())
    var ratio by mutableStateOf(Styles.ratios.first())
    var variations by mutableStateOf(2)
    var extra by mutableStateOf("")
    var hashtags by mutableStateOf(true)

    // Output
    val results = mutableStateListOf<Result>()
    var selected by mutableStateOf(0)
    var caption by mutableStateOf("")
    var generating by mutableStateOf(false); private set
    var captioning by mutableStateOf(false); private set
    var posting by mutableStateOf(false); private set

    /** One-shot user-facing message (shown in a snackbar, then cleared). */
    var message by mutableStateOf<String?>(null)

    val selectedResult get() = results.getOrNull(selected)
    val hasKey get() = prefs.geminiKey.isNotBlank()

    fun loadUri(uri: Uri) = loadSource { Images.fromUri(getApplication(), uri) }

    fun loadSharedText(text: String) = loadSource { Images.fromSharedText(text) }

    private fun loadSource(block: () -> ByteArray) {
        loadingSource = true
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { block().let { it to Images.decode(it) } } }
                .onSuccess { (jpeg, bmp) ->
                    sourceJpeg = jpeg
                    sourceBitmap = bmp
                    results.clear()
                    caption = ""
                }
                .onFailure { message = it.message ?: "Couldn't open that image." }
            loadingSource = false
        }
    }

    fun generate() {
        val src = sourceBitmap ?: return run { message = "Pick a photo first." }
        generating = true
        results.clear()
        selected = 0

        viewModelScope.launch {
            if (hasKey) launch { captionFrom(Gemini(prefs.geminiKey), sourceJpeg!!) }
            else message = "Images ready. Add a Gemini API key in Settings for AI captions."
            val made = withContext(Dispatchers.Default) {
                (0 until variations).map { i ->
                    async {
                        val bmp = Filters.render(src, aesthetic, ratio.prompt, i)
                        Result(Images.toJpeg(bmp, 95), bmp)
                    }
                }.awaitAll()
            }
            results.addAll(made)
            generating = false
        }
    }

    fun regenerateCaption() {
        val src = selectedResult?.jpeg ?: sourceJpeg ?: return
        if (!hasKey) return run { message = "Add your Gemini API key in Settings." }
        viewModelScope.launch { captionFrom(Gemini(prefs.geminiKey), src) }
    }

    private suspend fun captionFrom(gemini: Gemini, jpeg: ByteArray) {
        captioning = true
        runCatching { withContext(Dispatchers.IO) { gemini.generateCaption(prefs.textModel, jpeg, captionPrompt()) } }
            .onSuccess { caption = if (hashtags) it.full() else it.caption }
            .onFailure { message = "Caption: ${it.message}" }
        captioning = false
    }

    /** The one-tap button: direct publish if configured, otherwise hand off to the Instagram app. */
    fun postToInstagram(context: Context) {
        val result = selectedResult ?: return run { message = "Generate a post first." }
        if (prefs.directPublish) {
            posting = true
            viewModelScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { Instagram.publishDirect(prefs, result.jpeg, caption, ratio.prompt) }
                }
                    .onSuccess { message = "Posted to Instagram!" }
                    .onFailure { message = "Instagram: ${it.message}" }
                posting = false
            }
        } else {
            runCatching { Instagram.openComposer(context, result.jpeg, caption) }
                .onSuccess {
                    if (caption.isNotBlank()) message = "Caption copied - long-press the caption box in Instagram and paste."
                }
                .onFailure { message = it.message ?: "Couldn't open Instagram." }
        }
    }

    fun shareElsewhere(context: Context) {
        val result = selectedResult ?: return
        runCatching { Instagram.shareAnywhere(context, result.jpeg, caption) }
            .onFailure { message = it.message }
    }

    fun saveSelected() {
        val result = selectedResult ?: return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { Images.saveToGallery(getApplication(), result.jpeg) } }
                .onSuccess { message = "Saved to Pictures/PostCraft" }
                .onFailure { message = it.message ?: "Save failed." }
        }
    }

    private fun captionPrompt() = buildString {
        appendLine("Write an Instagram caption for a post made from this photo.")
        appendLine("Visual style: ${aesthetic.label}.")
        appendLine("Tone: ${tone.prompt}.")
        if (prefs.brand.isNotBlank()) appendLine("Account: ${prefs.brand}. Write in its voice.")
        if (extra.isNotBlank()) appendLine("Context from the creator: ${extra.trim()}")
        appendLine("Keep the caption under 300 characters, use emojis only where they feel natural, no hashtags inside the caption text.")
        append(
            if (hashtags) "Return 8-15 relevant hashtags (mix of popular and niche), each a single word without spaces."
            else "Return an empty hashtags list."
        )
    }
}
