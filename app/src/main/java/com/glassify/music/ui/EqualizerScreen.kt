package com.glassify.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassify.music.playback.AudioFx
import com.glassify.music.ui.neu.NeuChip
import com.glassify.music.ui.neu.NeuColors
import com.glassify.music.ui.neu.NeuSlider
import com.glassify.music.ui.neu.NeuToggle
import com.glassify.music.ui.neu.NeuVSlider
import com.glassify.music.ui.neu.neuRaised
import kotlin.math.roundToInt

private fun freqLabel(hz: Int): String = if (hz >= 1000) "${hz / 1000}k" else "$hz"

@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val min = AudioFx.minLevel
    val max = AudioFx.maxLevel
    val span = (max - min).coerceAtLeast(1)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Equalizer", onBack = onBack)

        if (!AudioFx.attached) {
            EmptyHint("Play a song first, then the equalizer becomes available.")
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Equalizer", color = NeuColors.TextMain, fontSize = 17.sp, modifier = Modifier.weight(1f))
            NeuToggle(AudioFx.enabled) { AudioFx.updateEnabled(it) }
        }

        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(AudioFx.presetNames) { i, name ->
                NeuChip(name, AudioFx.preset == i, { AudioFx.choosePreset(i) })
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .neuRaised(28.dp, 6.dp)
                .padding(horizontal = 10.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AudioFx.bandLevels.forEachIndexed { i, level ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NeuVSlider(
                        value = (level - min).toFloat() / span,
                        onChange = { AudioFx.updateBand(i, (min + it * span).roundToInt()) },
                        modifier = Modifier.height(190.dp),
                    )
                    Text(
                        AudioFx.bandFreqs.getOrNull(i)?.let { freqLabel(it) } ?: "",
                        color = NeuColors.TextDim,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Bass boost", color = NeuColors.TextMain, fontSize = 15.sp)
            NeuSlider(
                value = AudioFx.bassLevel / 1000f,
                onChange = { AudioFx.updateBass((it * 1000).roundToInt()) },
            )
            Text("Virtualizer", color = NeuColors.TextMain, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
            NeuSlider(
                value = AudioFx.virtLevel / 1000f,
                onChange = { AudioFx.updateVirtualizer((it * 1000).roundToInt()) },
            )
        }
        Text(
            "The phone's built-in equalizer has 5 bands. Moving a slider switches to Custom.",
            color = NeuColors.TextDim,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }
}
