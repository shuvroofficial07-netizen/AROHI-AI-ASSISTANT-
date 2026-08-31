package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MagentaAccent
import kotlin.math.sin

@Composable
fun VoiceWaveform(
    isActive: Boolean,
    rmsLevel: Float,
    modifier: Modifier = Modifier,
    barsCount: Int = 24
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnimation")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val totalWidth = size.width
        val maxHeight = size.height
        val barWidth = (totalWidth / barsCount) * 0.55f
        val gap = (totalWidth - (barWidth * barsCount)) / (barsCount + 1)

        val gradient = Brush.verticalGradient(
            colors = listOf(CyanPrimary, MagentaAccent)
        )

        for (i in 0 until barsCount) {
            val x = gap + i * (barWidth + gap)
            val sinMultiplier = sin(phase + (i * 0.4f)).coerceIn(0.1f, 1.0f)

            val heightFactor = if (isActive) {
                (0.2f + (rmsLevel * 0.8f * sinMultiplier)).coerceIn(0.15f, 1.0f)
            } else {
                0.08f
            }

            val barHeight = maxHeight * heightFactor
            val topY = (maxHeight - barHeight) / 2f

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, topY),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}
