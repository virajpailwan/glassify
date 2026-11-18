package com.glassify.music.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.glassify.music.data.AlbumInfo
import com.glassify.music.data.AppSettings
import com.glassify.music.data.ArtistInfo
import com.glassify.music.data.MusicRepository
import com.glassify.music.data.Song
import com.glassify.music.data.toAlbums
import com.glassify.music.data.toArtists
import com.glassify.music.playback.PlayerController
import com.glassify.music.ui.glass.LocalGlassActive
import com.glassify.music.ui.glass.glassSource
import com.glassify.music.ui.glass.glassStyleOf
import com.glassify.music.ui.glass.rememberGlassBackdrop
import kotlinx.coroutines.delay

enum class Tab(val label: String, val icon: ImageVector) {
    Songs("Songs", Icons.Rounded.MusicNote),
    Albums("Albums", Icons.Rounded.Album),
    Artists("Artists", Icons.Rounded.Person),
    Settings("Glass", Icons.Rounded.Settings),
}

private sealed interface Detail {
    data class OfAlbum(val album: AlbumInfo) : Detail
    data class OfArtist(val artist: ArtistInfo) : Detail
}

private fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

@Composable
fun GlassifyApp() {
    val context = LocalContext.current.applicationContext
    val settings = remember { AppSettings(context) }
    val player = remember { PlayerController(context) }

    DisposableEffect(Unit) {
        player.connect()
        onDispose { player.release() }
    }

    var granted by remember { mutableStateOf(hasAudioPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted = hasAudioPermission(context) }
    LaunchedEffect(Unit) {
        if (!granted) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS),
            )
        }
    }

    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(granted, reload) {
        if (granted) {
            loading = true
            songs = MusicRepository.loadSongs(context)
            loading = false
        }
    }

    // Keep the seek bar moving while music plays.
    LaunchedEffect(player.isPlaying) {
        while (player.isPlaying) {
            player.refreshPosition()
            delay(250)
        }
    }

    var showPlayer by remember { mutableStateOf(false) }

    // While the player fully covers the library, stop drawing the library's glass (saves frames).
    var playerCovering by remember { mutableStateOf(false) }
    LaunchedEffect(showPlayer) {
        if (showPlayer) {
            delay(400)
            playerCovering = true
        } else {
            playerCovering = false
        }
    }

    val style = glassStyleOf(settings.blur, settings.refraction, settings.opacity, settings.shine, settings.quality)

    MaterialTheme(colorScheme = darkColorScheme(primary = Accent)) {
        CompositionLocalProvider(LocalGlassStyle provides style) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                if (!granted) {
                    PermissionScreen(
                        onAllow = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS),
                            )
                        },
                        onOpenSettings = {
                            context.startActivity(
                                Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null),
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        },
                    )
                } else {
                    CompositionLocalProvider(LocalGlassActive provides !playerCovering) {
                        MainContent(
                            settings = settings,
                            player = player,
                            songs = songs,
                            loading = loading,
                            onRescan = { reload++ },
                            onOpenPlayer = { showPlayer = true },
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showPlayer,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                ) {
                    NowPlayingScreen(player = player, onClose = { showPlayer = false })
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    settings: AppSettings,
    player: PlayerController,
    songs: List<Song>,
    loading: Boolean,
    onRescan: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val backdrop = rememberGlassBackdrop()
    var tab by rememberSaveable { mutableStateOf(Tab.Songs) }
    var detail by remember { mutableStateOf<Detail?>(null) }
    var query by remember { mutableStateOf("") }

    val albums = remember(songs) { songs.toAlbums() }
    val artists = remember(songs) { songs.toArtists() }

    var tabsOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = tabsOpen) { tabsOpen = false }
    BackHandler(enabled = detail != null && !tabsOpen) { detail = null }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val current = player.currentSong
    val dockHeight = 66.dp
    val listPadding = PaddingValues(
        top = statusTop + 72.dp,
        bottom = navBottom + dockHeight + 28.dp,
    )

    val onPlay: (List<Song>, Int) -> Unit = { list, index -> player.play(list, index) }
    val onShuffle: (List<Song>) -> Unit = { list -> player.shuffleAll(list) }

    Box(Modifier.fillMaxSize()) {
        // Everything inside this box is what the glass looks through.
        Box(Modifier.fillMaxSize().glassSource(backdrop)) {
            AppBackground(current, settings.albumBackground)

            val d = detail
            when {
                d is Detail.OfAlbum -> DetailScreen(
                    d.album.title, d.album.artist, d.album.songs, current, listPadding, onPlay, onShuffle,
                )
                d is Detail.OfArtist -> DetailScreen(
                    d.artist.name, "${d.artist.songs.size} songs", d.artist.songs, current, listPadding, onPlay, onShuffle,
                )
                tab == Tab.Songs -> SongsScreen(
                    songs, current, listPadding, query, { query = it }, settings.sort, onPlay, onShuffle,
                )
                tab == Tab.Albums -> AlbumsScreen(albums, listPadding) { detail = Detail.OfAlbum(it) }
                tab == Tab.Artists -> ArtistsScreen(artists, listPadding) { detail = Detail.OfArtist(it) }
                else -> SettingsScreen(settings, listPadding, songs.size, onRescan)
            }

            if (songs.isEmpty() && !loading && tab != Tab.Settings && detail == null) {
                Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No music found on this phone yet.\nAdd some songs and tap Rescan in the Glass tab.",
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                    )
                }
            }
        }

        // Glass UI floating on top.
        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
            val title = when (val d = detail) {
                is Detail.OfAlbum -> d.album.title
                is Detail.OfArtist -> d.artist.name
                null -> when (tab) {
                    Tab.Songs -> "Songs"
                    Tab.Albums -> "Albums"
                    Tab.Artists -> "Artists"
                    Tab.Settings -> "Glass settings"
                }
            }
            TopBar(
                title = title,
                showBack = detail != null,
                onBack = { detail = null },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusTop + 8.dp, start = 16.dp, end = 16.dp),
            )

            // Convx-style dock: [tab button] [mini player] [search button].
            // Tap the tab button to open the full tab bar. With nothing playing, the tab bar shows.
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = navBottom + 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (current == null || tabsOpen) {
                    Box(Modifier.weight(1f)) {
                        TabBar(selected = tab, onSelect = {
                            tab = it
                            detail = null
                            tabsOpen = false
                        })
                    }
                } else {
                    GlassIconButton(
                        icon = tab.icon,
                        onClick = { tabsOpen = true },
                        size = 58.dp,
                        description = tab.label,
                        tint = Accent,
                    )
                    Box(Modifier.weight(1f)) { MiniPlayer(player, current, onOpenPlayer) }
                    GlassIconButton(
                        icon = Icons.Rounded.Search,
                        onClick = {
                            tab = Tab.Songs
                            detail = null
                        },
                        size = 58.dp,
                        description = "Search",
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String, showBack: Boolean, onBack: () -> Unit, modifier: Modifier = Modifier) {
    GlassBox(modifier.fillMaxWidth().height(52.dp), cornerRadius = 26.dp) {
        if (showBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .padding(8.dp),
            )
        }
        Text(
            title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 56.dp),
        )
    }
}

@Composable
private fun MiniPlayer(player: PlayerController, song: Song, onOpen: () -> Unit) {
    GlassBox(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .clickable(onClick = onOpen),
        cornerRadius = 29.dp,
    ) {
        Row(
            Modifier.fillMaxSize().padding(start = 7.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtImage(song, Modifier.size(44.dp), corner = 12.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artist,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = "Play or pause",
                tint = Color.White,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable { player.togglePlay() }
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun TabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    GlassBox(Modifier.fillMaxWidth().height(64.dp), cornerRadius = 32.dp) {
        Row(Modifier.fillMaxSize().padding(6.dp)) {
            Tab.entries.forEach { t ->
                val isSelected = t == selected
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onSelect(t) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            t.icon,
                            contentDescription = t.label,
                            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            t.label,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionScreen(onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    val backdrop = rememberGlassBackdrop()
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().glassSource(backdrop)) { BlobField() }
        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
            GlassBox(
                Modifier.align(Alignment.Center).padding(24.dp).fillMaxWidth(),
                cornerRadius = 36.dp,
            ) {
                Column(
                    Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("Glassify", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Allow access to the music on your phone so Glassify can play it. " +
                            "Nothing leaves your device.",
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                    )
                    GlassPillButton("Allow music access", Icons.Rounded.MusicNote, onAllow)
                    Text(
                        "Already denied? Open app settings",
                        color = Accent,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(onClick = onOpenSettings).padding(6.dp),
                    )
                }
            }
        }
    }
}
