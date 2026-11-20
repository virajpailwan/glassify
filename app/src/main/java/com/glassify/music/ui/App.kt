package com.glassify.music.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassify.music.data.AppStore
import com.glassify.music.data.MusicRepository
import com.glassify.music.data.Song
import com.glassify.music.playback.PlayerController
import com.glassify.music.ui.neu.AccentColor
import com.glassify.music.ui.neu.AccentIndex
import com.glassify.music.ui.neu.NeuButton
import com.glassify.music.ui.neu.NeuColors
import com.glassify.music.ui.neu.neuInset
import com.glassify.music.ui.neu.neuRaised
import com.glassify.music.ui.neu.plainClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class NavTab(val label: String, val icon: ImageVector) {
    Search("Search", Icons.Rounded.Search),
    Library("Library", Icons.Rounded.LibraryMusic),
    Settings("Settings", Icons.Rounded.Settings),
}

@Composable
fun NeumoApp() {
    val context = LocalContext.current
    val store = remember { AppStore(context) }
    val player = remember { PlayerController(context) }
    remember {
        AccentIndex = store.accent
        player.onSongChanged = { store.pushRecent(it.id) }
        true
    }
    LaunchedEffect(store.accent) { AccentIndex = store.accent }

    DisposableEffect(Unit) {
        player.connect()
        onDispose { player.release() }
    }
    LaunchedEffect(Unit) {
        while (true) {
            player.refreshPosition()
            delay(500)
        }
    }

    var granted by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    LaunchedEffect(Unit) {
        if (!granted) {
            launcher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var scanKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(granted, scanKey) {
        if (granted) {
            songs = withContext(Dispatchers.IO) { MusicRepository.loadSongs(context) }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AccentColor,
            background = NeuColors.Bg,
            surface = NeuColors.Surface,
            onSurface = NeuColors.TextMain,
        ),
    ) {
        Box(Modifier.fillMaxSize().background(NeuColors.Bg)) {
            if (!granted) {
                PermissionScreen {
                    launcher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS))
                }
            } else {
                Shell(songs, store, player, onRescan = { scanKey++ })
            }
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text("Neumo", color = NeuColors.TextMain, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "Allow access to your music so Neumo can find the songs on this phone.",
            color = NeuColors.TextDim,
            fontSize = 15.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        NeuButton(Icons.Rounded.PlayArrow, onRequest, size = 72.dp, iconSize = 34.dp, accent = true, description = "Allow")
    }
}

@Composable
private fun Shell(songs: List<Song>, store: AppStore, player: PlayerController, onRescan: () -> Unit) {
    var tab by remember { mutableIntStateOf(NavTab.Library.ordinal) }
    var detail by remember { mutableStateOf<Detail?>(null) }
    var showNow by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }

    BackHandler(enabled = detail != null) { detail = null }
    BackHandler(enabled = detail == null && tab != NavTab.Library.ordinal) { tab = NavTab.Library.ordinal }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
                val screen: Any = detail ?: tab
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        if (targetState is Detail) {
                            (slideInHorizontally(tween(260)) { it / 4 } + fadeIn(tween(260))) togetherWith
                                (fadeOut(tween(160)))
                        } else if (initialState is Detail) {
                            fadeIn(tween(220)) togetherWith
                                (slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(220)))
                        } else {
                            fadeIn(tween(220)) togetherWith fadeOut(tween(140))
                        }
                    },
                    label = "screen",
                ) { s ->
                    if (s is Detail) {
                        DetailScreen(s, songs, store, player, onBack = { detail = null })
                    } else {
                        when (NavTab.entries[s as Int]) {
                            NavTab.Search -> SearchScreen(songs, store, player, onDetail = { detail = it })
                            NavTab.Library -> LibraryScreen(songs, store, player, onDetail = { detail = it })
                            NavTab.Settings -> SettingsScreen(songCount = songs.size, onRescan = onRescan)
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = player.currentSong != null,
                enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200)),
            ) {
                MiniPlayer(player, onOpen = { showNow = true })
            }
            BottomNav(tab) { tab = it; detail = null }
        }

        Overlay(showNow) {
            NowPlayingScreen(
                player, store,
                onClose = { showNow = false },
                onQueue = { showQueue = true },
            )
        }
        Overlay(showQueue) { QueueScreen(player, onBack = { showQueue = false }) }
    }
}

@Composable
private fun Overlay(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it / 3 } + fadeIn(),
        exit = slideOutVertically { it / 3 } + fadeOut(),
    ) {
        Box(Modifier.fillMaxSize().background(NeuColors.Bg).windowInsetsPadding(WindowInsets.systemBars)) {
            content()
        }
    }
}

@Composable
private fun MiniPlayer(player: PlayerController, onOpen: () -> Unit) {
    val song = player.currentSong ?: return
    val target = if (player.durationMs > 0) player.positionMs.toFloat() / player.durationMs else 0f
    val fraction by animateFloatAsState(target, tween(500), label = "mini")
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .neuRaised(24.dp, 5.dp)
            .plainClickable(onOpen)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArtImage(song, Modifier.size(44.dp), corner = 12.dp, px = 128)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    song.title,
                    color = NeuColors.TextMain,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artist,
                    color = NeuColors.TextDim,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NeuButton(
                if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                { player.togglePlay() },
                size = 38.dp,
                iconSize = 20.dp,
                accent = true,
                description = "Play or pause",
            )
            NeuButton(Icons.Rounded.SkipNext, { player.next() }, size = 34.dp, iconSize = 18.dp, description = "Next")
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(5.dp).neuInset(3.dp)) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(5.dp)
                    .background(AccentColor, androidx.compose.foundation.shape.RoundedCornerShape(3.dp)),
            )
        }
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .neuRaised(28.dp, 6.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        NavTab.entries.forEachIndexed { i, t ->
            val on = i == selected
            Column(
                Modifier
                    .weight(1f)
                    .then(if (on) Modifier.neuInset(18.dp) else Modifier)
                    .plainClickable { onSelect(i) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    t.icon,
                    contentDescription = t.label,
                    tint = if (on) AccentColor else NeuColors.TextDim,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    t.label,
                    color = if (on) AccentColor else NeuColors.TextDim,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
