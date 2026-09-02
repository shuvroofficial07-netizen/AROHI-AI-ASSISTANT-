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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.capability.CapabilityStatus
import com.example.core.capability.CapabilityStatusEntry
import com.example.ui.theme.ArohiDarkSurface
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ArohiViewModel

/**
 * Dynamic Capability Discovery screen (spec §76/§77). Instead of a fixed
 * feature list it enumerates the CapabilityRegistry and colors each item by its
 * REAL runtime status read from the device — available / needs permission /
 * needs service / unsupported. Nothing here is hard-coded or faked.
 */
@Composable
fun CapabilitiesScreen(
    viewModel: ArohiViewModel,
    onBack: () -> Unit
) {
    // Re-evaluate each time the screen is composed/opened.
    var entries by remember { mutableStateOf(viewModel.capabilities()) }

    val grouped = entries.groupBy { it.descriptor.category }
    val available = entries.count { it.usable }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06070D))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CyanPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Capability Discovery",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$available of ${entries.size} capabilities ready on this device",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendDot(EmeraldSuccess, "Ready")
            LegendDot(CyanPrimary, "Permission/Service")
            LegendDot(MagentaAccent, "Unsupported")
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            grouped.forEach { (category, list) ->
                item(key = "header_${category.name}") {
                    Text(
                        text = category.name.replace('_', ' ').lowercase()
                            .replaceFirstChar { it.uppercase() },
                        color = CyanPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                    )
                }
                items(list, key = { it.descriptor.id }) { entry ->
                    CapabilityCard(entry)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(label, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun CapabilityCard(entry: CapabilityStatusEntry) {
    val (color, label) = when (entry.status) {
        CapabilityStatus.AVAILABLE -> EmeraldSuccess to "Available"
        CapabilityStatus.REQUIRES_PERMISSION -> CyanPrimary to "Needs permission"
        CapabilityStatus.REQUIRES_SERVICE -> CyanPrimary to "Needs service"
        CapabilityStatus.DISABLED -> TextMuted to "Disabled"
        CapabilityStatus.UNSUPPORTED -> MagentaAccent to "Unsupported"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ArohiDarkSurface.copy(alpha = 0.75f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                entry.descriptor.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            entry.descriptor.description,
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            entry.detail,
            color = TextMuted,
            fontSize = 11.sp
        )
        if (entry.descriptor.voiceCommands.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Try: " + entry.descriptor.voiceCommands.joinToString("  •  "),
                color = CyanPrimary.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}
