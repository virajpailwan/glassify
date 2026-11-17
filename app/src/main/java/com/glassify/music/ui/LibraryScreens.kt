package com.glassify.music.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassify.music.data.AlbumInfo
import com.glassify.music.data.ArtistInfo
import com.glassify.music.data.Song

@Composable
fun SongRow(song: Song, playing: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtImage(song, Modifier.size(52.dp), corner = 12.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (playing) Accent else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artist,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(formatTime(song.durationMs), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    GlassBox(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(48.dp),
        cornerRadius = 24.dp,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search your music", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Rounded.Close,
                    null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp).clickable { onQuery("") },
                )
            }
        }
    }
}

@Composable
fun SongsScreen(
    songs: List<Song>,
    current: Song?,
    padding: PaddingValues,
    query: String,
    onQuery: (String) -> Unit,
    sort: Int,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
) {
    val shown = remember(songs, query, sort) {
        val filtered = if (query.isBlank()) songs else songs.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        }
        when (sort) {
            1 -> filtered.sortedBy { it.artist.lowercase() }
            2 -> filtered.sortedByDescending { it.dateAdded }
            else -> filtered.sortedBy { it.title.lowercase() }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item { SearchField(query, onQuery) }
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                GlassPillButton("Shuffle all", Icons.Rounded.Shuffle, { onShuffleAll(shown) })
            }
        }
        items(shown, key = { it.id }) { song ->
            SongRow(song, playing = song.id == current?.id) {
                onPlay(shown, shown.indexOf(song))
            }
        }
    }
}

@Composable
fun AlbumsScreen(albums: List<AlbumInfo>, padding: PaddingValues, onOpen: (AlbumInfo) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        gridItems(albums, key = { it.id }) { album ->
            Column(Modifier.clickable { onOpen(album) }) {
                ArtImage(
                    album.songs.firstOrNull(),
                    Modifier.fillMaxWidth().aspectRatio(1f),
                    corner = 18.dp,
                    px = 400,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    album.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    album.artist,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ArtistsScreen(artists: List<ArtistInfo>, padding: PaddingValues, onOpen: (ArtistInfo) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        items(artists, key = { it.name }) { artist ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(artist) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtImage(artist.songs.firstOrNull(), Modifier.size(56.dp), corner = 28.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        artist.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${artist.songs.size} songs",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

/** Songs of one album or one artist. */
@Composable
fun DetailScreen(
    title: String,
    subtitle: String,
    songs: List<Song>,
    current: Song?,
    padding: PaddingValues,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ArtImage(songs.firstOrNull(), Modifier.size(220.dp), corner = 28.dp, px = 600)
                Spacer(Modifier.height(16.dp))
                Text(
                    title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(subtitle, color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassPillButton("Play", Icons.Rounded.PlayArrow, { onPlay(songs, 0) })
                    GlassPillButton("Shuffle", Icons.Rounded.Shuffle, { onShuffle(songs) })
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        items(songs, key = { it.id }) { song ->
            SongRow(song, playing = song.id == current?.id) {
                onPlay(songs, songs.indexOf(song))
            }
        }
    }
}
