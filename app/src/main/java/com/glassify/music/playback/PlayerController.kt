package com.glassify.music.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.glassify.music.data.Song
import com.google.common.util.concurrent.ListenableFuture

/** Talks to [PlaybackService] and exposes the playback state to Compose. */
class PlayerController(private val context: Context) {

    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    var currentSong by mutableStateOf<Song?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
        private set

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            sync()
        }
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener(
            {
                controller = runCatching { f.get() }.getOrNull()
                controller?.addListener(listener)
                sync()
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun release() {
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        future = null
        controller = null
    }

    private fun sync() {
        val c = controller ?: return
        isPlaying = c.isPlaying
        shuffle = c.shuffleModeEnabled
        repeatMode = c.repeatMode
        val item = c.currentMediaItem
        val song = item?.toSong()
        if (song?.id != currentSong?.id) currentSong = song
        durationMs = if (c.duration > 0) c.duration else (song?.durationMs ?: 0L)
        positionMs = c.currentPosition.coerceAtLeast(0L)
    }

    /** Called ~4x per second by the UI while music is playing. */
    fun refreshPosition() {
        val c = controller ?: return
        positionMs = c.currentPosition.coerceAtLeast(0L)
        if (c.duration > 0) durationMs = c.duration
    }

    fun play(songs: List<Song>, index: Int) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        c.setMediaItems(songs.map { it.toMediaItem() }, index.coerceIn(0, songs.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun shuffleAll(songs: List<Song>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        c.shuffleModeEnabled = true
        c.setMediaItems(songs.map { it.toMediaItem() }, songs.indices.random(), 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPrevious() }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        positionMs = ms
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
}

private fun Song.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artUri)
                .setExtras(
                    Bundle().apply {
                        putLong("albumId", albumId)
                        putLong("duration", durationMs)
                    },
                )
                .build(),
        )
        .build()

private fun MediaItem.toSong(): Song? {
    val id = mediaId.toLongOrNull() ?: return null
    val md = mediaMetadata
    return Song(
        id = id,
        title = md.title?.toString() ?: "Unknown title",
        artist = md.artist?.toString() ?: "Unknown artist",
        album = md.albumTitle?.toString() ?: "",
        albumId = md.extras?.getLong("albumId") ?: 0L,
        durationMs = md.extras?.getLong("duration") ?: 0L,
        dateAdded = 0L,
        track = 0,
    )
}
