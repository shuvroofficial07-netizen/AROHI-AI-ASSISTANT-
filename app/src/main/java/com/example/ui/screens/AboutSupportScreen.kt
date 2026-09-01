package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.viewmodel.ArohiViewModel

/**
 * SUPPORT CENTER & ABOUT — "Arohi AI Assistant by Shù Vrô".
 * All three support channels open the REAL deep links, with honest fallbacks
 * when the target app is not installed.
 */
@Composable
fun AboutSupportScreen(
    viewModel: ArohiViewModel,
    onBack: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val report by viewModel.diagnosticReport.collectAsState()

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
                text = "About & Support",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Branding hero
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(CyanPrimary, VioletBright, MagentaAccent, CyanPrimary)
                        )
                    )
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF070A14)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Arohi AI Assistant",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "by Shù Vrô",
                fontSize = 15.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                color = MagentaAccent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "AROHI AI ASSISTANT — NEXT GENERATION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = CyanPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = EmeraldSuccess
            )
            Text(
                text = "AI provider: Google Gemini (REST) • Android operating layer",
                fontSize = 10.sp,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SUPPORT CENTER
        Text(
            text = "SUPPORT CENTER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))

        SupportChannelRow(
            icon = Icons.Default.Facebook,
            tint = Color(0xFF1877F2),
            title = "Facebook",
            subtitle = "Shù Vrô",
            onClick = { openUrl(context, "https://www.facebook.com/shuvromridha77") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        SupportChannelRow(
            icon = Icons.Default.Send,
            tint = Color(0xFF22C55E),
            title = "WhatsApp Support",
            subtitle = "+880 1915 551 436",
            onClick = { openWhatsApp(context, "8801915551436") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        SupportChannelRow(
            icon = Icons.Default.Send,
            tint = Color(0xFF26A5E4),
            title = "Telegram Support",
            subtitle = "@Shuvrojr07",
            onClick = { openTelegram(context, "Shuvrojr07") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "SYSTEM",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))

        val overall = report.overallStatus
        val (statusColor, statusLabel) = when (overall) {
            com.example.service.DiagnosticStatusLevel.READY -> EmeraldSuccess to "ALL SYSTEMS READY"
            com.example.service.DiagnosticStatusLevel.LIMITED -> Color(0xFFF59E0B) to "SYSTEM LIMITED"
            com.example.service.DiagnosticStatusLevel.ERROR -> MagentaAccent to "SYSTEM ERROR"
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x0DFFFFFF))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                .clickable(onClick = onNavigateToDiagnostics)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = statusLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = statusColor,
                modifier = Modifier.weight(1f)
            )
            Text("Diagnostics →", fontSize = 11.sp, color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x0DFFFFFF))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                .clickable(onClick = onNavigateToPermissions)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = VioletBright,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Permissions",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Text("Open →", fontSize = 11.sp, color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x0DFFFFFF))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PrivacyTip,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Privacy",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "API keys stay on your device. Camera and microphone activate only on your explicit action.",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Made with passion by Shù Vrô (Shuvro)\nshuvroofficial07@gmail.com",
            fontSize = 10.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SupportChannelRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x141E293B), Color(0x220D1222))
                )
            )
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Icon(
            imageVector = Icons.Default.Help,
            contentDescription = "Open support",
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Opens the real Facebook profile — browser deep link with fallback. */
private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // No handler — nothing fake happens
    }
}

/** WhatsApp deep link; falls back to the browser only if WhatsApp is missing. */
private fun openWhatsApp(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone")).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        openUrl(context, "https://wa.me/$phone")
    }
}

/** Telegram deep link; falls back to the web profile if Telegram is missing. */
private fun openTelegram(context: Context, username: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$username")).apply {
            setPackage("org.telegram.messenger")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        openUrl(context, "https://t.me/$username")
    }
}
