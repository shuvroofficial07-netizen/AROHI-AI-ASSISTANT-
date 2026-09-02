package com.example.core.personality

/**
 * The selectable personality traits Arohi can blend (spec §35). These shape
 * wording and response style only — they are stylistic states, NOT claims of
 * real emotion.
 */
enum class PersonalityTrait(val displayName: String) {
    WARM("Warm"), CARING("Caring"), INTELLIGENT("Intelligent"), CALM("Calm"),
    PLAYFUL("Playful"), CONFIDENT("Confident"), WITTY("Witty"),
    PROFESSIONAL("Professional"), SERIOUS("Serious"), CURIOUS("Curious"),
    ENCOURAGING("Encouraging")
}

/** Detected conversational/emotional context (spec §36). Never used for medical diagnosis. */
enum class EmotionalContext { NEUTRAL, FRUSTRATED, EXCITED, SAD, URGENT, CONFUSED, HAPPY, ANXIOUS }

/**
 * Pure-Kotlin personality + emotional-context engine. It restyles system
 * phrasing according to the active traits and detects the user's tone so the
 * response style can adapt. It never fabricates feelings — it only adjusts
 * wording, and explicitly does not perform any psychological/medical diagnosis.
 */
class PersonalityEngine(
    activeTraits: Set<PersonalityTrait> = setOf(PersonalityTrait.WARM, PersonalityTrait.CARING, PersonalityTrait.WITTY)
) {
    var activeTraits: Set<PersonalityTrait> = activeTraits
        private set

    fun setTraits(traits: Set<PersonalityTrait>) {
        activeTraits = if (traits.isEmpty()) setOf(PersonalityTrait.WARM) else traits
    }

    fun has(trait: PersonalityTrait): Boolean = trait in activeTraits

    /**
     * Produces a short style directive appended to prompts / used by the local
     * phrasing layer. Kept deterministic so tests are stable.
     */
    fun styleDirective(): String {
        val parts = mutableListOf<String>()
        if (has(PersonalityTrait.WARM)) parts += "warm and affectionate"
        if (has(PersonalityTrait.CARING)) parts += "caring and supportive"
        if (has(PersonalityTrait.WITTY)) parts += "lightly witty"
        if (has(PersonalityTrait.PLAYFUL)) parts += "playful"
        if (has(PersonalityTrait.CALM)) parts += "calm and measured"
        if (has(PersonalityTrait.CONFIDENT)) parts += "confident and decisive"
        if (has(PersonalityTrait.PROFESSIONAL)) parts += "professional and concise"
        if (has(PersonalityTrait.SERIOUS)) parts += "serious and direct"
        if (has(PersonalityTrait.CURIOUS)) parts += "curious and inquisitive"
        if (has(PersonalityTrait.ENCOURAGING)) parts += "encouraging"
        if (has(PersonalityTrait.INTELLIGENT)) parts += "clear and insightful"
        return if (parts.isEmpty()) "neutral" else parts.joinToString(", ")
    }

    /**
     * Detects the user's emotional context from their message text. This is a
     * lightweight heuristic for tone adaptation only (spec §36) — it is NOT a
     * medical or psychological assessment.
     */
    fun detectContext(input: String): EmotionalContext {
        val t = input.lowercase()
        var frustrated = 0
        var excited = 0
        var sad = 0
        var urgent = 0
        var confused = 0
        var happy = 0
        var anxious = 0

        // English / Banglish cues
        listOf("damn", "stupid", "hate", "why isn't", "not working", "useless", "fed up",
            "বিরক্ত", "বাজে", "কাজ করছে না", "পারছি না", "হচ্ছে না", "গ্রাস", "বিরক্তিকর")
            .forEach { if (t.contains(it)) frustrated++ }
        listOf("!", "wow", "awesome", "great", "yay", "দারুণ", "চমৎকার", "খুব ভালো", "অসাধারণ", "যাক্")
            .forEach { if (t.contains(it)) excited++ }
        listOf("sad", "depressed", "unhappy", "দুঃখ", "কষ্ট", "মন খারাপ", "কাঁদ", "একা")
            .forEach { if (t.contains(it)) sad++ }
        listOf("urgent", "asap", "now", "hurry", "emergency", "জরুরি", "এখনই", "তাড়াতাড়ি", "দ্রুত", "এখন দরকার")
            .forEach { if (t.contains(it)) urgent++ }
        listOf("confused", "don't understand", "how do i", "what do you mean", "বুঝছি না", "বুঝতে পারছি না", "কীভাবে", "কিভাবে")
            .forEach { if (t.contains(it)) confused++ }
        listOf("happy", "love it", "thank", "thanks", "ধন্যবাদ", "ভালোবাসি", "খুশি", "থ্যাংকস")
            .forEach { if (t.contains(it)) happy++ }
        listOf("worried", "anxious", "scared", "tension", "চিন্তা", "ভয়", "টেনশন", "শঙ্কা")
            .forEach { if (t.contains(it)) anxious++ }

        return listOf(
            EmotionalContext.FRUSTRATED to frustrated,
            EmotionalContext.EXCITED to excited,
            EmotionalContext.SAD to sad,
            EmotionalContext.URGENT to urgent,
            EmotionalContext.CONFUSED to confused,
            EmotionalContext.HAPPY to happy,
            EmotionalContext.ANXIOUS to anxious
        ).filter { it.second > 0 }.maxByOrNull { it.second }?.first ?: EmotionalContext.NEUTRAL
    }

    /**
     * Given an emotional context, returns a short adaptive opener. When the user
     * seems distressed the assistant softens; when urgent, it stays efficient.
     */
    fun adaptivePrefix(context: EmotionalContext, callUser: String = "বস"): String = when (context) {
        EmotionalContext.FRUSTRATED -> "শান্ত থাকুন $callUser, আমি দেখছি। "
        EmotionalContext.URGENT -> "এখনই করছি, $callUser। "
        EmotionalContext.SAD -> "আমি আছি তো, $callUser। "
        EmotionalContext.CONFUSED -> "আচ্ছা, ধীরে বলুন তো — "
        EmotionalContext.EXCITED -> "দারুণ! "
        EmotionalContext.HAPPY -> if (has(PersonalityTrait.WITTY)) "হাহা, " else ""
        EmotionalContext.ANXIOUS -> "চিন্তা করবেন না, $callUser, "
        EmotionalContext.NEUTRAL -> ""
    }
}
