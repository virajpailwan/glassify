package com.glassify.music.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassify.music.data.AppStore
import com.glassify.music.data.Song
import com.glassify.music.data.toAlbums
import com.glassify.music.data.toArtists
import com.glassify.music.playback.PlayerController
import com.glassify.music.ui.neu.AccentColor
import com.glassify.music.ui.neu.NeuChip
import com.glassify.music.ui.neu.NeuColors
import com.glassify.music.ui.neu.neuInset
import com.glassify.music.ui.neu.plainClickable

@Composable
fun NeuTextField(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().neuInset(22.dp).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = NeuColors.TextDim, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f).padding(start = 10.dp)) {
            if (value.isEmpty()) Text(hint, color = NeuColors.TextDim, fontSize = 16.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = NeuColors.TextMain, fontSize = 16.sp),
                cursorBrush = SolidColor(AccentColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun SearchScreen(
    songs: List<Song>,
    store: AppStore,
    player: PlayerController,
    onDetail: (Detail) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var menuSong by remember { mutableStateOf<Song?>(null) }
    val q = query.trim()

    val suggestions = remember(songs) { songs.shuffled().take(8) }
    val songHits = remember(songs, q) {
        if (q.isEmpty()) emptyList()
        else songs.filter {
            it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
        }
    }
    val albumHits = remember(songs, q) {
        if (q.isEmpty()) emptyList()
        else songs.toAlbums().filter { it.title.contains(q, true) || it.artist.contains(q, true) }
    }
    val artistHits = remember(songs, q) {
        if (q.isEmpty()) emptyList() else songs.toArtists().filter { it.name.contains(q, true) }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Search")
        NeuTextField(
            value = query,
            onChange = { query = it },
            hint = "Songs, albums, artists",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            onSearch = { store.addSearch(query) },
        )

        LazyColumn(Modifier.weight(1f)) {
            if (q.isEmpty()) {
                if (store.recentSearches.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle("Recent searches", Modifier.weight(1f))
                            Text(
                                "Clear",
                                color = AccentColor,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .plainClickable { store.clearSearches() }
                                    .padding(end = 22.dp, top = 14.dp, bottom = 4.dp),
                            )
                        }
                        LazyRow(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(store.recentSearches) { s -> NeuChip(s, false, { query = s }) }
                        }
                    }
                }
                item { SectionTitle("Suggested for you") }
                songItems(suggestions, player) { menuSong = it }
            } else {
                if (songHits.isEmpty() && albumHits.isEmpty() && artistHits.isEmpty()) {
                    item { EmptyHint("Nothing found for \"$q\".") }
                }
                if (songHits.isNotEmpty()) {
                    item { SectionTitle("Songs") }
                    songItems(songHits.take(25), player, onPlay = { store.addSearch(q) }) { menuSong = it }
                }
                if (albumHits.isNotEmpty()) {
                    item { SectionTitle("Albums") }
                    items(albumHits) { a ->
                        EntryRow(a.title, "${a.artist} · ${a.songs.size} songs", a.songs.first()) {
                            store.addSearch(q)
                            onDetail(Detail.OfAlbum(a.id))
                        }
                    }
                }
                if (artistHits.isNotEmpty()) {
                    item { SectionTitle("Artists") }
                    items(artistHits) { a ->
                        EntryRow(a.name, "${a.songs.size} songs", a.songs.first()) {
                            store.addSearch(q)
                            onDetail(Detail.OfArtist(a.name))
                        }
                    }
                }
            }
        }
    }

    menuSong?.let { SongOptionsDialog(it, store, player, null) { menuSong = null } }
}
