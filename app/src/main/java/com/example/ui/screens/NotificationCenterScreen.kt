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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NotificationEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.viewmodel.ArohiViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationCenterScreen(
    viewModel: ArohiViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadNotifCount.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val context = LocalContext.current

    // Real data only — no mock seeds. Empty state guides the user to grant access.
    val displayNotifications = notifications
    val unreadNotifications = remember(notifications) { notifications.filter { !it.isRead } }

    // Real AI summary derived from actually captured notifications
    val aiSummaryText = remember(notifications, unreadNotifications) {
        when {
            !diagnostics.isNotificationListenerActive ->
                "বস, Notification Access এখনো বন্ধ। নিচের \"Allow Access\" বাটনে ট্যাপ করে পারমিশন দিন — এরপর সব অ্যাপের নোটিফিকেশন আমি রিয়েল-টাইমে পড়ে জানাব।"
            notifications.isEmpty() ->
                "বস, এখনো কোনো নোটিফিকেশন ক্যাপচার হয়নি। কোনো অ্যাপে নোটিফিকেশন এলে সাথে সাথে এখানে দেখা যাবে।"
            unreadNotifications.isEmpty() ->
                "সব নোটিফিকেশন পড়া হয়ে গেছে। মোট ${notifications.size}টি নোটিফিকেশন সংরক্ষিত আছে।"
            else -> {
                val byApp = unreadNotifications.groupBy { it.appName }
                val appSummary = byApp.entries.take(3).joinToString(", ") { (app, list) ->
                    "$app থেকে ${list.size}টি"
                }
                val latest = unreadNotifications.first()
                "আপনার ${unreadNotifications.size}টি অপঠিত নোটিফিকেশন আছে — $appSummary। সাম্প্রতিকতম: ${latest.appName}: ${latest.title}।"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header: < Notifications  [Filter icon]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    text = "Notifications",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            IconButton(
                onClick = { /* Filter or options */ },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x14FFFFFF))
                    .border(1.dp, Color(0x22FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // AI Summary Glowing Card (Exact to design Screen 4)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x268B5CF6),
                            Color(0x140D1222)
                        )
                    )
                )
                .border(1.dp, Color(0x448B5CF6), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(VioletBright.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = VioletBright,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "AI Summary",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (displayNotifications.isEmpty()) "STANDBY" else "LIVE",
                            fontSize = 11.sp,
                            color = CyanPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = aiSummaryText,
                    fontSize = 12.sp,
                    color = Color(0xFFD1D5DB),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // "Recent (4)" and "Mark all read" row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent (${displayNotifications.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (unreadCount > 0) {
                Text(
                    text = "Mark all read",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VioletBright,
                    modifier = Modifier
                        .clickable { viewModel.clearAllNotifications() }
                        .testTag("mark_all_read_btn")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Notification List (real captured notifications only)
        if (displayNotifications.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "কোনো নোটিফিকেশন ক্যাপচার হয়নি",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (diagnostics.isNotificationListenerActive)
                        "নতুন নোটিফিকেশন এলেই এখানে রিয়েল-টাইমে দেখা যাবে।"
                    else
                        "AROHI-কে নোটিফিকেশন পড়ার অনুমতি দিন:",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (!diagnostics.isNotificationListenerActive) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            context.startActivity(
                                android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletBright),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Allow Access", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayNotifications, key = { it.id }) { item ->
                    NotificationRowCard(
                        notification = item,
                        onSpeak = {
                            viewModel.ttsManager.speak("${item.appName} থেকে নোটিফিকেশন: ${item.title}। ${item.text}")
                        },
                        onMarkRead = { viewModel.markNotificationRead(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationRowCard(
    notification: NotificationEntity,
    onSpeak: () -> Unit,
    onMarkRead: () -> Unit
) {
    // Pick app icon & color based on app name
    val (appIcon, iconColor, iconBg) = when {
        notification.appName.contains("WhatsApp", ignoreCase = true) ->
            Triple(Icons.Default.Chat, Color(0xFF22C55E), Color(0x2222C55E))
        notification.appName.contains("Calendar", ignoreCase = true) ->
            Triple(Icons.Default.CalendarMonth, Color(0xFF3B82F6), Color(0x223B82F6))
        notification.appName.contains("Gmail", ignoreCase = true) || notification.appName.contains("Mail", ignoreCase = true) ->
            Triple(Icons.Default.Mail, Color(0xFFEF4444), Color(0x22EF4444))
        else ->
            Triple(Icons.Default.PhoneAndroid, VioletBright, Color(0x228B5CF6))
    }

    // Time ago string
    val timeAgo = remember(notification.timestamp) {
        val diffMinutes = ((System.currentTimeMillis() - notification.timestamp) / 60000).toInt()
        when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
            else -> "${diffMinutes / 1440}d ago"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x141E293B),
                        Color(0x220D1222)
                    )
                )
            )
            .border(1.dp, Color(0x1A8B5CF6), RoundedCornerShape(14.dp))
            .clickable { onMarkRead() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // App Icon Circle
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconBg)
                    .border(1.dp, iconColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = appIcon,
                    contentDescription = notification.appName,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.appName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = timeAgo,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${notification.title}: ${notification.text}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onSpeak,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Speak notification",
                    tint = CyanPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

