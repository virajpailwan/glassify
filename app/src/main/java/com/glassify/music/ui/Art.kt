package com.glassify.music.ui

import android.graphics.Bitmap
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glassify.music.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object ArtCache {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, bitmap: Bitmap) { cache.put(key, bitmap) }
}

private fun artKey(song: Song, px: Int): String =
    if (song.albumId > 0) "a${song.albumId}_$px" else "s${song.id}_$px"

/** Loads the cover art of [song] (embedded art or folder art) at about [px] pixels. */
@Composable
fun rememberSongArt(song: Song?, px: Int): ImageBitmap? {
    val context = LocalContext.current
    val key = song?.let { artKey(it, px) }
    val state = produceState<ImageBitmap?>(
        initialValue = key?.let { ArtCache.get(it)?.asImageBitmap() },
        key1 = key,
    ) {
        if (song == null || key == null) {
            value = null
            return@produceState
        }
        val cached = ArtCache.get(key)
        if (cached != null) {
            value = cached.asImageBitmap()
            return@produceState
        }
        value = null
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.loadThumbnail(song.uri, Size(px, px), null)
            }.getOrNull()
        }
        if (bitmap != null) ArtCache.put(key, bitmap)
        value = bitmap?.asImageBitmap()
    }
    return state.value
}

@Composable
fun ArtImage(
    song: Song?,
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp,
    px: Int = 256,
) {
    val art = rememberSongArt(song, px)
    Box(modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(corner))) {
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val hue = (((song?.albumId ?: 0L) * 47L) % 360L).toFloat().let { if (it < 0) it + 360f else it }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color.hsv(hue, 0.55f, 0.55f), Color.hsv((hue + 50f) % 360f, 0.6f, 0.3f)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
