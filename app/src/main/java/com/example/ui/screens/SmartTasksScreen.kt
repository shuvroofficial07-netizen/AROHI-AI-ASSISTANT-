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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TaskLogEntity
import com.example.data.repository.TaskLogRepository
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.theme.VioletSecondary
import com.example.ui.viewmodel.ArohiViewModel

@Composable
fun SmartTasksScreen(
    viewModel: ArohiViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Real tasks persisted in Room database — no fake seeds
    val tasks by viewModel.taskLogs.collectAsState()
    val runningTaskIds by viewModel.runningTaskIds.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    val finishedCount = tasks.count {
        it.status == TaskLogRepository.STATUS_COMPLETED || it.status == TaskLogRepository.STATUS_FAILED
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header with Back Arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
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
                text = "Smart Tasks",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.weight(1f))

            if (finishedCount > 0) {
                Text(
                    text = "Clear finished",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VioletBright,
                    modifier = Modifier
                        .clickable { viewModel.clearFinishedTasks() }
                        .testTag("clear_finished_btn")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (tasks.isEmpty()) {
            // Real empty state — nothing fake
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "কোনো স্মার্ট টাস্ক নেই",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "নিচের \"+ New Task\" বাটনে ট্যাপ করে যেকোনো কমান্ড সেভ করুন —\nযেমন: \"টর্চ অন করো\" বা \"ব্যাটারি স্ট্যাটাস বলো\"।\nট্যাপ করলেই AROHI সত্যিকারের কাজটি করবে।",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            // Tasks List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    SmartTaskCard(
                        task = task,
                        isRunning = runningTaskIds.contains(task.id),
                        onClick = {
                            if (!runningTaskIds.contains(task.id)) {
                                viewModel.runSmartTask(task.id)
                            }
                        },
                        onDelete = { viewModel.deleteSmartTask(task.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Full-width Purple Pill "+ New Task" Button
        Button(
            onClick = { showAddTaskDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            VioletSecondary,
                            VioletBright
                        )
                    )
                )
                .testTag("new_smart_task_btn"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "+ New Task",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showAddTaskDialog) {
        var newTaskTitle by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            containerColor = Color(0xFF0D1222),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("নতুন স্মার্ট টাস্ক যোগ করুন", color = CyanPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "আরোহী এই কমান্ডটি সত্যিকারভাবে সম্পাদন করবে:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        placeholder = { Text("যেমন: ব্যাটারি কত বলো", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color(0x22FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            viewModel.addSmartTask(newTaskTitle)
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("যোগ করুন", color = Color(0xFF020205), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("বাতিল", color = TextMuted)
                }
            }
        )
    }
}

private fun statusLabel(status: String, isRunning: Boolean): String = when {
    isRunning || status == TaskLogRepository.STATUS_EXECUTING -> "Running..."
    status == TaskLogRepository.STATUS_COMPLETED -> "Completed"
    status == TaskLogRepository.STATUS_FAILED -> "Failed"
    else -> "Pending"
}

@Composable
fun SmartTaskCard(
    task: TaskLogEntity,
    isRunning: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when {
        isRunning || task.status == TaskLogRepository.STATUS_EXECUTING -> AmberWarning
        task.status == TaskLogRepository.STATUS_COMPLETED -> EmeraldSuccess
        task.status == TaskLogRepository.STATUS_FAILED -> CrimsonError
        else -> TextMuted
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task Leading Icon inside rounded badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.2f))
                    .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isRunning || task.status == TaskLogRepository.STATUS_EXECUTING -> Icons.Default.Sync
                        task.status == TaskLogRepository.STATUS_COMPLETED -> Icons.Default.Check
                        task.status == TaskLogRepository.STATUS_FAILED -> Icons.Default.ErrorOutline
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title, result & status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.taskName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!task.resultSummary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = task.resultSummary,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = statusLabel(task.status, isRunning),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete task
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Task",
                    tint = MagentaAccent.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Status Trailing Badge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isRunning || task.status == TaskLogRepository.STATUS_EXECUTING -> AmberWarning
                            task.status == TaskLogRepository.STATUS_COMPLETED -> EmeraldSuccess
                            task.status == TaskLogRepository.STATUS_FAILED -> CrimsonError
                            else -> Color(0x22FFFFFF)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isRunning || task.status == TaskLogRepository.STATUS_EXECUTING -> Icons.Default.Sync
                        task.status == TaskLogRepository.STATUS_COMPLETED -> Icons.Default.Check
                        task.status == TaskLogRepository.STATUS_FAILED -> Icons.Default.HourglassEmpty
                        else -> Icons.Default.HourglassEmpty
                    },
                    contentDescription = null,
                    tint = if (task.status == TaskLogRepository.STATUS_PENDING && !isRunning) TextMuted else Color(0xFF020205),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
