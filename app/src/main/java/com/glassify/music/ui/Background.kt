package com.glassify.music.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.glassify.music.data.Song

/** Colourful soft light blobs. Gives glass something nice to bend even without album art. */
@Composable
fun BlobField(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFF16112E), Color(0xFF0B1020), Color(0xFF05060B)),
            ),
        )
        fun blob(color: Color, cx: Float, cy: Float, r: Float) {
            val center = Offset(size.width * cx, size.height * cy)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color, Color.Transparent),
                    center = center,
                    radius = r,
                ),
                radius = r,
                center = center,
            )
        }
        blob(Color(0xFF7B5CFF).copy(alpha = 0.55f), 0.15f, 0.18f, size.width * 0.9f)
        blob(Color(0xFF16C7B8).copy(alpha = 0.40f), 0.95f, 0.50f, size.width * 0.8f)
        blob(Color(0xFFFF4F9A).copy(alpha = 0.38f), 0.25f, 0.88f, size.width * 0.85f)
    }
}

/** Full-screen background: blurred cover art of the playing song (or the colour blobs). */
@Composable
fun AppBackground(song: Song?, useArt: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color(0xFF07070C))) {
        BlobField()
        if (useArt && song != null) {
            Crossfade(targetState = song, label = "background") { s ->
                val art = rememberSongArt(s, 200)
                if (art != null) {
                    Box(Modifier.fillMaxSize()) {
                        Image(
                            bitmap = art,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(1.35f)
                                .blur(40.dp),
                        )
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)))
                    }
                }
            }
        }
    }
}
