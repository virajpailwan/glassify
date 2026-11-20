package com.glassify.music.ui

import android.content.Context
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import com.glassify.music.ui.neu.NeuChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.glassify.music.data.AppStore
import com.glassify.music.playback.PlayerController
import com.glassify.music.ui.neu.AccentColor
import com.glassify.music.ui.neu.NeuButton
import com.glassify.music.ui.neu.NeuColors
import com.glassify.music.ui.neu.NeuSlider
import com.glassify.music.ui.neu.Visualizer
import com.glassify.music.ui.neu.neuInset
import com.glassify.music.ui.neu.neuRaised
import kotlin.math.roundToInt

@Composable
fun NowPlayingScreen(
    player: PlayerController,
    store: AppStore,
    onClose: () -> Unit,
    onQueue: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val song = player.currentSong
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volume by remember {
        mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    var showSleep by remember { mutableStateOf(false) }
    val artScale by animateFloatAsState(if (player.isPlaying) 1f else 0.93f, tween(400), label = "art")

    Box(
        Modifier
            .fillMaxSize()
            .background(NeuColors.Bg)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
            val room = maxHeight - 440.dp
            val frame = (if (maxWidth - 16.dp < room) maxWidth - 16.dp else room).coerceAtLeast(160.dp)

            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                // top bar
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    NeuButton(Icons.Rounded.KeyboardArrowDown, onClose, size = 42.dp, description = "Close")
                    Text(
                        "NOW PLAYING",
                        color = NeuColors.TextDim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp,
                        modifier = Modifier.weight(1f),
                    )
                    NeuButton(Icons.Rounded.QueueMusic, onQueue, size = 42.dp, description = "Queue")
                }

                Spacer(Modifier.weight(1f))

                // artwork in a raised plate with a recessed well
                Box(
                    Modifier
                        .padding(10.dp)
                        .size(frame)
                        .graphicsLayer {
                            scaleX = artScale
                            scaleY = artScale
                        }
                        .neuRaised(36.dp, 8.dp)
                        .padding(14.dp),
                ) {
                    Box(Modifier.fillMaxSize().neuInset(26.dp).padding(8.dp)) {
                        Crossfade(targetState = song, animationSpec = tween(350), label = "cover") { sg ->
                            ArtImage(sg, Modifier.fillMaxSize(), corner = 20.dp, px = 800)
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // title + favourite
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(start = 6.dp)) {
                        Text(
                            song?.title ?: "Nothing playing",
                            color = NeuColors.TextMain,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            song?.artist ?: "",
                            color = NeuColors.TextDim,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val fav = song != null && song.id in store.favourites
                    val pop = remember { Animatable(1f) }
                    LaunchedEffect(fav) {
                        pop.animateTo(1.3f, tween(90))
                        pop.animateTo(1f, spring())
                    }
                    NeuButton(
                        modifier = Modifier.graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                        },
                        icon = if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        onClick = { song?.let { store.toggleFavourite(it.id) } },
                        size = 42.dp,
                        active = fav,
                        description = "Favourite",
                    )
                }

                // progress
                val duration = player.durationMs.coerceAtLeast(1L)
                val fraction = scrubbing ?: (player.positionMs.toFloat() / duration)
                val elapsed = (fraction * duration).toLong()
                NeuSlider(
                    value = fraction,
                    onChange = { scrubbing = it },
                    onFinished = {
                        scrubbing?.let { player.seekTo((it * duration).toLong()) }
                        scrubbing = null
                    },
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatTime(elapsed), color = NeuColors.TextDim, fontSize = 12.sp)
                    Text("-" + formatTime(duration - elapsed), color = NeuColors.TextDim, fontSize = 12.sp)
                }

                // transport controls
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeuButton(
                        Icons.Rounded.Shuffle,
                        { player.toggleShuffle() },
                        size = 44.dp,
                        iconSize = 20.dp,
                        active = player.shuffle,
                        description = "Shuffle",
                    )
                    NeuButton(Icons.Rounded.SkipPrevious, { player.previous() }, size = 54.dp, description = "Previous")
                    NeuButton(
                        icon = if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        onClick = { player.togglePlay() },
                        size = 80.dp,
                        iconSize = 38.dp,
                        accent = true,
                        description = "Play or pause",
                    )
                    NeuButton(Icons.Rounded.SkipNext, { player.next() }, size = 54.dp, description = "Next")
                    NeuButton(
                        if (player.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        { player.cycleRepeat() },
                        size = 44.dp,
                        iconSize = 20.dp,
                        active = player.repeatMode != Player.REPEAT_MODE_OFF,
                        description = "Repeat",
                    )
                }

                // volume
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.VolumeDown, null, tint = NeuColors.TextDim, modifier = Modifier.size(20.dp))
                    Box(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        NeuSlider(
                            value = volume,
                            onChange = {
                                volume = it
                                audio.setStreamVolume(AudioManager.STREAM_MUSIC, (it * maxVolume).roundToInt(), 0)
                            },
                        )
                    }
                    Icon(Icons.Rounded.VolumeUp, null, tint = NeuColors.TextDim, modifier = Modifier.size(20.dp))
                }

                // lyrics / equalizer / queue
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeuButton(
                        Icons.Rounded.Bedtime,
                        { showSleep = true },
                        size = 42.dp,
                        iconSize = 20.dp,
                        active = player.sleepMinutes > 0,
                        description = "Sleep timer",
                    )
                    NeuButton(Icons.Rounded.QueueMusic, onQueue, size = 42.dp, iconSize = 20.dp, description = "Queue")
                }
            }
        }
    }

    if (showSleep) {
        AlertDialog(
            onDismissRequest = { showSleep = false },
            containerColor = NeuColors.Raised,
            title = { Text("Sleep timer", color = NeuColors.TextMain) },
            text = {
                Column {
                    Text(
                        if (player.sleepMinutes > 0) "Music will pause in ${player.sleepMinutes} min." else "Pause the music after:",
                        color = NeuColors.TextDim,
                        fontSize = 14.sp,
                    )
                    Row(
                        Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        listOf(0, 15, 30, 45, 60).forEach { m ->
                            NeuChip(
                                if (m == 0) "Off" else "$m",
                                player.sleepMinutes == m,
                                { player.updateSleepTimer(m); showSleep = false },
                            )
                        }
                    }
                    Text("minutes", color = NeuColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleep = false }) { Text("Close", color = AccentColor) }
            },
        )
    }
}
