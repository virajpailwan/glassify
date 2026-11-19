package com.glassify.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassify.music.playback.PlayerController
import com.glassify.music.ui.neu.AccentColor
import com.glassify.music.ui.neu.NeuColors
import com.glassify.music.ui.neu.neuInset
import com.glassify.music.ui.neu.plainClickable

@Composable
fun QueueScreen(player: PlayerController, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val queue = player.queue
    val current = player.currentIndex

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Queue", onBack = onBack)
        if (queue.isEmpty()) {
            EmptyHint("The queue is empty. Play a song to start.")
        }
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(queue) { i, s ->
                val isCurrent = i == current
                val upcoming = i > current
                Row(
                    Modifier
                        .fillMaxWidth()
                        .plainClickable { player.skipTo(i) }
                        .padding(horizontal = 20.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtImage(s, Modifier.size(48.dp), corner = 12.dp, px = 128)
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(
                            s.title,
                            color = if (isCurrent) AccentColor else if (upcoming) NeuColors.TextMain else NeuColors.TextDim,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if (isCurrent) "Now playing · ${s.artist}" else s.artist,
                            color = NeuColors.TextDim,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (upcoming) {
                        if (i - 1 > current) {
                            QueueIcon(Icons.Rounded.KeyboardArrowUp, "Move up") { player.moveInQueue(i, i - 1) }
                        }
                        if (i < queue.size - 1) {
                            QueueIcon(Icons.Rounded.KeyboardArrowDown, "Move down") { player.moveInQueue(i, i + 1) }
                        }
                        QueueIcon(Icons.Rounded.Close, "Remove") { player.removeFromQueue(i) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Icon(
        icon,
        contentDescription = label,
        tint = NeuColors.TextDim,
        modifier = Modifier.size(38.dp).plainClickable(onClick).padding(8.dp),
    )
}
