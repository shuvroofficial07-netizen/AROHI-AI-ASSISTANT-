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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.theme.VioletSecondary
import com.example.ui.viewmodel.ArohiViewModel

enum class SmartTaskStatus(val label: String, val color: Color) {
    COMPLETED("Completed", EmeraldSuccess),
    IN_PROGRESS("In Progress", AmberWarning),
    PENDING("Pending", TextMuted)
}

data class SmartTaskItem(
    val id: String,
    val title: String,
    val status: SmartTaskStatus,
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconTint: Color = Color.White,
    val actionPhrase: String = title
)

@Composable
fun SmartTasksScreen(
    viewModel: ArohiViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tasks = remember {
        mutableStateListOf(
            SmartTaskItem(
                id = "1",
                title = "YouTube এ গান চালাও",
                status = SmartTaskStatus.COMPLETED,
                icon = Icons.Default.PlayArrow,
                iconBgColor = EmeraldSuccess.copy(alpha = 0.2f),
                iconTint = EmeraldSuccess
            ),
            SmartTaskItem(
                id = "2",
                title = "Rahim কে মেসেজ পাঠাও",
                status = SmartTaskStatus.IN_PROGRESS,
                icon = Icons.Default.Message,
                iconBgColor = CyanPrimary.copy(alpha = 0.2f),
                iconTint = CyanPrimary
            ),
            SmartTaskItem(
                id = "3",
                title = "আজকের আবহাওয়া বলো",
                status = SmartTaskStatus.PENDING,
                icon = Icons.Default.WbSunny,
                iconBgColor = VioletBright.copy(alpha = 0.2f),
                iconTint = VioletBright
            ),
            SmartTaskItem(
                id = "4",
                title = "Calculator ওপেন করো",
                status = SmartTaskStatus.PENDING,
                icon = Icons.Default.Calculate,
                iconBgColor = Color(0x22FFFFFF),
                iconTint = Color.White
            )
        )
    }

    var showAddTaskDialog by remember { mutableStateOf(false) }

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
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                    onClick = {
                        viewModel.sendUserMessage(task.actionPhrase, isVoice = true)
                    }
                )
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
                        "আরোহী এই কমান্ডটি অটোমেট করবে:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        placeholder = { Text("যেমন: ব্লুটুথ অন করো", color = TextMuted) },
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
                            tasks.add(
                                SmartTaskItem(
                                    id = System.currentTimeMillis().toString(),
                                    title = newTaskTitle,
                                    status = SmartTaskStatus.PENDING,
                                    icon = Icons.Default.PlayArrow,
                                    iconBgColor = VioletBright.copy(alpha = 0.2f),
                                    iconTint = VioletBright
                                )
                            )
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

@Composable
fun SmartTaskCard(
    task: SmartTaskItem,
    onClick: () -> Unit
) {
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
                    .background(task.iconBgColor)
                    .border(1.dp, task.iconTint.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = task.icon,
                    contentDescription = null,
                    tint = task.iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title & Status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = task.status.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = task.status.color
                )
            }

            // Status Trailing Check Badge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when (task.status) {
                            SmartTaskStatus.COMPLETED -> EmeraldSuccess
                            SmartTaskStatus.IN_PROGRESS -> AmberWarning
                            SmartTaskStatus.PENDING -> Color(0x22FFFFFF)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (task.status) {
                        SmartTaskStatus.COMPLETED -> Icons.Default.Check
                        SmartTaskStatus.IN_PROGRESS -> Icons.Default.Check
                        SmartTaskStatus.PENDING -> Icons.Default.HourglassEmpty
                    },
                    contentDescription = null,
                    tint = if (task.status == SmartTaskStatus.PENDING) TextMuted else Color(0xFF020205),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
