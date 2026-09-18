package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val ArohiDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

/** High-contrast accessibility variant: pure black surfaces, maximum contrast text. */
private val ArohiHighContrastScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF00363A),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF1A1A1A),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFFFD400),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF3A3000),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF101010),
    onSurfaceVariant = Color(0xFFFFFFFF),
    outline = Color(0xFFFFFFFF)
)

@Composable
fun ArohiTheme(
    accent: ArohiAccent = AccentPalettes.cyan,
    highContrast: Boolean = false,
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val scheme = if (highContrast) ArohiHighContrastScheme else ArohiDarkColorScheme
    val density = LocalDensity.current
    val scaledDensity = Density(
        density = density.density,
        fontScale = density.fontScale * fontScale.coerceIn(0.85f, 1.4f)
    )

    CompositionLocalProvider(
        LocalArohiAccent provides accent,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            content = content
        )
    }
}

/** Backward-compatible alias kept for the existing screens. */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    accent: ArohiAccent = AccentPalettes.cyan,
    highContrast: Boolean = false,
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    ArohiTheme(accent = accent, highContrast = highContrast, fontScale = fontScale, content = content)
}
