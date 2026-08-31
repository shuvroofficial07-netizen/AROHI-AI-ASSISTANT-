package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.service.DiagnosticReport
import com.example.service.DiagnosticStatusLevel
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VioletBright
import com.example.ui.theme.VioletSecondary
import com.example.ui.viewmodel.ArohiViewModel
import kotlinx.coroutines.delay

/**
 * Premium futuristic splash. Every line below the logo is a REAL system
 * check taken from the live diagnostic report — no fake percentages, no
 * simulated progress. Navigation happens once the actual checks complete.
 */
@Composable
fun SplashScreen(
    viewModel: ArohiViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val report by viewModel.diagnosticReport.collectAsState()
    val isChecking by viewModel.isDiagnosticsChecking.collectAsState()
    var minimumTimePassed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.runFullDiagnostics()
        delay(1200)
        minimumTimePassed = true
    }

    // Navigate only when the REAL checks have finished running
    LaunchedEffect(isChecking, minimumTimePassed, report.timestamp) {
        if (minimumTimePassed && !isChecking && report.timestamp > 0L) {
            delay(350)
            onFinish()
        }
    }

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
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
                .padding(horizontal = 28.dp, vertical = 40.dp),
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
                Box(
                    modifier = Modifier.size(170.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = size.minDimension * 0.36f

                        val rayCount = 12
                        for (i in 0 until rayCount) {
                            val rayHeight = (10 + (i % 4) * 7) * pulseAlpha
                            val offsetXLeft = centerX - radius - 10 - (i * 6)
                            val offsetXRight = centerX + radius + 10 + (i * 6)
                            val rayAlpha = (1f - (i.toFloat() / rayCount)) * 0.8f
                            drawLine(
                                color = CyanPrimary.copy(alpha = rayAlpha),
                                start = Offset(offsetXLeft, centerY - rayHeight / 2),
                                end = Offset(offsetXLeft, centerY + rayHeight / 2),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = MagentaAccent.copy(alpha = rayAlpha),
                                start = Offset(offsetXRight, centerY - rayHeight / 2),
                                end = Offset(offsetXRight, centerY + rayHeight / 2),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                        }

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

                        drawCircle(
                            color = CyanPrimary.copy(alpha = 0.2f * pulseAlpha),
                            radius = radius + 8.dp.toPx(),
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    Text(
                        text = "A",
                        fontSize = 52.sp,
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

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Arohi AI Assistant",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = CyanPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "by Shù Vrô",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    color = MagentaAccent,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "NEXT GENERATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    color = TextMuted
                )
            }

            // Bottom: REAL initialization states from the live diagnostic report
            RealInitStatusList(report = report, isChecking = isChecking)
        }
    }
}

@Composable
private fun RealInitStatusList(
    report: DiagnosticReport,
    isChecking: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .background(Color(0x0DFFFFFF))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        val statusLines = buildList {
            add("Initializing Arohi" to if (!isChecking && report.items.isEmpty()) SplashCheck.DONE else SplashCheck.PENDING)
            report.items.forEach { item ->
                val check = when (item.status) {
                    DiagnosticStatusLevel.READY -> SplashCheck.DONE
                    DiagnosticStatusLevel.LIMITED -> SplashCheck.WARN
                    DiagnosticStatusLevel.ERROR -> SplashCheck.ERROR
                }
                add(item.summary to check)
            }
        }

        Column(
            modifier = Modifier
                .height(120.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            statusLines.take(8).forEach { (label, check) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                when (check) {
                                    SplashCheck.DONE -> EmeraldSuccess
                                    SplashCheck.WARN -> Color(0xFFF59E0B)
                                    SplashCheck.ERROR -> MagentaAccent
                                    SplashCheck.PENDING -> Color(0xFF334155)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isChecking) "Running real system checks..." else "System initialization complete",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isChecking) CyanPrimary else EmeraldSuccess
        )
    }
}

private enum class SplashCheck { DONE, WARN, ERROR, PENDING }
