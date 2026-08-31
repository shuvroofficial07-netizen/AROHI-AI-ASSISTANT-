package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArohiDarkSurface
import com.example.ui.theme.ArohiSurfaceBorder
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.VioletSecondary

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderColor: Color = ArohiSurfaceBorder,
    glowBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    val borderModifier = if (glowBorder) {
        Modifier.border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    CyanPrimary.copy(alpha = 0.5f),
                    VioletSecondary.copy(alpha = 0.35f),
                    Color(0x1AFFFFFF)
                )
            ),
            shape = shape
        )
    } else {
        Modifier.border(1.dp, borderColor, shape)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x14FFFFFF),
                        Color(0x08FFFFFF),
                        ArohiDarkSurface.copy(alpha = 0.7f)
                    )
                )
            )
            .then(borderModifier)
            .then(clickModifier)
            .padding(16.dp),
        content = content
    )
}

