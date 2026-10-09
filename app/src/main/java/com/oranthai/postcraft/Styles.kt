package com.oranthai.postcraft

/** A selectable option: [label] is shown in the UI, [prompt] is what we tell the model. */
data class Choice(val label: String, val prompt: String)

object Styles {
    /** Free on-device looks - see [Filters]. */
    val aesthetics = listOf(
        Look("Natural Boost", saturation = 1.15f, contrast = 1.08f, brightness = 4f),
        Look("35mm Film", saturation = 0.9f, contrast = 1.05f, warmth = 0.6f, fade = 0.5f, vignette = 0.25f, grain = 0.5f),
        Look("Cinematic", saturation = 0.9f, contrast = 1.15f, warmth = 0.3f, tint = 0xFF1E6F7A.toInt(), tintAlpha = 0.18f, vignette = 0.3f),
        Look("Clean Minimal", saturation = 0.85f, contrast = 0.95f, brightness = 18f, fade = 0.2f),
        Look("Moody Dark", saturation = 0.7f, contrast = 1.2f, brightness = -18f, vignette = 0.55f),
        Look("Pastel Dream", saturation = 0.75f, contrast = 0.9f, brightness = 20f, tint = 0xFFF4C2D7.toInt(), tintAlpha = 0.2f, fade = 0.35f),
        Look("Golden Hour", saturation = 1.1f, warmth = 1.2f, tint = 0xFFFFB347.toInt(), tintAlpha = 0.15f, vignette = 0.2f),
        Look("Vintage 70s", saturation = 0.8f, warmth = 0.9f, tint = 0xFFD9A066.toInt(), tintAlpha = 0.15f, fade = 0.6f, vignette = 0.35f, grain = 0.35f),
        Look("Luxury Editorial", saturation = 0.95f, contrast = 1.22f, vignette = 0.3f),
        Look("Neon Night", saturation = 1.35f, contrast = 1.15f, tint = 0xFFB026FF.toInt(), tintAlpha = 0.16f, vignette = 0.35f),
        Look("Y2K Flash", saturation = 1.4f, contrast = 1.2f, brightness = 10f),
        Look("Polaroid", saturation = 0.9f, warmth = 0.3f, fade = 0.35f, polaroid = true),
        Look("B&W Classic", saturation = 0f, contrast = 1.25f, grain = 0.4f),
        Look("B&W Soft", saturation = 0f, contrast = 0.95f, fade = 0.4f),
    )

    val tones = listOf(
        Choice("Aesthetic", "short, aesthetic, lowercase-friendly and vibe-driven"),
        Choice("Witty", "witty and playful with a clever hook"),
        Choice("Inspiring", "uplifting and inspirational"),
        Choice("Professional", "professional and brand-appropriate, clear call to action"),
        Choice("Gen-Z", "Gen-Z slang, casual, emoji-friendly, funny"),
        Choice("Poetic", "poetic and evocative, like a short verse"),
        Choice("Storytelling", "a short engaging story behind the moment, ending with a question to drive comments"),
    )

    /** Instagram-friendly aspect ratios. */
    val ratios = listOf(
        Choice("Portrait 4:5", "4:5"),
        Choice("Square 1:1", "1:1"),
        Choice("Story 9:16", "9:16"),
        Choice("Landscape 16:9", "16:9"),
    )
}
