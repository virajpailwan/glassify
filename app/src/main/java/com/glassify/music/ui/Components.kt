package com.glassify.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassify.music.ui.glass.GlassBackdrop
import com.glassify.music.ui.glass.GlassStyle
import com.glassify.music.ui.glass.glass

val Accent = Color(0xFFB9AAFF)
val GlassTint = Color(0xFF14141C)

val LocalGlassBackdrop = compositionLocalOf<GlassBackdrop?> { null }
val LocalGlassStyle = compositionLocalOf { GlassStyle.Default }

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

/** A container drawn as liquid glass using the nearest [LocalGlassBackdrop]. */
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop = LocalGlassBackdrop.current
    val style = LocalGlassStyle.current
    val glassModifier = if (backdrop != null) {
        Modifier.glass(backdrop, cornerRadius, style, GlassTint)
    } else {
        Modifier.background(Color(0xCC15151D), RoundedCornerShape(cornerRadius))
    }
    Box(modifier.then(glassModifier), contentAlignment = contentAlignment, content = content)
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 26.dp,
    description: String? = null,
) {
    GlassBox(
        modifier = modifier.size(size).clip(CircleShape).clickable(onClick = onClick),
        cornerRadius = size,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun GlassPillButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassBox(
        modifier = modifier.height(46.dp).clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        cornerRadius = 50.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Cheap, flat chip (no glass) used for presets and options. */
@Composable
fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) Color(0xFF14141C) else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
