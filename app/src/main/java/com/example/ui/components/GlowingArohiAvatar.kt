package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.engine.ArohiEmotion
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.IndigoDeep
import com.example.ui.theme.VioletSecondary
import com.example.voice.SpeechState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlowingArohiAvatar(
    emotion: ArohiEmotion,
    speechState: SpeechState,
    rmsLevel: Float,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AvatarPulse")

    // Slow orbital rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitRotation"
    )

    // Breathing pulse for outer aura
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingPulse"
    )

    // Pulsing inner ring
    val innerPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "InnerPulse"
    )

    // Dynamic color transition based on emotion
    val primaryGlow by animateColorAsState(
        targetValue = if (emotion == ArohiEmotion.IDLE) CyanPrimary else emotion.glowColor,
        animationSpec = tween(500),
        label = "PrimaryGlowColor"
    )
    val secondaryGlow by animateColorAsState(
        targetValue = if (emotion == ArohiEmotion.IDLE) VioletSecondary else emotion.secondaryColor,
        animationSpec = tween(500),
        label = "SecondaryGlowColor"
    )

    // Dynamic scale driven by speech RMS or speaking animation
    val activeScale = when {
        speechState == SpeechState.LISTENING -> 1.0f + (rmsLevel * 0.35f)
        isSpeaking -> breathingPulse * 1.05f
        speechState == SpeechState.PROCESSING -> breathingPulse * 0.98f
        else -> breathingPulse
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = (this.size.minDimension / 2f) * 0.92f * activeScale
            val sphereRadius = (this.size.minDimension / 2f) * 0.58f * activeScale

            // 1. Ambient blurred background glow orbs (Violet left, Cyan right)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryGlow.copy(alpha = 0.22f),
                        secondaryGlow.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - 20.dp.toPx(), center.y - 10.dp.toPx()),
                    radius = outerRadius * 1.25f
                ),
                radius = outerRadius * 1.25f,
                center = Offset(center.x - 20.dp.toPx(), center.y - 10.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.25f),
                        primaryGlow.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(center.x + 25.dp.toPx(), center.y + 10.dp.toPx()),
                    radius = outerRadius * 1.15f
                ),
                radius = outerRadius * 1.15f,
                center = Offset(center.x + 25.dp.toPx(), center.y + 10.dp.toPx())
            )

            // 2. Concentric Outer Pulse Ring (Cyan-500/20 with 2dp stroke)
            drawCircle(
                color = primaryGlow.copy(alpha = 0.25f),
                radius = outerRadius * 0.96f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 3. Concentric Inset Ring (Violet-500/30 with 1.5dp stroke)
            drawCircle(
                color = secondaryGlow.copy(alpha = 0.35f),
                radius = outerRadius * 0.78f * innerPulse,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 4. Rotating energy satellite particles
            rotate(rotationAngle, pivot = center) {
                val particleOffset = Offset(
                    center.x + (outerRadius * 0.88f) * cos(0.0).toFloat(),
                    center.y + (outerRadius * 0.88f) * sin(0.0).toFloat()
                )
                drawCircle(
                    color = primaryGlow,
                    radius = 4.dp.toPx(),
                    center = particleOffset
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = 2.dp.toPx(),
                    center = particleOffset
                )
            }
            rotate(-rotationAngle * 1.2f, pivot = center) {
                val particle2 = Offset(
                    center.x + (outerRadius * 0.78f) * cos(PI.toFloat()),
                    center.y + (outerRadius * 0.78f) * sin(PI.toFloat())
                )
                drawCircle(
                    color = secondaryGlow,
                    radius = 3.5.dp.toPx(),
                    center = particle2
                )
            }

            // 5. Rich Ambient Outer Drop Glow from Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.45f),
                        secondaryGlow.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = sphereRadius * 1.6f
                ),
                radius = sphereRadius * 1.6f,
                center = center
            )

            // 6. Radiant Gradient Sphere: Cyan-400 via Violet-600 to Indigo-900
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryGlow,
                        secondaryGlow,
                        IndigoDeep
                    ),
                    start = Offset(center.x - sphereRadius, center.y - sphereRadius),
                    end = Offset(center.x + sphereRadius, center.y + sphereRadius)
                ),
                radius = sphereRadius,
                center = center
            )

            // Subtle sphere border (white/20)
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = sphereRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 7. Frosted Inner Core (Black/40 with white/15 border)
            val coreRadius = sphereRadius * 0.50f
            drawCircle(
                color = Color(0x73020205),
                radius = coreRadius,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.20f),
                radius = coreRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 8. Center Pure White Glowing Spark (with intense halo)
            val sparkRadius = 6.dp.toPx() * (if (isSpeaking || speechState == SpeechState.LISTENING) 1.25f else 1.0f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryGlow.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = sparkRadius * 2.5f
                ),
                radius = sparkRadius * 2.5f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = sparkRadius,
                center = center
            )
        }
    }
}

