package com.example.engine

import java.util.Locale

/**
 * The assistant is instructed to prefix every reply with exactly one emotion tag, e.g.
 * `<emotion>happy</emotion> আজকে তোমার presentation কেমন গেলো?`
 *
 * This parser extracts that tag (used to drive the avatar expression) and removes it from the
 * text that is shown and spoken.
 */
object EmotionTagParser {

    const val ALLOWED_TAGS =
        "neutral, happy, excited, thinking, concerned, laughing, sad, surprised"

    private val tagRegex = Regex("<emotion>\\s*([a-zA-Z_]+)\\s*</emotion>", RegexOption.IGNORE_CASE)
    private val strayTagRegex = Regex("</?emotion[^>]*>", RegexOption.IGNORE_CASE)

    data class Parsed(
        val text: String,
        val emotion: ArohiEmotion?,
        val hadTag: Boolean
    )

    fun parse(raw: String): Parsed {
        if (raw.isBlank()) return Parsed(raw, null, false)
        val match = tagRegex.find(raw)
        val emotion = match?.groupValues?.getOrNull(1)?.let { mapToEmotion(it) }
        val cleaned = raw.replace(tagRegex, " ").replace(strayTagRegex, " ").trim()
        val finalText = if (cleaned.isBlank()) raw.trim() else cleaned
        return Parsed(finalText, emotion, match != null)
    }

    fun mapToEmotion(name: String): ArohiEmotion? = when (name.trim().lowercase(Locale.ROOT)) {
        "neutral" -> ArohiEmotion.CALM
        "happy" -> ArohiEmotion.HAPPY
        "excited" -> ArohiEmotion.EXCITED
        "thinking" -> ArohiEmotion.THINKING
        "concerned" -> ArohiEmotion.CONCERNED
        "laughing" -> ArohiEmotion.PLAYFUL
        "sad" -> ArohiEmotion.SAD
        "surprised" -> ArohiEmotion.CURIOUS
        else -> null
    }

    /** True when the model forgot the tag but the text still deserves an expression. */
    fun needsFallbackInference(hadTag: Boolean): Boolean = !hadTag
}
