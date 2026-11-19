package com.glassify.music.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.glassify.music.playback.PlayerController
import com.glassify.music.ui.neu.NeuButton
import com.glassify.music.ui.neu.NeuColors
import com.glassify.music.ui.neu.neuRaised
import com.glassify.music.ui.neu.plainClickable

@Composable
fun HomeScreen(
    songs: List<Song>,
    store: AppStore,
    player: PlayerController,
    onDetail: (Detail) -> Unit,
    onSettings: () -> Unit,
) {
    var menuSong by remember { mutableStateOf<Song?>(null) }

    val featured = remember(songs) {
        songs.toAlbums().maxByOrNull { a -> a.songs.maxOf { it.dateAdded } }
    }
    val recents = remember(songs, store.recents) {
        val byId = songs.associateBy { it.id }
        store.recents.mapNotNull { byId[it] }
    }
    val favourites = remember(songs, store.favourites) { songs.filter { it.id in store.favourites } }
    val added = remember(songs) { songs.sortedByDescending { it.dateAdded }.take(12) }
    val tilePad = PaddingValues(horizontal = 20.dp, vertical = 8.dp)

    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), color = NeuColors.TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("What do you want to hear?", color = NeuColors.TextDim, fontSize = 14.sp)
                }
                NeuButton(Icons.Rounded.Person, onSettings, size = 42.dp, iconSize = 20.dp, description = "Profile")
                NeuButton(Icons.Rounded.Settings, onSettings, size = 42.dp, iconSize = 20.dp, description = "Settings")
            }
        }

        if (songs.isEmpty()) {
            item { EmptyHint("No music found yet. Copy some songs to your phone and tap Rescan in Settings.") }
            return@LazyColumn
        }

        if (featured != null) {
            item {
                SectionTitle("Featured album")
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .neuRaised(30.dp, 8.dp)
                        .plainClickable { onDetail(Detail.OfAlbum(featured.id)) }
                        .padding(14.dp),
                ) {
                    ArtImage(featured.songs.first(), Modifier.fillMaxWidth().height(190.dp), corner = 20.dp, px = 600)
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                featured.title,
                                color = NeuColors.TextMain,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${featured.artist} · ${featured.songs.size} songs",
                                color = NeuColors.TextDim,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        NeuButton(
                            Icons.Rounded.PlayArrow,
                            { player.play(featured.songs, 0) },
                            size = 52.dp,
                            accent = true,
                            description = "Play album",
                        )
                    }
                }
            }
        }

        if (recents.isNotEmpty()) {
            item {
                SectionTitle("Recently played")
                LazyRow(contentPadding = tilePad, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(recents) { s ->
                        ArtTile(s, s.title, s.artist, Modifier.width(132.dp)) { player.play(recents, recents.indexOf(s)) }
                    }
                }
            }
        }

        if (favourites.isNotEmpty()) {
            item { SectionTitle("Favourite songs") }
            items(favourites.take(5)) { s ->
                SongRow(
                    s,
                    s.id == player.currentSong?.id,
                    { player.play(favourites, favourites.indexOf(s)) },
                    { menuSong = s },
                )
            }
        }

        item {
            SectionTitle("Recently added")
            LazyRow(contentPadding = tilePad, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(added) { s ->
                    ArtTile(s, s.title, s.artist, Modifier.width(132.dp)) { player.play(added, added.indexOf(s)) }
                }
            }
        }

        item {
            SectionTitle("Your playlists")
            if (store.playlists.isEmpty()) {
                EmptyHint("Create playlists in the Library tab.")
            } else {
                val byId = songs.associateBy { it.id }
                LazyRow(contentPadding = tilePad, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(store.playlists) { p ->
                        val first = p.ids.firstNotNullOfOrNull { byId[it] }
                        ArtTile(first, p.name, "${p.ids.size} songs", Modifier.width(132.dp)) {
                            onDetail(Detail.OfPlaylist(p.name))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    menuSong?.let { SongOptionsDialog(it, store, player, null) { menuSong = null } }
}
