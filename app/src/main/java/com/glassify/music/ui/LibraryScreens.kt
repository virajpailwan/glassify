package com.glassify.music.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassify.music.data.AppStore
import com.glassify.music.data.Song
import com.glassify.music.data.toAlbums
import com.glassify.music.data.toArtists
import com.glassify.music.playback.PlayerController
import com.glassify.music.ui.neu.AccentColor
import com.glassify.music.ui.neu.NeuButton
import com.glassify.music.ui.neu.NeuChip
import com.glassify.music.ui.neu.NeuColors
import com.glassify.music.ui.neu.neuInset
import com.glassify.music.ui.neu.plainClickable

sealed interface Detail {
    data class OfAlbum(val id: Long) : Detail
    data class OfArtist(val name: String) : Detail
    data class OfPlaylist(val name: String) : Detail
}

/** Adds one [SongRow] per song; tapping plays the whole [list] from that song. */
fun LazyListScope.songItems(
    list: List<Song>,
    player: PlayerController,
    onPlay: () -> Unit = {},
    onMore: (Song) -> Unit,
) {
    itemsIndexed(list) { i, s ->
        SongRow(
            s,
            s.id == player.currentSong?.id,
            { onPlay(); player.play(list, i) },
            { onMore(s) },
        )
    }
}

@Composable
fun EntryRow(title: String, subtitle: String, art: Song?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().plainClickable(onClick).padding(horizontal = 20.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(54.dp).neuInset(15.dp).padding(3.dp)) {
            ArtImage(art, Modifier.fillMaxSize(), corner = 12.dp, px = 128)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                title,
                color = NeuColors.TextMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = NeuColors.TextDim,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private class Entry(val title: String, val subtitle: String, val art: Song?, val onClick: () -> Unit)

@Composable
fun LibraryScreen(
    songs: List<Song>,
    store: AppStore,
    player: PlayerController,
    onDetail: (Detail) -> Unit,
) {
    var section by remember { mutableIntStateOf(0) }
    var menuSong by remember { mutableStateOf<Song?>(null) }
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    val sorted = remember(songs, store.sort) {
        when (store.sort) {
            1 -> songs.sortedBy { it.artist.lowercase() }
            2 -> songs.sortedByDescending { it.dateAdded }
            else -> songs.sortedBy { it.title.lowercase() }
        }
    }
    val albums = remember(songs) { songs.toAlbums() }
    val artists = remember(songs) { songs.toArtists() }

    val entries: List<Entry> = when (section) {
        0 -> sorted.mapIndexed { i, s -> Entry(s.title, s.artist, s) { player.play(sorted, i) } }
        1 -> albums.map { a ->
            Entry(a.title, "${a.artist} · ${a.songs.size} songs", a.songs.first()) { onDetail(Detail.OfAlbum(a.id)) }
        }
        2 -> artists.map { a ->
            Entry(a.name, "${a.songs.size} songs", a.songs.first()) { onDetail(Detail.OfArtist(a.name)) }
        }
        else -> {
            val byId = songs.associateBy { it.id }
            store.playlists.map { p ->
                Entry(p.name, "${p.ids.size} songs", p.ids.firstNotNullOfOrNull { byId[it] }) {
                    onDetail(Detail.OfPlaylist(p.name))
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Library") {
            if (section == 3) {
                NeuButton(Icons.Rounded.Add, { creating = true }, size = 42.dp, iconSize = 20.dp, description = "New playlist")
            }
            NeuButton(
                if (store.gridView) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                { store.updateGrid(!store.gridView) },
                size = 42.dp,
                iconSize = 20.dp,
                description = "Toggle grid",
            )
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf("Songs", "Albums", "Artists", "Playlists").forEachIndexed { i, t ->
                NeuChip(t, section == i, { section = i })
            }
        }
        if (section == 0) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Sort", color = NeuColors.TextDim, fontSize = 13.sp)
                listOf("Title", "Artist", "Recent").forEachIndexed { i, t ->
                    NeuChip(t, store.sort == i, { store.updateSort(i) })
                }
            }
        }

        Box(Modifier.weight(1f)) {
            if (entries.isEmpty()) {
                EmptyHint(
                    if (section == 3) "No playlists yet. Tap + to create one." else "No music found yet.",
                )
            } else if (store.gridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(entries) { e -> ArtTile(e.art, e.title, e.subtitle, Modifier, e.onClick) }
                }
            } else if (section == 0) {
                LazyColumn(Modifier.fillMaxSize()) {
                    songItems(sorted, player) { menuSong = it }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(entries) { e -> EntryRow(e.title, e.subtitle, e.art, e.onClick) }
                }
            }
        }
    }

    menuSong?.let { SongOptionsDialog(it, store, player, null) { menuSong = null } }

    if (creating) {
        AlertDialog(
            onDismissRequest = { creating = false },
            containerColor = NeuColors.Raised,
            title = { Text("New playlist", color = NeuColors.TextMain) },
            text = { NeuTextField(newName, { newName = it }, "Playlist name") },
            confirmButton = {
                TextButton(onClick = {
                    store.createPlaylist(newName)
                    newName = ""
                    creating = false
                }) { Text("Create", color = AccentColor) }
            },
            dismissButton = {
                TextButton(onClick = { creating = false }) { Text("Cancel", color = NeuColors.TextDim) }
            },
        )
    }
}

@Composable
fun DetailScreen(
    detail: Detail,
    songs: List<Song>,
    store: AppStore,
    player: PlayerController,
    onBack: () -> Unit,
) {
    var menuSong by remember { mutableStateOf<Song?>(null) }

    val title: String
    val list: List<Song>
    val playlist: String? = (detail as? Detail.OfPlaylist)?.name
    when (detail) {
        is Detail.OfAlbum -> {
            val a = songs.toAlbums().firstOrNull { it.id == detail.id }
            title = a?.title ?: "Album"
            list = a?.songs?.sortedBy { it.track } ?: emptyList()
        }
        is Detail.OfArtist -> {
            title = detail.name
            list = songs.filter { it.artist == detail.name }
        }
        is Detail.OfPlaylist -> {
            title = detail.name
            val byId = songs.associateBy { it.id }
            list = store.playlists.firstOrNull { it.name == detail.name }?.ids?.mapNotNull { byId[it] } ?: emptyList()
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title, onBack = onBack) {
            if (playlist != null) {
                NeuButton(
                    Icons.Rounded.Delete,
                    { store.deletePlaylist(playlist); onBack() },
                    size = 42.dp,
                    iconSize = 20.dp,
                    description = "Delete playlist",
                )
            }
            NeuButton(
                Icons.Rounded.Shuffle,
                { if (list.isNotEmpty()) player.shuffleAll(list) },
                size = 42.dp,
                iconSize = 20.dp,
                description = "Shuffle",
            )
            NeuButton(
                Icons.Rounded.PlayArrow,
                { if (list.isNotEmpty()) player.play(list, 0) },
                size = 46.dp,
                accent = true,
                description = "Play all",
            )
        }
        Text(
            "${list.size} songs",
            color = NeuColors.TextDim,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
        )
        if (list.isEmpty()) {
            EmptyHint("Nothing here yet. Use the ⋮ menu on any song to add it to a playlist.")
        }
        LazyColumn(Modifier.weight(1f)) {
            songItems(list, player) { menuSong = it }
        }
    }

    menuSong?.let { SongOptionsDialog(it, store, player, playlist) { menuSong = null } }
}
