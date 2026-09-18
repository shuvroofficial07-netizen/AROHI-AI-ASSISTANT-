package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.ArohiEmotion
import com.example.ui.theme.AccentPalettes
import com.example.ui.theme.AvatarOutfit
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.VioletBright
import com.example.voice.SpeechState

/**
 * The avatar stage: switches between the illustrated 2D portrait and the animated 3D energy
 * orb, applies the user's outfit palette and always keeps the idle animation alive.
 */
@Composable
fun ArohiAvatarStage(
    emotion: ArohiEmotion,
    speechState: SpeechState,
    rmsLevel: Float,
    isSpeaking: Boolean,
    is3D: Boolean,
    outfit: AvatarOutfit,
    modifier: Modifier = Modifier,
    size: Dp = 210.dp,
    onClick: () -> Unit = {}
) {
    if (is3D) {
        GlowingArohiAvatar(
            emotion = emotion,
            speechState = speechState,
            rmsLevel = rmsLevel,
            isSpeaking = isSpeaking,
            modifier = modifier,
            size = size,
            primaryOverride = outfit.primary,
            secondaryOverride = outfit.secondary,
            idleAnimation = true,
            onClick = onClick
        )
    } else {
        Arohi2DPortrait(
            emotion = emotion,
            outfit = outfit,
            modifier = modifier,
            size = size,
            onClick = onClick
        )
    }
}

@Composable
private fun Arohi2DPortrait(
    emotion: ArohiEmotion,
    outfit: AvatarOutfit,
    modifier: Modifier = Modifier,
    size: Dp = 210.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "idle_2d")
    val float by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_float"
    )
    val glow = if (emotion == ArohiEmotion.IDLE) outfit.primary else emotion.glowColor

    Box(
        modifier = modifier
            .size(size)
            .scale(1.0f + float * 0.015f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.95f)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glow.copy(alpha = 0.35f + 0.15f * float),
                                outfit.secondary.copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        ),
                        radius = this.size.minDimension / 1.6f
                    )
                }
        )
        Box(
            modifier = Modifier
                .size(size * 0.88f)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            outfit.primary,
                            outfit.secondary,
                            AccentPalettes.cyan.tertiary,
                            outfit.primary
                        )
                    )
                )
                .padding(3.dp)
                .clip(CircleShape)
        ) {
            Image(
                painter = painterResource(id = R.drawable.arohi_avatar),
                contentDescription = "Arohi Avatar (2D)",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Emotion tint keeps the illustrated portrait expressive.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                glow.copy(alpha = 0.16f),
                                Color.Transparent,
                                outfit.secondary.copy(alpha = 0.18f)
                            )
                        )
                    )
            )
            Image(
                painter = painterResource(id = R.drawable.ic_arohi_logo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(glow.copy(alpha = 0.10f), BlendMode.SrcAtop),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .size(size * 0.88f)
                .clip(CircleShape)
                .border(1.dp, glow.copy(alpha = 0.55f), CircleShape)
        )
    }
}

/** Small reusable pill used across the new screens. */
@Composable
fun AvatarModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) CyanPrimary.copy(alpha = 0.18f) else Color(0x0DFFFFFF))
            .border(
                1.dp,
                if (selected) CyanPrimary.copy(alpha = 0.6f) else Color(0x1AFFFFFF),
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (selected) CyanPrimary else VioletBright
        )
    }
}
