package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * User selectable theme accents ("থিম কালার") plus avatar outfit palettes.
 * The selected accent is provided through [LocalArohiAccent] so every screen can pick it up.
 */
data class ArohiAccent(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

object AccentPalettes {

    val cyan = ArohiAccent("cyan", "সায়ান নিয়ন", CyanPrimary, VioletBright, MagentaAccent)
    val sakura = ArohiAccent("sakura", "সাকুরা পিংক", Color(0xFFFF6EA8), Color(0xFFB14CFF), Color(0xFF4CC9F0))
    val emerald = ArohiAccent("emerald", "এমারেল্ড", Color(0xFF10B981), Color(0xFF22D3EE), Color(0xFFA3E635))
    val amber = ArohiAccent("amber", "অ্যাম্বার সানসেট", Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF22D3EE))
    val midnight = ArohiAccent("midnight", "মিডনাইট", Color(0xFF6C8CFF), Color(0xFF7C3AED), Color(0xFF22D3EE))

    val all = listOf(cyan, sakura, emerald, amber, midnight)

    fun byId(id: String): ArohiAccent = all.firstOrNull { it.id == id } ?: cyan
}

/** Outfit = the palette the avatar body wears. */
data class AvatarOutfit(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color
)

object AvatarOutfits {
    val cyber = AvatarOutfit("cyber", "সাইবার স্যুট", CyanPrimary, VioletSecondary)
    val sakura = AvatarOutfit("sakura", "সাকুরা কিমোনো", Color(0xFFFF6EA8), Color(0xFFB14CFF))
    val midnight = AvatarOutfit("midnight", "মিডনাইট ড্রেস", Color(0xFF4152FF), Color(0xFF7C3AED))
    val ember = AvatarOutfit("ember", "এম্বার আর্মর", Color(0xFFF97316), Color(0xFFEC4899))
    val aurora = AvatarOutfit("aurora", "অরোরা", Color(0xFF10B981), Color(0xFF22D3EE))

    val all = listOf(cyber, sakura, midnight, ember, aurora)

    fun byId(id: String): AvatarOutfit = all.firstOrNull { it.id == id } ?: cyber
}

val LocalArohiAccent = staticCompositionLocalOf { AccentPalettes.cyan }
