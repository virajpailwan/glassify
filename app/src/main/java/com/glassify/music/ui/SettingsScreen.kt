package com.glassify.music.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassify.music.data.AppSettings
import com.glassify.music.ui.glass.glassSource
import com.glassify.music.ui.glass.rememberGlassBackdrop
import kotlin.math.abs

@Composable
fun SettingsScreen(
    settings: AppSettings,
    padding: PaddingValues,
    songCount: Int,
    onRescan: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Box(Modifier.padding(horizontal = 16.dp)) { GlassPreview() } }

        item {
            SettingsCard("Glass") {
                SliderRow("How glassy", settings.amount) { settings.setAmount(it) }
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val presets = listOf("Solid" to 0f, "Frosted" to 0.35f, "Liquid" to 0.7f, "Crystal" to 1f)
                    presets.forEach { (name, value) ->
                        OptionChip(name, abs(settings.amount - value) < 0.04f, { settings.setAmount(value) })
                    }
                }
            }
        }

        item {
            SettingsCard("Fine tune") {
                SliderRow("Blur", settings.blur) { settings.setBlur(it) }
                SliderRow("Refraction", settings.refraction) { settings.setRefraction(it) }
                SliderRow("Tint opacity", settings.opacity) { settings.setOpacity(it) }
                SliderRow("Edge shine", settings.shine) { settings.setShine(it) }
            }
        }

        item {
            SettingsCard("Appearance") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Album art background", color = Color.White, fontSize = 16.sp)
                        Text(
                            "Blurred cover of the playing song",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                        )
                    }
                    Switch(
                        checked = settings.albumBackground,
                        onCheckedChange = { settings.setAlbumBackground(it) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Accent,
                            checkedThumbColor = Color.White,
                        ),
                    )
                }
            }
        }

        item {
            SettingsCard("Library") {
                Text("Sort songs by", color = Color.White, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Title", "Artist", "Recent").forEachIndexed { index, name ->
                        OptionChip(name, settings.sort == index, { settings.setSort(index) })
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassPillButton("Rescan", Icons.Rounded.Refresh, onRescan)
                    Text("$songCount songs found", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }
        }

        item {
            Text(
                "Glassify 1.0 · local music only · Android 13+",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    GlassBox(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        cornerRadius = 28.dp,
        contentAlignment = Alignment.TopStart,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 15.sp)
            Text("${(value * 100).toInt()}%", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
            ),
        )
    }
}

/** A colourful scene with a glass pill on top, so every slider change is visible live. */
@Composable
private fun GlassPreview() {
    val backdrop = rememberGlassBackdrop()
    Box(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(28.dp)),
    ) {
        Box(Modifier.matchParentSize().glassSource(backdrop)) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Brush.linearGradient(
                        listOf(Color(0xFF3A1C8C), Color(0xFF0F6E78), Color(0xFFB02A6B)),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                )
                val stripeW = size.width / 14f
                for (i in 0 until 14) {
                    drawRect(
                        Color.White.copy(alpha = if (i % 2 == 0) 0.22f else 0.05f),
                        topLeft = Offset(i * stripeW, 0f),
                        size = androidx.compose.ui.geometry.Size(stripeW, size.height),
                    )
                }
                drawCircle(Color(0xFFFFC857), radius = size.height * 0.28f, center = Offset(size.width * 0.18f, size.height * 0.32f))
                drawCircle(Color(0xFF5CE1E6), radius = size.height * 0.2f, center = Offset(size.width * 0.86f, size.height * 0.7f))
            }
        }
        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
            GlassBox(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.72f)
                    .height(84.dp),
                cornerRadius = 42.dp,
            ) {
                Text("Glass preview", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
