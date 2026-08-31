package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VioletBright
import com.example.ui.theme.VioletSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var currentPhaseText by remember { mutableStateOf("Initializing Arohi v13.97.7...") }

    LaunchedEffect(Unit) {
        val phases = listOf(
            "Initializing Arohi v13.97.7...",
            "Checking voice engine...",
            "Checking Gemini connection...",
            "Checking system permissions...",
            "Starting background operating layer...",
            "Loading local memory & routines...",
            "Checking notification intelligence...",
            "Arohi AI Assistant Ready"
        )
        val steps = 50
        for (i in 1..steps) {
            delay(35)
            progress = i / steps.toFloat()
            val phaseIdx = ((progress * (phases.size - 1)).toInt()).coerceIn(0, phases.size - 1)
            currentPhaseText = phases[phaseIdx]
        }
        delay(300)
        onFinish()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splash_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .clickable { onFinish() }
            .testTag("splash_screen_view"),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .size(360.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                VioletSecondary.copy(alpha = 0.25f),
                                CyanGlow.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension / 1.5f
                        )
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Center Glowing Emblem & Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Circular Neon Emblem with Audio Waveform horizontal rays
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = size.minDimension * 0.36f

                        // Outer waveform rays (horizontal left & right)
                        val rayCount = 14
                        for (i in 0 until rayCount) {
                            val rayHeight = (12 + (i % 5) * 8) * pulseAlpha
                            val offsetXLeft = centerX - radius - 10 - (i * 6)
                            val offsetXRight = centerX + radius + 10 + (i * 6)
                            val rayAlpha = (1f - (i.toFloat() / rayCount)) * 0.8f

                            // Left rays
                            drawLine(
                                color = CyanPrimary.copy(alpha = rayAlpha),
                                start = Offset(offsetXLeft, centerY - rayHeight / 2),
                                end = Offset(offsetXLeft, centerY + rayHeight / 2),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                            // Right rays
                            drawLine(
                                color = MagentaAccent.copy(alpha = rayAlpha),
                                start = Offset(offsetXRight, centerY - rayHeight / 2),
                                end = Offset(offsetXRight, centerY + rayHeight / 2),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                        }

                        // Outer glowing circular ring
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    CyanPrimary,
                                    VioletBright,
                                    MagentaAccent,
                                    CyanPrimary
                                )
                            ),
                            radius = radius,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Outer halo
                        drawCircle(
                            color = CyanPrimary.copy(alpha = 0.2f * pulseAlpha),
                            radius = radius + 8.dp.toPx(),
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Central "A" Logo Symbol
                    Text(
                        text = "A",
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.White,
                        modifier = Modifier.drawBehind {
                            drawCircle(
                                color = CyanPrimary.copy(alpha = 0.35f * pulseAlpha),
                                radius = size.minDimension * 0.8f
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // "Arohi" Gradient Title
                Text(
                    text = "Arohi",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = CyanPrimary
                )

                // "AI Assistant" Subtitle
                Text(
                    text = "AI Assistant",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // "by Shù Vrô" Calligraphic Signature
                Text(
                    text = "by Shù Vrô",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    color = MagentaAccent,
                    letterSpacing = 1.sp
                )
            }

            // Bottom Loading Indicator
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentPhaseText,
                    fontSize = 12.sp,
                    color = CyanPrimary,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar with Cyan/Purple Gradient
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x1AFFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            CyanPrimary,
                                            VioletBright,
                                            MagentaAccent
                                        )
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Centered -- 100% -- indicator
                Text(
                    text = "-- ${(animatedProgress * 100).toInt()}% --",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = CyanPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}
