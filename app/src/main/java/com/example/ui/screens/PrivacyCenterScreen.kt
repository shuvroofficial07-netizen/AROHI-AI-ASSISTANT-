package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.viewmodel.ArohiViewModel

/**
 * Privacy & data control centre: offline fallback, at-rest encryption, export, cloud sync
 * and the full local data wipe — everything the user is entitled to control.
 */
@Composable
fun PrivacyCenterScreen(
    viewModel: ArohiViewModel,
    modifier: Modifier = Modifier
) {
    val privateMode by viewModel.privateModeFlow.collectAsState()
    val offlineFallback by viewModel.offlineFallbackFlow.collectAsState()
    val encryption by viewModel.encryptionFlow.collectAsState()
    val cloudSync by viewModel.cloudSyncFlow.collectAsState()
    val cloudStatus by viewModel.cloudSyncStatus.collectAsState()
    val lastExport by viewModel.lastExportPath.collectAsState()
    val autoMemory by viewModel.autoMemoryFlow.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "PRIVACY & DATA",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.5.sp,
            color = CyanPrimary
        )
        Text(
            text = "প্রাইভেসি ও ডেটা কন্ট্রোল",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (statusMessage != null) {
            Text(
                text = statusMessage ?: "",
                fontSize = 11.sp,
                color = EmeraldSuccess,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Header(Icons.Default.Security, "প্রাইভেসি নিয়ন্ত্রণ")
                Spacer(modifier = Modifier.height(8.dp))
                ToggleLine(
                    title = "প্রাইভেট মোড (ক্লাউড বন্ধ)",
                    subtitle = "সব উত্তর লোকাল ইঞ্জিন থেকে আসবে, কিছুই বাইরে যাবে না",
                    checked = privateMode,
                    onCheckedChange = { viewModel.setPrivateMode(it) }
                )
                ToggleLine(
                    title = "অফলাইন ফলব্যাক",
                    subtitle = "নেট না থাকলে প্রিসেট বাংলা উত্তর দেবে",
                    checked = offlineFallback,
                    onCheckedChange = { viewModel.setOfflineFallbackEnabled(it) }
                )
                ToggleLine(
                    title = "লোকাল স্টোরেজ এনক্রিপশন",
                    subtitle = "Android Keystore (AES-256/GCM) দিয়ে সংবেদনশীল ডেটা সুরক্ষিত",
                    checked = encryption,
                    onCheckedChange = { viewModel.setLocalEncryptionEnabled(it) },
                    modifier = Modifier.testTag("encryption_switch")
                )
                ToggleLine(
                    title = "অটো মেমোরি",
                    subtitle = "\"আমার নাম...\", \"আমার প্রিয় রং...\" — নিজে থেকে মনে রাখবে",
                    checked = autoMemory,
                    onCheckedChange = { viewModel.setAutoMemoryEnabled(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Header(Icons.Default.Memory, "ডেটা এক্সপোর্ট ও ডিলিট")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "তোমার মেসেজ, মেমোরি, রিমাইন্ডার, টু-ডু, নোট — সব নিজের হাতে। JSON ফাইল হিসেবে এক্সপোর্ট করে রাখতে বা শেয়ার করতে পারো।",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.exportUserData(share = false) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_data_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF020205), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("এক্সপোর্ট", color = Color(0xFF020205), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { viewModel.shareLastExport() },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VioletBright.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VioletBright),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("শেয়ার", fontSize = 11.sp)
                    }
                }
                if (lastExport != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "সর্বশেষ ফাইল: ${lastExport?.substringAfterLast('/')}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.deleteAllUserData() },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_data_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CrimsonError, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সব লোকাল ডেটা মুছে ফেলুন", color = CrimsonError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Header(Icons.Default.Cloud, "Firebase ক্লাউড সিঙ্ক (ঐচ্ছিক)")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "google-services.json যোগ করা থাকলে তোমার নিজের ডেটা তোমার Firebase অ্যাকাউন্টে ব্যাকআপ হবে। ডিফল্টে বন্ধ।",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                ToggleLine(
                    title = "ক্লাউড সিঙ্ক চালু",
                    subtitle = "টগল বন্ধ থাকলে কোনো ডেটা ক্লাউডে যাবে না",
                    checked = cloudSync,
                    onCheckedChange = { viewModel.setCloudSyncEnabled(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.syncToCloud() },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ব্যাকআপ", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.restoreFromCloud() },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("রিস্টোর", fontSize = 11.sp)
                    }
                }
                if (cloudStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = cloudStatus ?: "", fontSize = 11.sp, color = MagentaAccent)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun Header(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun ToggleLine(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Text(text = subtitle, fontSize = 10.sp, color = TextMuted, lineHeight = 14.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
