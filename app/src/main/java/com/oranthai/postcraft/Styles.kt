package com.oranthai.postcraft

/** A selectable option: [label] is shown in the UI, [prompt] is what we tell the model. */
data class Choice(val label: String, val prompt: String)

object Styles {
    val aesthetics = listOf(
        Choice("Cinematic", "cinematic movie still, anamorphic lens look, teal-and-orange grade, dramatic lighting, shallow depth of field"),
        Choice("35mm Film", "shot on 35mm Kodak Portra film, natural grain, soft highlights, warm nostalgic tones"),
        Choice("Clean Minimal", "clean minimal aesthetic, lots of negative space, soft neutral palette, bright even light, airy and modern"),
        Choice("Moody Dark", "moody dark aesthetic, deep shadows, desaturated low-key tones, rich blacks, atmospheric"),
        Choice("Pastel Dream", "dreamy pastel aesthetic, soft pinks lilacs and mint, hazy glow, light and whimsical"),
        Choice("Golden Hour", "warm golden hour sunlight, long soft shadows, glowing rim light, sun flare"),
        Choice("Vintage 70s", "1970s vintage photo, faded warm colors, retro textures, slight vignette"),
        Choice("Luxury Editorial", "high-end luxury fashion editorial, glossy magazine lighting, rich contrast, elegant and premium"),
        Choice("Neon Cyberpunk", "neon cyberpunk night aesthetic, magenta and cyan neon glow, reflections, futuristic city vibe"),
        Choice("Anime Art", "hand-painted anime illustration style, vibrant colors, soft cel shading, painterly background"),
        Choice("Watercolor", "delicate watercolor painting, soft bleeding edges, paper texture, gentle colors"),
        Choice("Y2K Flash", "Y2K party aesthetic, direct on-camera flash, high saturation, glossy, playful"),
        Choice("Polaroid", "instant polaroid photo look, slightly washed colors, soft focus, with the white polaroid frame"),
        Choice("B&W Classic", "timeless black and white photography, strong contrast, fine grain, classic portrait feel"),
    )

    val postTypes = listOf(
        Choice("Photo", "Keep it a pure photograph-style image with no added text."),
        Choice("Quote Post", "Add a short, elegant, relevant quote as tasteful typography integrated into the image, perfectly spelled."),
        Choice("Product Ad", "Turn it into a polished product advertisement: hero the main subject, clean studio-quality backdrop, premium lighting, room for a brand feel. No fake logos."),
        Choice("Magazine Cover", "Design it as a stylish magazine cover with a bold masthead title and a couple of short cover lines, perfectly spelled."),
        Choice("Announcement", "Make it an eye-catching announcement graphic with a short bold headline integrated into the design, perfectly spelled."),
        Choice("Moodboard", "Make it an aesthetic moodboard collage built around the subject, with complementary textures, colors and details."),
        Choice("Meme", "Make it a funny, clean meme with short bold caption text at the top, perfectly spelled."),
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

    /** Instagram-friendly aspect ratios supported by the image model. */
    val ratios = listOf(
        Choice("Portrait 4:5", "4:5"),
        Choice("Square 1:1", "1:1"),
        Choice("Story 9:16", "9:16"),
        Choice("Landscape 16:9", "16:9"),
    )
}
