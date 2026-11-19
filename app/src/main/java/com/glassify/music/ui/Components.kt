package com.glassify.music.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassify.music.data.AppStore
import com.glassify.music.data.Song
import com.glassify.music.playback.PlayerController
import com.glassify.music.ui.neu.AccentColor
import com.glassify.music.ui.neu.NeuButton
import com.glassify.music.ui.neu.NeuColors
import com.glassify.music.ui.neu.neuInset
import com.glassify.music.ui.neu.neuRaised
import com.glassify.music.ui.neu.plainClickable

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            NeuButton(Icons.AutoMirrored.Rounded.ArrowBack, onBack, size = 42.dp, iconSize = 20.dp, description = "Back")
        } else {
            Spacer(Modifier.width(10.dp))
        }
        Text(
            title,
            color = NeuColors.TextMain,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 6.dp),
        )
        trailing()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = NeuColors.TextMain,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 4.dp),
    )
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = NeuColors.TextDim,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 18.dp),
    )
}

@Composable
fun SongRow(song: Song, playing: Boolean, onClick: () -> Unit, onMore: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .plainClickable(onClick)
            .padding(horizontal = 20.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(54.dp).neuInset(15.dp).padding(3.dp)) {
            ArtImage(song, Modifier.fillMaxSize(), corner = 12.dp, px = 128)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (playing) AccentColor else NeuColors.TextMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artist,
                color = NeuColors.TextDim,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(formatTime(song.durationMs), color = NeuColors.TextDim, fontSize = 12.sp)
        Icon(
            Icons.Rounded.MoreVert,
            contentDescription = "More",
            tint = NeuColors.TextDim,
            modifier = Modifier.size(40.dp).plainClickable(onMore).padding(9.dp),
        )
    }
}

/** Square raised cover tile with title below. Width comes from [modifier]. */
@Composable
fun ArtTile(
    song: Song?,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(modifier.plainClickable(onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).neuRaised(20.dp, 5.dp).padding(7.dp)) {
            ArtImage(song, Modifier.fillMaxSize(), corner = 14.dp, px = 360)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            color = NeuColors.TextMain,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            subtitle,
            color = NeuColors.TextDim,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OptionRow(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = NeuColors.TextMain,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth().plainClickable(onClick).padding(vertical = 13.dp),
    )
}

@Composable
fun SongOptionsDialog(
    song: Song,
    store: AppStore,
    player: PlayerController,
    playlistName: String?,
    onDismiss: () -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeuColors.Raised,
        title = {
            Text(
                song.title,
                color = NeuColors.TextMain,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column {
                if (!picking) {
                    OptionRow("Play next") { player.playNext(song); onDismiss() }
                    OptionRow("Add to queue") { player.addToQueue(song); onDismiss() }
                    OptionRow(if (song.id in store.favourites) "Remove from favourites" else "Add to favourites") {
                        store.toggleFavourite(song.id)
                        onDismiss()
                    }
                    OptionRow("Add to playlist") { picking = true }
                    if (playlistName != null) {
                        OptionRow("Remove from this playlist") {
                            store.removeFromPlaylist(playlistName, song.id)
                            onDismiss()
                        }
                    }
                } else {
                    if (store.playlists.isEmpty()) {
                        Text("No playlists yet. Create one in Library.", color = NeuColors.TextDim, fontSize = 14.sp)
                    }
                    store.playlists.forEach { p ->
                        OptionRow(p.name) {
                            store.addToPlaylist(p.name, song.id)
                            onDismiss()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = AccentColor) }
        },
    )
}
