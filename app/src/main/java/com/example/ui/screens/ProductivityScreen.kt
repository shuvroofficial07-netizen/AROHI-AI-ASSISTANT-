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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ReminderRepeat
import com.example.device.ReminderScheduler
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.viewmodel.ArohiViewModel

/**
 * Real productivity hub: reminders/alarms, to-dos and notes — everything here writes to the
 * Room database and (for reminders) to AlarmManager.
 */
@Composable
fun ProductivityScreen(
    viewModel: ArohiViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val reminders by viewModel.reminders.collectAsState()
    val todos by viewModel.todos.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("রিমাইন্ডার", "টু-ডু", "নোট")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "PRODUCTIVITY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
                color = CyanPrimary
            )
            Text(
                text = "রিমাইন্ডার · কাজ · নোট",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusMessage ?: "",
                    fontSize = 11.sp,
                    color = EmeraldSuccess,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { viewModel.dismissStatus() }
                )
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF020205),
            contentColor = CyanPrimary,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyanPrimary
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            color = if (selectedTab == index) CyanPrimary else TextMuted
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
        ) {
            if (selectedTab == 0) {
                item {
                    var title by remember { mutableStateOf("") }
                    var timeText by remember { mutableStateOf("") }
                    var repeatRule by remember { mutableStateOf(ReminderRepeat.NONE) }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "নতুন রিমাইন্ডার",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("কী মনে করিয়ে দেব?", color = TextSecondary) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reminder_title_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = darkFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = timeText,
                                onValueChange = { timeText = it },
                                label = { Text("কখন? (যেমন: সন্ধ্যা ৭টায়)", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = darkFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ReminderRepeat.all.forEach { rule ->
                                    val selected = repeatRule == rule
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(
                                                if (selected) CyanPrimary.copy(alpha = 0.2f)
                                                else Color(0x0DFFFFFF)
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) CyanPrimary.copy(alpha = 0.6f) else Color(0x1AFFFFFF),
                                                RoundedCornerShape(999.dp)
                                            )
                                            .clickable { repeatRule = rule }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = ReminderRepeat.bengaliLabel(rule),
                                            fontSize = 10.sp,
                                            color = if (selected) CyanPrimary else TextMuted
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    viewModel.addReminder(title, timeText, repeatRule)
                                    title = ""
                                    timeText = ""
                                    repeatRule = ReminderRepeat.NONE
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_reminder_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF020205))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("রিমাইন্ডার যোগ করুন", color = Color(0xFF020205), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (reminders.isEmpty()) {
                    item { EmptyHint("কোনো রিমাইন্ডার নেই — উপরে যোগ করুন বা আরোহীকে বলে দিন।") }
                }
                items(reminders, key = { it.id }) { reminder ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = if (reminder.isCompleted) TextMuted else MagentaAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reminder.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (reminder.isCompleted) TextMuted else Color.White
                                )
                                Text(
                                    text = "${
                                        ReminderScheduler.formatBengaliDateTime(reminder.triggerAt)
                                    } · ${ReminderRepeat.bengaliLabel(reminder.repeatRule)}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            IconButton(onClick = { viewModel.toggleReminderDone(reminder.id, !reminder.isCompleted) }) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done",
                                    tint = if (reminder.isCompleted) EmeraldSuccess else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.deleteReminder(reminder.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (selectedTab == 1) {
                item {
                    var todoTitle by remember { mutableStateOf("") }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "নতুন কাজ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = todoTitle,
                                onValueChange = { todoTitle = it },
                                label = { Text("কী করতে হবে?", color = TextSecondary) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("todo_title_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = darkFieldColors()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    viewModel.addTodo(todoTitle)
                                    todoTitle = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VioletBright),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_todo_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("লিস্টে যোগ করুন", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (todos.isEmpty()) {
                    item { EmptyHint("টু-ডু লিস্ট খালি।") }
                }
                items(todos, key = { it.id }) { todo ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = todo.isDone,
                                onCheckedChange = { viewModel.toggleTodo(todo.id) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = todo.title,
                                    fontSize = 13.sp,
                                    color = if (todo.isDone) TextMuted else Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                if (todo.priority > 1) {
                                    Text("জরুরি", fontSize = 10.sp, color = MagentaAccent)
                                }
                                todo.dueAt?.let { due ->
                                    Text(
                                        text = "সময়: ${ReminderScheduler.formatBengaliDateTime(due)}",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteTodo(todo.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                if (todos.any { it.isDone }) {
                    item {
                        Button(
                            onClick = { viewModel.clearCompletedTodos() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("সম্পন্ন কাজগুলো মুছুন", fontSize = 12.sp, color = CyanPrimary)
                        }
                    }
                }
            }

            if (selectedTab == 2) {
                item {
                    var noteTitle by remember { mutableStateOf("") }
                    var noteContent by remember { mutableStateOf("") }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "নতুন নোট",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = noteTitle,
                                onValueChange = { noteTitle = it },
                                label = { Text("শিরোনাম", color = TextSecondary) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("note_title_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = darkFieldColors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = noteContent,
                                onValueChange = { noteContent = it },
                                label = { Text("নোট", color = TextSecondary) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("note_content_field"),
                                maxLines = 4,
                                shape = RoundedCornerShape(12.dp),
                                colors = darkFieldColors()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    viewModel.addNote(noteTitle, noteContent)
                                    noteTitle = ""
                                    noteContent = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_note_button")
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFF020205))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("নোট সেভ করুন", color = Color(0xFF020205), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (notes.isEmpty()) {
                    item { EmptyHint("কোনো নোট নেই।") }
                }
                items(notes, key = { it.id }) { note ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(text = note.content, fontSize = 12.sp, color = TextSecondary)
                            }
                            IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0AFFFFFF))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(14.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 12.sp, color = TextMuted)
    }
}

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanPrimary,
    unfocusedBorderColor = Color(0x1AFFFFFF),
    focusedContainerColor = Color(0x0DFFFFFF),
    unfocusedContainerColor = Color(0x0DFFFFFF),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
