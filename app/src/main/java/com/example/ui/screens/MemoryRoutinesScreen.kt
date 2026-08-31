package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.RoutineEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.ArohiDarkSurface
import com.example.ui.theme.ArohiSurfaceBorder
import com.example.ui.theme.ArohiSurfaceCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.viewmodel.ArohiViewModel

@Composable
fun MemoryRoutinesScreen(
    viewModel: ArohiViewModel,
    modifier: Modifier = Modifier
) {
    val memories by viewModel.memories.collectAsState()
    val routines by viewModel.routines.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var showAddRoutineDialog by remember { mutableStateOf(false) }
    var memoryBeingEdited by remember { mutableStateOf<MemoryEntity?>(null) }
    var routineBeingEdited by remember { mutableStateOf<RoutineEntity?>(null) }
    val context = LocalContext.current

    // Real JSON export via the Android share sheet
    fun exportMemories() {
        val json = viewModel.exportMemories() ?: return
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, json)
            putExtra(Intent.EXTRA_TITLE, "arohi_memories.json")
        }
        try {
            context.startActivity(Intent.createChooser(sendIntent, "Export Arohi Memories"))
        } catch (e: Exception) {
            // Ignored
        }
    }

    // Real JSON import from a user-chosen text file
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                if (!json.isNullOrBlank()) {
                    viewModel.importMemoriesFromJson(json)
                }
            } catch (e: Exception) {
                // Ignored
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "মেমোরি ও রুটিন হাব",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CyanPrimary
        )
        Text(
            text = "দীর্ঘমেয়াদী স্মৃতি ও অটোমেশন রুটিনসমূহ",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Memory center action strip: REMEMBER / EXPORT / IMPORT / CLEAR ALL
        if (selectedTab == 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MemoryActionChip("+ Remember", CyanPrimary) { showAddMemoryDialog = true }
                MemoryActionChip("Export", EmeraldSuccess, enabled = memories.isNotEmpty()) { exportMemories() }
                MemoryActionChip("Import", VioletBright) { importLauncher.launch("text/*") }
                MemoryActionChip("Clear All", MagentaAccent, enabled = memories.isNotEmpty()) { viewModel.clearAllMemories() }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ArohiDarkSurface,
            contentColor = CyanPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyanPrimary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("স্মৃতিভান্ডার (${memories.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("রুটিন (${routines.size})", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                // Memories Tab
                if (memories.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "এখনো কোনো স্মৃতি সংরক্ষিত নেই",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "বলুন \"মনে রাখো আমার জন্মদিন ১৫ আগস্ট\" — আরোহী সেটি সেভ করবে।",
                            fontSize = 11.sp,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(memories, key = { it.id }) { memory ->
                            MemoryItemCard(
                                memory = memory,
                                onEdit = { memoryBeingEdited = memory },
                                onDelete = { viewModel.deleteMemory(memory.id) }
                            )
                        }
                    }
                }
            } else {
                // Routines Tab
                if (routines.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "কোনো রুটিন নেই",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "নতুন রুটিন তৈরি করতে + বাটনে ট্যাপ করুন।",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(routines, key = { it.id }) { routine ->
                            RoutineItemCard(
                                routine = routine,
                                onToggle = { viewModel.toggleRoutine(routine.id, it) },
                                onRun = { viewModel.runRoutine(routine.id) },
                                onEdit = { routineBeingEdited = routine },
                                onDelete = { viewModel.deleteRoutine(routine.id) }
                            )
                        }
                    }
                }
            }

            // FAB to add memory or routine
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddMemoryDialog = true else showAddRoutineDialog = true
                },
                containerColor = CyanPrimary,
                contentColor = ArohiDarkSurface,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_memory_routine_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
        }
    }

    if (showAddMemoryDialog) {
        AddMemoryDialog(
            onDismiss = { showAddMemoryDialog = false },
            onConfirm = { cat, key, value ->
                viewModel.saveMemory(cat, key, value)
                showAddMemoryDialog = false
            }
        )
    }

    memoryBeingEdited?.let { memory ->
        EditMemoryDialog(
            memory = memory,
            onDismiss = { memoryBeingEdited = null },
            onConfirm = { cat, key, value ->
                viewModel.editMemory(memory.id, cat, key, value)
                memoryBeingEdited = null
            }
        )
    }

    routineBeingEdited?.let { routine ->
        EditRoutineDialog(
            routine = routine,
            onDismiss = { routineBeingEdited = null },
            onConfirm = { name, desc, trigger, actions ->
                viewModel.editRoutine(routine.id, name, desc, trigger, actions)
                routineBeingEdited = null
            }
        )
    }

    if (showAddRoutineDialog) {
        AddRoutineDialog(
            onDismiss = { showAddRoutineDialog = false },
            onConfirm = { name, desc, trigger, actions ->
                viewModel.addRoutine(name, desc, trigger, actions, "routine")
                showAddRoutineDialog = false
            }
        )
    }
}

@Composable
private fun MemoryActionChip(
    label: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tint.copy(alpha = if (enabled) 0.12f else 0.05f))
            .border(1.dp, tint.copy(alpha = if (enabled) 0.4f else 0.12f), RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) tint else TextMuted
        )
    }
}

@Composable
fun MemoryItemCard(
    memory: MemoryEntity,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = memory.key,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ArohiDarkSurface)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = memory.category,
                            fontSize = 10.sp,
                            color = VioletBright,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = memory.value,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit memory",
                    tint = CyanPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete memory",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun RoutineItemCard(
    routine: RoutineEntity,
    onToggle: (Boolean) -> Unit,
    onRun: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = routine.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Switch(
                    checked = routine.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldSuccess,
                        checkedTrackColor = ArohiSurfaceCard,
                        uncheckedTrackColor = ArohiDarkSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = routine.description,
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ভয়েস ট্রিগার: \"${routine.triggerPhrase}\"",
                fontSize = 11.sp,
                color = CyanPrimary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit routine",
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Button(
                    onClick = onRun,
                    colors = ButtonDefaults.buttonColors(containerColor = ArohiDarkSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("রান করুন (Execute)", fontSize = 11.sp, color = CyanPrimary)
                }
            }
        }
    }
}

@Composable
fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (category: String, key: String, value: String) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("PROFILE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArohiSurfaceCard,
        title = { Text("নতুন স্মৃতি সংরক্ষণ (Save Memory)", color = CyanPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("কী / শিরোনাম (Key)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("বিবরণ / তথ্য (Value)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (key.isNotBlank() && value.isNotBlank()) {
                        onConfirm(category, key, value)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("সেভ করুন", color = ArohiDarkSurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = TextMuted)
            }
        }
    )
}

@Composable
fun AddRoutineDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String, trigger: String, actions: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var trigger by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArohiSurfaceCard,
        title = { Text("নতুন অটোমেশন রুটিন তৈরি", color = CyanPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("রুটিনের নাম (Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("সংক্ষিপ্ত বিবরণ (Description)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    label = { Text("ভয়েস ট্রিগার বাক্য (Voice Trigger)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && trigger.isNotBlank()) {
                        onConfirm(name, desc, trigger, "[\"readDeviceState\"]")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("তৈরি করুন", color = ArohiDarkSurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = TextMuted)
            }
        }
    )
}

@Composable
fun EditMemoryDialog(
    memory: MemoryEntity,
    onDismiss: () -> Unit,
    onConfirm: (category: String, key: String, value: String) -> Unit
) {
    var key by remember(memory.id) { mutableStateOf(memory.key) }
    var value by remember(memory.id) { mutableStateOf(memory.value) }
    var category by remember(memory.id) { mutableStateOf(memory.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArohiSurfaceCard,
        title = { Text("স্মৃতি সম্পাদনা (Edit Memory)", color = CyanPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("বিভাগ (Category)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("কী / শিরোনাম (Key)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("বিবরণ / তথ্য (Value)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (key.isNotBlank() && value.isNotBlank()) {
                        onConfirm(category, key, value)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("সেভ করুন", color = ArohiDarkSurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = TextMuted)
            }
        }
    )
}

@Composable
fun EditRoutineDialog(
    routine: RoutineEntity,
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String, trigger: String, actions: String) -> Unit
) {
    var name by remember(routine.id) { mutableStateOf(routine.name) }
    var desc by remember(routine.id) { mutableStateOf(routine.description) }
    var trigger by remember(routine.id) { mutableStateOf(routine.triggerPhrase) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArohiSurfaceCard,
        title = { Text("রুটিন সম্পাদনা (Edit Routine)", color = CyanPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("রুটিনের নাম (Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("সংক্ষিপ্ত বিবরণ (Description)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    label = { Text("ভয়েস ট্রিগার বাক্য (Voice Trigger)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && trigger.isNotBlank()) {
                        onConfirm(name, desc, trigger, routine.actionsJson)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("সেভ করুন", color = ArohiDarkSurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = TextMuted)
            }
        }
    )
}
