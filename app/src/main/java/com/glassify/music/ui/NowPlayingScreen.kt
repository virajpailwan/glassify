package com.glassify.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.glassify.music.playback.PlayerController
import com.glassify.music.ui.glass.glassSource
import com.glassify.music.ui.glass.rememberGlassBackdrop

@Composable
fun NowPlayingScreen(player: PlayerController, onClose: () -> Unit) {
    val song = player.currentSong
    val backdrop = rememberGlassBackdrop()
    var dragTotal by remember { mutableFloatStateOf(0f) }
    var scrubbing by remember { mutableStateOf<Float?>(null) }

    // Registered after the library's back handler, so it wins while the player is open.
    BackHandler(onBack = onClose)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = {
                        if (dragTotal > 250f) onClose()
                        dragTotal = 0f
                    },
                    onVerticalDrag = { _, dy -> dragTotal += dy },
                )
            },
    ) {
        // What the glass looks through: blurred art + the big cover + titles.
        Box(Modifier.fillMaxSize().glassSource(backdrop)) {
            AppBackground(song, useArt = true)
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(64.dp))
                ArtImage(
                    song,
                    Modifier.weight(1f, fill = false).aspectRatio(1f),
                    corner = 32.dp,
                    px = 800,
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    song?.title ?: "Nothing playing",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    song?.artist ?: "",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(268.dp))
            }
        }

        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                GlassIconButton(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                    size = 48.dp,
                    description = "Close",
                )

                GlassBox(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    cornerRadius = 36.dp,
                    contentAlignment = Alignment.TopStart,
                ) {
                    Column(
                        Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val duration = player.durationMs.coerceAtLeast(1L).toFloat()
                        Slider(
                            value = (scrubbing ?: player.positionMs.toFloat()).coerceIn(0f, duration),
                            onValueChange = { scrubbing = it },
                            onValueChangeFinished = {
                                scrubbing?.let { player.seekTo(it.toLong()) }
                                scrubbing = null
                            },
                            valueRange = 0f..duration,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                            ),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                formatTime((scrubbing ?: player.positionMs.toFloat()).toLong()),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                            )
                            Text(
                                formatTime(player.durationMs),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (player.shuffle) Accent else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(26.dp).clickable { player.toggleShuffle() },
                            )
                            GlassIconButton(Icons.Rounded.SkipPrevious, { player.previous() }, size = 56.dp, description = "Previous")
                            GlassIconButton(
                                icon = if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                onClick = { player.togglePlay() },
                                size = 76.dp,
                                iconSize = 38.dp,
                                description = "Play or pause",
                            )
                            GlassIconButton(Icons.Rounded.SkipNext, { player.next() }, size = 56.dp, description = "Next")
                            Icon(
                                if (player.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                contentDescription = "Repeat",
                                tint = if (player.repeatMode == Player.REPEAT_MODE_OFF) Color.White.copy(alpha = 0.6f) else Accent,
                                modifier = Modifier.size(26.dp).clickable { player.cycleRepeat() },
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}
