package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ArohiEmotion
import com.example.engine.BrainPhase
import com.example.engine.SystemEvent
import com.example.engine.SystemEventLevel
import com.example.engine.TaskStep
import com.example.engine.TaskStepStatus
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.viewmodel.ArohiViewModel

/**
 * AROHI BRAIN — system-level view of the REAL processing pipeline
 * (understand → context → plan → execute → verify). Shows the live task card
 * and the genuine event stream. Never displays Gemini's private
 * chain-of-thought.
 */
@Composable
fun BrainScreen(
    viewModel: ArohiViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val phase by viewModel.brainPhase.collectAsState()
    val task by viewModel.taskProgress.collectAsState()
    val emotion by viewModel.emotion.collectAsState()
    val events by viewModel.systemEvents.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    val phasesInOrder = listOf(
        BrainPhase.UNDERSTANDING,
        BrainPhase.CHECKING_CONTEXT,
        BrainPhase.PLANNING_ACTION,
        BrainPhase.EXECUTING,
        BrainPhase.VERIFYING,
        BrainPhase.RESPONDING
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x14FFFFFF))
                    .border(1.dp, Color(0x22FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "AROHI BRAIN",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isProcessing) AmberWarning else EmeraldSuccess)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isProcessing) "ACTIVE" else "STANDBY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isProcessing) AmberWarning else EmeraldSuccess
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Emotion + current phase hero
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = emotion.glowColor.copy(alpha = 0.45f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(emotion.glowColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = emotion.glowColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = emotion.bengaliLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = emotion.glowColor
                    )
                    Text(
                        text = "Pipeline phase: ${phase.label}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Pipeline stages
        Text(
            text = "PROCESSING PIPELINE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            phasesInOrder.forEach { pipelinePhase ->
                val state: PipelineState = when {
                    phase == pipelinePhase -> PipelineState.ACTIVE
                    phase == BrainPhase.DONE && pipelinePhase.ordinal <= BrainPhase.VERIFYING.ordinal -> PipelineState.DONE
                    phase == BrainPhase.ERROR && pipelinePhase == BrainPhase.EXECUTING -> PipelineState.ERROR
                    else -> PipelineState.PENDING
                }
                PipelineRow(pipelinePhase.label, state)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live task card — real steps from the task engine
        Text(
            text = "TASK EXECUTION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (task.isRunning || task.steps.isNotEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (task.overallStatus == TaskStepStatus.COMPLETED) EmeraldSuccess.copy(alpha = 0.5f)
                else if (task.overallStatus == TaskStepStatus.FAILED) MagentaAccent.copy(alpha = 0.5f)
                else AmberWarning.copy(alpha = 0.5f)
            ) {
                Column {
                    Text(
                        text = if (task.isRunning) "TASK IN PROGRESS" else
                            if (task.overallStatus == TaskStepStatus.COMPLETED) "TASK COMPLETED"
                            else "TASK FAILED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (task.isRunning) AmberWarning
                        else if (task.overallStatus == TaskStepStatus.COMPLETED) EmeraldSuccess
                        else MagentaAccent
                    )
                    if (task.taskName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.taskName,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    task.steps.forEach { step ->
                        TaskStepRow(step)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        } else {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No active task. Send AROHI a voice command like \"YouTube খোলো\" or run a Smart Task — real execution steps will appear here.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real-time system event stream
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SYSTEM EVENTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            if (events.isNotEmpty()) {
                Text(
                    text = "Clear",
                    fontSize = 10.sp,
                    color = VioletBright,
                    modifier = Modifier.clickable { viewModel.clearSystemEvents() }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (events.isEmpty()) {
            Text(
                text = "No events yet — every entry here is a real system occurrence.",
                fontSize = 11.sp,
                color = TextMuted
            )
        } else {
            events.take(30).forEach { event ->
                EventRow(event)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private enum class PipelineState { PENDING, ACTIVE, DONE, ERROR }

@Composable
private fun PipelineRow(label: String, state: PipelineState) {
    val color = when (state) {
        PipelineState.ACTIVE -> AmberWarning
        PipelineState.DONE -> EmeraldSuccess
        PipelineState.ERROR -> MagentaAccent
        PipelineState.PENDING -> Color(0xFF334155)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = if (state == PipelineState.PENDING) 0.06f else 0.12f))
            .border(1.dp, color.copy(alpha = if (state == PipelineState.PENDING) 0.15f else 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (state == PipelineState.ACTIVE) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = if (state == PipelineState.PENDING) TextMuted else color,
            modifier = Modifier.weight(1f)
        )
        if (state == PipelineState.ACTIVE) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                tint = AmberWarning,
                modifier = Modifier.size(13.dp)
            )
        } else if (state == PipelineState.DONE) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = EmeraldSuccess,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun TaskStepRow(step: TaskStep) {
    val (icon, color) = when (step.status) {
        TaskStepStatus.COMPLETED -> Icons.Default.Check to EmeraldSuccess
        TaskStepStatus.FAILED -> Icons.Default.Close to MagentaAccent
        TaskStepStatus.RUNNING -> Icons.Default.Sync to AmberWarning
        TaskStepStatus.PENDING -> Icons.Default.Close to TextMuted
    }
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(14.dp)
                .padding(top = 1.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "${step.order}. ${step.description}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            if (step.detail.isNotBlank()) {
                Text(
                    text = step.detail,
                    fontSize = 10.sp,
                    color = TextMuted,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun EventRow(event: SystemEvent) {
    val color = when (event.level) {
        SystemEventLevel.SUCCESS -> EmeraldSuccess
        SystemEventLevel.WARNING -> AmberWarning
        SystemEventLevel.ERROR -> MagentaAccent
        SystemEventLevel.INFO -> CyanPrimary
    }
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .padding(top = 4.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = event.message,
                fontSize = 11.sp,
                color = Color.White
            )
            Text(
                text = "${event.component} • ${event.formattedTime()}",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}
